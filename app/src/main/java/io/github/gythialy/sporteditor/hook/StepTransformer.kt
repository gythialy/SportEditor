package io.github.gythialy.sporteditor.hook

import io.github.gythialy.sporteditor.data.DailyLimit
import io.github.gythialy.sporteditor.data.StepMode
import io.github.gythialy.sporteditor.data.TargetSettings
import java.time.LocalDate

/**
 * Applies a [TargetSettings] to a raw sensor step value.
 *
 * One instance belongs to one hooked process and sensor. The reported value stays an absolute,
 * monotonically growing counter (the target apps treat `TYPE_STEP_COUNTER` as steps since boot),
 * but every local day gets its own budget:
 *
 *  - The first event of a day records the raw baseline and carries the previous day's last
 *    reported value as today's starting point, so the app sees no backward jump at midnight.
 *  - Within the day only the raw delta since the baseline is transformed:
 *    MULTIPLY/ADD use `delta * value` / `delta + value`, REPLACE reports a constant value and
 *    AUTO adds `value` per callback. The increment is capped by the day's budget.
 *  - The budget is [TargetSettings.maxSteps] plus [DailyLimit]'s per-day jitter (0..100).
 *    Lowering the limit mid-day takes effect immediately: the increment stops at the new cap.
 *  - A rollback of the raw counter (device reboot) is detected when the new reading is below
 *    the last one; today's progress is kept by moving the accumulated raw delta into the
 *    baseline compensation instead of restarting the day.
 *  - The absolute counter would reach [MAX_STEPS] after many months, so the next day starts
 *    from zero again (target apps treat that like a sensor reset); this keeps the app's math
 *    inside its own limits.
 *
 * `TYPE_STEP_DETECTOR` is not a cumulative counter: every event carries `1.0` and means one
 * step. Set [incremental] for it so each event is added to the day's delta instead of being
 * compared with a raw baseline.
 *
 * State is persisted through [DailyStateStore]; states written before daily accumulation
 * (version 0) are migrated on first use so the target app does not see a negative jump.
 * The class is intentionally free of Android dependencies so it can be unit tested.
 */
