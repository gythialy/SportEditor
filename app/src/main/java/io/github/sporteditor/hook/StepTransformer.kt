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
    fun transform(raw: Float, settings: TargetSettings): Float = transformDetailed(raw, settings).value

    /**
     * [transform] plus the intermediate values that produced the reported counter, for the
     * diagnostics log. The calculation and the trace are built in one place, so the logged
     * numbers cannot drift from what the target app actually receives.
     */
    @Synchronized
    fun transformDetailed(raw: Float, settings: TargetSettings): StepResult {
        if (!settings.enabled) {
            return StepResult(
                raw,
                trace(
                    reported = raw,
                    raw = raw,
                    settings = settings,
                    rawDelta = 0f,
                    increment = 0f,
                    limit = dailyLimit,
                    rollovers = emptyList(),
                    held = false
                )
            )
        }
        if (!restored) restore()

        val today = clock()
        if (needsMigration) {
            migrate(raw, settings, today)
            return StepResult(
                lastReported,
                trace(
                    reported = lastReported,
                    raw = raw,
                    settings = settings,
                    rawDelta = 0f,
                    increment = 0f,
                    limit = dailyLimit,
                    rollovers = listOf(StepRollover.MIGRATION),
                    held = false
                )
            )
        }

        val newDay = day != today
        val rebooted = elapsedRealtime() < lastElapsed
        val counterReset = raw < lastRaw
        val rollovers = buildList {
            if (newDay) add(StepRollover.NEW_DAY)
            if (rebooted) add(StepRollover.REBOOT)
            if (counterReset) add(StepRollover.COUNTER_RESET)
        }
        if (rollovers.isNotEmpty()) {
            beginPeriod(today, raw, settings, counterReset = counterReset, rebooted = rebooted)
        }

        val limit = DailyLimit.effective(settings.maxSteps, extraSteps(today.toEpochDay()))
        dailyLimit = limit

        if (incremental) return transformIncremental(raw, settings, limit, rollovers)

        // Only today's raw delta is transformed; lastRaw/baseline keep the day's progress across
        // a reboot without ever re-counting the same steps.
        val rawDelta = (rawDeltaOffset + (raw - baselineRaw)).coerceAtLeast(0f)
        val autoIndex = if (settings.mode == StepMode.AUTO) autoCount else 0
        val increment = cappedIncrement(rawIncrement(rawDelta, settings), limit)
        val settled = settle(dayStartReported + increment, rollovers.isNotEmpty())

        lastRaw = raw
        lastElapsed = elapsedRealtime()
        lastReported = settled.value
        persist()
        return StepResult(
            lastReported,
            trace(
                reported = lastReported,
                raw = raw,
                settings = settings,
                rawDelta = rawDelta,
                increment = increment,
                limit = limit,
                rollovers = rollovers,
                held = settled.held,
                autoIndex = autoIndex
            )
        )
    }

    /**
     * `TYPE_STEP_DETECTOR` events are independent steps (value 1.0 each), so they are summed
     * into the day's delta instead of being differenced against a cumulative baseline.
     */
    private fun transformIncremental(raw: Float, settings: TargetSettings, limit: Int, rollovers: List<StepRollover>): StepResult {
        // Day rollover, reboot and counter rollback are already handled by beginPeriod(); the
        // detector stream only appends this event to the day's delta.
        val delta = rawDeltaOffset + raw.coerceAtLeast(0f)
        rawDeltaOffset = delta
        val autoIndex = if (settings.mode == StepMode.AUTO) autoCount else 0
        val increment = cappedIncrement(rawIncrement(delta, settings), limit)
        val settled = settle(dayStartReported + increment, rollovers.isNotEmpty())

        lastRaw = raw
        lastReported = settled.value
        persist()
        return StepResult(
            lastReported,
            trace(
                reported = lastReported,
                raw = raw,
                settings = settings,
                rawDelta = delta,
                increment = increment,
                limit = limit,
                rollovers = rollovers,
                held = settled.held,
                autoIndex = autoIndex,
                incremental = true
            )
        )
    }

    /** Raw increment produced by the current mode, before the day's budget is applied. */
    private fun rawIncrement(delta: Float, settings: TargetSettings): Float = when (settings.mode) {
        StepMode.MULTIPLY -> delta * settings.value
        StepMode.ADD -> delta + settings.value
        StepMode.REPLACE -> settings.value.toFloat()
        StepMode.AUTO -> nextAutoIncrement(settings)
    }

    /** AUTO adds `value` per callback; returns this callback's increment and advances the count. */
    private fun nextAutoIncrement(settings: TargetSettings): Float {
        val increment = settings.value.toLong() * autoCount
        autoCount++
        return increment.toFloat()
    }

    /** Caps the day's increment; the absolute counter is never capped. */
    private fun cappedIncrement(increment: Float, limit: Int): Float = increment.coerceIn(0f, limit.toFloat())

    private fun trace(
        reported: Float,
        raw: Float,
        settings: TargetSettings,
        rawDelta: Float,
        increment: Float,
        limit: Int,
        rollovers: List<StepRollover>,
        held: Boolean,
        autoIndex: Int = 0,
        incremental: Boolean = this.incremental
    ): StepTrace = StepTrace(
        epochDay = day?.toEpochDay() ?: 0L,
        raw = raw,
        reported = reported,
        dayStartReported = dayStartReported,
        baselineRaw = baselineRaw,
        rawDelta = rawDelta,
        increment = increment,
        limit = limit,
        autoCount = autoIndex,
        mode = settings.mode,
        value = settings.value,
        schemaVersion = STATE_VERSION,
        rollovers = rollovers,
        held = held,
        incremental = incremental
    )

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

    /**
     * Reports the new value unless it would move the counter backwards within the same day.
     * Lowering [TargetSettings.maxSteps] must stop growth, not emit a smaller counter: target
     * apps treat the drop as a sensor reset and freeze the displayed step count.
     */
    private fun settle(reported: Float, rolledOver: Boolean): Settled {
        val next = sanitize(reported)
        val held = !rolledOver && next < lastReported
        return Settled(if (held) lastReported else next, held)
    }

    private data class Settled(val value: Float, val held: Boolean)

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

/** Why the day's state was rebased for one sensor event. */
internal enum class StepRollover {
    /** The local calendar day changed and the absolute counter was carried over. */
    NEW_DAY,

    /** `elapsedRealtime()` went backwards, i.e. the device rebooted. */
    REBOOT,

    /** The raw sensor counter moved backwards without a reboot. */
    COUNTER_RESET,

    /** Persisted state from an older schema was migrated in this event. */
    MIGRATION
}

/**
 * The intermediate values of one [StepTransformer.transformDetailed] call.
 *
 * [rawDelta] is the raw delta since [baselineRaw] (including reboot compensation), [increment] is
 * that delta transformed by the mode and capped by [limit], and [reported] is
 * `dayStartReported + increment` after the backwards-jump guard ([held]).
 */
internal data class StepTrace(
    val epochDay: Long,
    val raw: Float,
    val reported: Float,
    val dayStartReported: Float,
    val baselineRaw: Float,
    val rawDelta: Float,
    val increment: Float,
    val limit: Int,
    val autoCount: Int,
    val mode: StepMode,
    val value: Int,
    val schemaVersion: Int,
    val rollovers: List<StepRollover>,
    val held: Boolean,
    val incremental: Boolean
)

/** Reported counter plus its calculation trace. */
internal data class StepResult(val value: Float, val trace: StepTrace)
