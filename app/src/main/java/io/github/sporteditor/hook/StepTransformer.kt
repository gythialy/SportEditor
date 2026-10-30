package io.github.sporteditor.hook

import io.github.sporteditor.data.DailyLimit
import io.github.sporteditor.data.StepMode
import io.github.sporteditor.data.TargetSettings
import java.time.LocalDate

/**
 * Applies a [TargetSettings] to a raw sensor step value.
 *
 * One instance belongs to one hooked process and sensor. The reported value stays an absolute,
 * monotonically growing counter (the target apps treat `TYPE_STEP_COUNTER` as steps since boot),
 * but every local day gets its own budget:
 *
 *  - The first event of a day records the raw baseline and carries the previous day's last
 *    reported value as today's starting point, so the absolute counter stays continuous and the
 *    target app never sees a backward jump at midnight.
 *  - Within the day only the raw delta since the baseline is transformed:
 *    MULTIPLY/ADD use `delta * value` / `delta + value`, REPLACE reports a constant value and
 *    AUTO adds `value` per callback. Only the day's increment is capped by the day's budget;
 *    the absolute counter keeps growing across days, so the target app's own daily delta stays
 *    positive instead of freezing at the limit after the first day.
 *  - The budget is [TargetSettings.maxSteps] plus [DailyLimit]'s per-day jitter (0..100).
 *    Lowering the limit mid-day takes effect immediately: the reported value stops growing
 *    without jumping backwards.
 *  - A device reboot is detected through `SystemClock.elapsedRealtime()` (it restarts at zero
 *    on boot), so the counter baseline is reset to zero and the steps taken since boot are
 *    counted even when the first event arrives after the raw value already surpassed the last
 *    reading. A raw rollback without a boot is treated as a sensor reset and rebased to the new
 *    reading. In both cases today's progress is preserved.
 *  - The absolute counter would reach [MAX_STEPS] after many months, so the next day starts
 *    from zero again (target apps treat that like a sensor reset); this keeps the app's math
 *    inside its own limits.
 *
 * `TYPE_STEP_DETECTOR` is not a cumulative counter: every event carries `1.0` and means one
 * step. Set [incremental] for it so each event is added to the day's delta instead of being
 * compared with a raw baseline.
 *
 * State is persisted through [DailyStateStore]; states written by earlier versions (schema
 * versions 0 and 1) are migrated on first use so the target app does not see a negative jump
 * and the counter is not pinned at the daily limit. The class is intentionally free of Android
 * dependencies so it can be unit tested.
 */