internal class StepTransformer(
    private val clock: () -> LocalDate = { LocalDate.now() },
    private val extraSteps: (epochDay: Long) -> Int = DailyLimit::jitter,
    private val store: DailyStateStore? = null,
    private val incremental: Boolean = false
) {
    private var restored = false
    private var needsMigration = false
    private var day: LocalDate? = null
    private var baselineRaw = 0f
    private var lastRaw = 0f
    private var rawDeltaOffset = 0f
    private var dayStartReported = 0f
    private var dailyLimit = 0
    private var lastReported = 0f
    private var autoCount = 0

    init {
        restore()
    }

    @Synchronized
    fun transform(raw: Float, settings: TargetSettings): Float {
        if (!settings.enabled) return raw
        if (!restored) restore()

        val today = clock()
        if (needsMigration) {
            migrate(raw, settings, today)
            return lastReported
        }

        val newDay = day != today
        val counterReset = raw < lastRaw
        if (newDay || counterReset) {
            beginPeriod(today, raw, settings, counterReset)
        }

        val limit = DailyLimit.effective(settings.maxSteps, extraSteps(today.toEpochDay()))
        dailyLimit = limit

        if (incremental) return transformIncremental(raw, settings, today, limit)

        // Only today's raw delta is transformed; lastRaw/baseline keep the day's progress across
        // a reboot without ever re-counting the same steps.
        val rawDelta = (rawDeltaOffset + (raw - baselineRaw)).coerceAtLeast(0f)
        val reported = when (settings.mode) {
            StepMode.MULTIPLY -> clampDaily(rawDelta * settings.value, limit)
            StepMode.ADD -> clampDaily(rawDelta + settings.value, limit)
            StepMode.REPLACE -> clampDaily(settings.value.toFloat(), limit)
            StepMode.AUTO -> autoValue(limit, settings)
        }

        lastRaw = raw
        lastReported = sanitize(reported)
        persist()
        return lastReported
    }

    /**
     * `TYPE_STEP_DETECTOR` events are independent steps (value 1.0 each), so they are summed
     * into the day's delta instead of being differenced against a cumulative baseline.
     */
    private fun transformIncremental(raw: Float, settings: TargetSettings, today: LocalDate, limit: Int): Float {
        if (day != today) {
            val carried = if (day == null) 0f else lastReported
            dayStartReported = if (carried + limit <= TargetSettings.MAX_VALUE) carried else 0f
            rawDeltaOffset = 0f
            autoCount = 0
            day = today
        }

        val delta = rawDeltaOffset + raw.coerceAtLeast(0f)
        rawDeltaOffset = delta
        val reported = when (settings.mode) {
            StepMode.MULTIPLY -> clampDaily(delta * settings.value, limit)
            StepMode.ADD -> clampDaily(delta + settings.value, limit)
            StepMode.REPLACE -> clampDaily(settings.value.toFloat(), limit)
            StepMode.AUTO -> autoValue(limit, settings)
        }

        lastRaw = raw
        lastReported = sanitize(reported)
        persist()
        return lastReported
    }

    private fun restore() {
        if (restored) return
        val state = store?.load() ?: return
        restored = true
        day = runCatching { LocalDate.ofEpochDay(state.epochDay) }.getOrNull()
        baselineRaw = state.baselineRaw
        lastRaw = state.lastRaw
        rawDeltaOffset = state.rawDeltaOffset
        dayStartReported = state.dayStartReported
        dailyLimit = state.dailyLimit
        lastReported = state.lastReported
        autoCount = state.autoCount
        needsMigration = state.version < STATE_VERSION
    }

    /**
     * Version 0 capped the absolute value and never accumulated a daily delta. Keep whatever the
     * app already displays, reset the raw baseline to the current reading and let the remaining
     * budget accumulate from now on; the app therefore never sees a negative jump on upgrade.
     */
    private fun migrate(raw: Float, settings: TargetSettings, today: LocalDate) {
        day = today
        baselineRaw = raw
        lastRaw = raw
        rawDeltaOffset = 0f
        dayStartReported = lastReported
        dailyLimit = DailyLimit.effective(settings.maxSteps, extraSteps(today.toEpochDay()))
        autoCount = 0
        needsMigration = false
        persist()
    }

    private fun persist() {
        val currentDay = day ?: return
        store?.save(
            DailyState(
                epochDay = currentDay.toEpochDay(),
                baselineRaw = baselineRaw,
                lastRaw = lastRaw,
                rawDeltaOffset = rawDeltaOffset,
                dayStartReported = dayStartReported,
                dailyLimit = dailyLimit,
                lastReported = lastReported,
                autoCount = autoCount,
                version = STATE_VERSION
            )
        )
    }

    private fun beginPeriod(today: LocalDate, raw: Float, settings: TargetSettings, counterReset: Boolean) {
        val startingNewDay = day != today
        if (startingNewDay) {
            val limit = DailyLimit.effective(settings.maxSteps, extraSteps(today.toEpochDay()))
            val carried = if (day == null) 0f else lastReported
            // After a few months the absolute counter would overflow the apps' own bounds. Start
            // the next day from zero then; targets treat it like a sensor reset.
            dayStartReported = if (carried + limit <= TargetSettings.MAX_VALUE) carried else 0f
            rawDeltaOffset = 0f
            autoCount = 0
            dailyLimit = limit
        } else if (counterReset) {
            // Device rebooted (or the counter rolled back) mid-day: keep the steps already
            // reported today and continue from the new raw baseline.
            rawDeltaOffset += (lastRaw - baselineRaw).coerceAtLeast(0f)
        }
        baselineRaw = raw
        lastRaw = raw
        day = today
    }

    private fun clampDaily(increment: Float, limit: Int): Float = dayStartReported + increment.coerceIn(0f, limit.toFloat())

    private fun autoValue(limit: Int, settings: TargetSettings): Float {
        val increment = settings.value.toLong() * autoCount
        if (increment > TargetSettings.AUTO_CAP) {
            autoCount = 0
            return clampDaily(0f, limit)
        }
        autoCount++
        return clampDaily(increment.toFloat(), limit)
    }

    private fun sanitize(value: Float): Float = when {
        value.isNaN() || value.isInfinite() -> 0f
        value < 0f -> 0f
        value > MAX_STEPS -> MAX_STEPS
        else -> value
    }

    companion object {
        /** Schema version persisted in [DailyState.version]. */
        const val STATE_VERSION = 1

        /** Upper bound reported to target apps to avoid overflow in their own math. */
        const val MAX_STEPS = 9_999_999f
    }
}