internal class StepTransformer(
    private val clock: () -> LocalDate = { LocalDate.now() },
    private val extraSteps: (epochDay: Long) -> Int = DailyLimit::jitter,
    private val store: DailyStateStore? = null,
    private val incremental: Boolean = false,
    private val elapsedRealtime: () -> Long = { 0L }
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
    private var lastElapsed = 0L

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
        val rebooted = elapsedRealtime() < lastElapsed
        val counterReset = raw < lastRaw
        val rolledOver = newDay || counterReset || rebooted
        if (rolledOver) {
            beginPeriod(today, raw, settings, counterReset = counterReset, rebooted = rebooted)
        }

        val limit = DailyLimit.effective(settings.maxSteps, extraSteps(today.toEpochDay()))
        dailyLimit = limit

        if (incremental) return transformIncremental(raw, settings, limit, rolledOver)

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
        lastElapsed = elapsedRealtime()
        lastReported = settle(reported, rolledOver)
        persist()
        return lastReported
    }

    /**
     * `TYPE_STEP_DETECTOR` events are independent steps (value 1.0 each), so they are summed
     * into the day's delta instead of being differenced against a cumulative baseline.
     */
    private fun transformIncremental(raw: Float, settings: TargetSettings, limit: Int, rolledOver: Boolean): Float {
        // Day rollover, reboot and counter rollback are already handled by beginPeriod(); the
        // detector stream only appends this event to the day's delta.
        val delta = rawDeltaOffset + raw.coerceAtLeast(0f)
        rawDeltaOffset = delta
        val reported = when (settings.mode) {
            StepMode.MULTIPLY -> clampDaily(delta * settings.value, limit)
            StepMode.ADD -> clampDaily(delta + settings.value, limit)
            StepMode.REPLACE -> clampDaily(settings.value.toFloat(), limit)
            StepMode.AUTO -> autoValue(limit, settings)
        }

        lastRaw = raw
        lastReported = settle(reported, rolledOver)
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
        lastElapsed = state.lastElapsed
        needsMigration = state.version < STATE_VERSION
    }

    /**
     * Version 0 capped the absolute value, and version 1 carried that capped value into the next
     * day, which pinned the reported counter at the daily limit. Keep the absolute counter
     * continuous, reset the raw baseline to the current reading and start today's delta from zero:
     * the target app sees no jump on upgrade and resumes growing immediately.
     */
    private fun migrate(raw: Float, settings: TargetSettings, today: LocalDate) {
        val limit = DailyLimit.effective(settings.maxSteps, extraSteps(today.toEpochDay()))
        day = today
        baselineRaw = raw
        lastRaw = raw
        rawDeltaOffset = 0f
        // Drop the part of today that the old implementation already counted instead of applying
        // the whole day's budget to the first event after the upgrade.
        val carried = if (lastReported + limit > MAX_STEPS) 0f else lastReported
        dayStartReported = carried
        lastReported = carried
        dailyLimit = limit
        autoCount = 0
        lastElapsed = elapsedRealtime()
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
                version = STATE_VERSION,
                lastElapsed = lastElapsed
            )
        )
    }

    private fun beginPeriod(today: LocalDate, raw: Float, settings: TargetSettings, counterReset: Boolean, rebooted: Boolean) {
        val limit = DailyLimit.effective(settings.maxSteps, extraSteps(today.toEpochDay()))
        val startingNewDay = day != today
        if (startingNewDay) {
            // Carry the absolute counter so the app's own daily delta stays positive; only an
            // imminent MAX_STEPS overflow restarts it from zero.
            dayStartReported = carriedValue(limit)
            rawDeltaOffset = 0f
            autoCount = 0
            dailyLimit = limit
        } else if (counterReset || rebooted) {
            // Keep the steps already reported today, then continue from the new counter origin.
            rawDeltaOffset += (lastRaw - baselineRaw).coerceAtLeast(0f)
        }
        // On a reboot the step counter restarts at zero, so raw already contains every step since
        // boot; from a plain sensor rollback the new reading is the safest baseline.
        baselineRaw = if (rebooted) 0f else raw
        lastRaw = raw
        lastElapsed = elapsedRealtime()
        day = today
    }

    /**
     * Carries yesterday's reported value into a new day so the target app keeps a continuous
     * counter. The only exception is an imminent overflow of [MAX_STEPS]: the counter then
     * restarts at zero, which target apps treat like a sensor reset.
     */
    private fun carriedValue(limit: Int): Float {
        if (day == null) return 0f
        return if (lastReported + limit > MAX_STEPS) 0f else lastReported
    }

    /** Adds the day's increment to the carried value; only the increment is capped. */
    private fun clampDaily(increment: Float, limit: Int): Float = dayStartReported + increment.coerceIn(0f, limit.toFloat())

    /**
     * AUTO adds [TargetSettings.value] per callback; the increment grows with the callback count
     * and is capped by the day's budget like every other mode.
     */
    private fun autoValue(limit: Int, settings: TargetSettings): Float {
        val increment = settings.value.toLong() * autoCount
        autoCount++
        return clampDaily(increment.toFloat(), limit)
    }

    /**
     * Reports the new value unless it would move the counter backwards within the same day.
     * Lowering [TargetSettings.maxSteps] must stop growth, not emit a smaller counter: target
     * apps treat the drop as a sensor reset and freeze the displayed step count.
     */
    private fun settle(reported: Float, rolledOver: Boolean): Float {
        val next = sanitize(reported)
        return if (rolledOver || next >= lastReported) next else lastReported
    }

    private fun sanitize(value: Float): Float = when {
        value.isNaN() || value.isInfinite() -> 0f
        value < 0f -> 0f
        value > MAX_STEPS -> MAX_STEPS
        else -> value
    }

    companion object {
        /** Schema version persisted in [DailyState.version]. */
        const val STATE_VERSION = 2

        /** Upper bound reported to target apps to avoid overflow in their own math. */
        const val MAX_STEPS = 9_999_999f
    }
}
