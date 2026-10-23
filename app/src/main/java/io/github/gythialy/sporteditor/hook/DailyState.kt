package io.github.gythialy.sporteditor.hook

/**
 * Per-app, per-sensor state that must survive a target process restart.
 *
 * The reported sensor value is an absolute counter that keeps growing across days, while each
 * local day gets its own budget:
 *
 *  - [epochDay] is a [java.time.LocalDate] epoch day.
 *  - [baselineRaw] is the raw sensor value the current raw delta is measured from; it is reset
 *    on a new day or when the sensor counter rolls back (device reboot).
 *  - [lastRaw] is the last raw value that was transformed; it lets a rollback preserve the raw
 *    delta accumulated so far instead of restarting the day.
 *  - [rawDeltaOffset] is the raw delta accumulated before [baselineRaw] (reboot compensation).
 *  - [dayStartReported] is the absolute value reported at the start of the day; the target app
 *    sees a continuous counter instead of a backward jump at midnight.
 *  - [dailyLimit] is today's budget for the reported increment (base value + jitter).
 *  - [lastReported] is the last transformed value written to the app.
 *  - [autoCount] is the per-day AUTO mode callback counter.
 *  - [lastElapsed] is `SystemClock.elapsedRealtime()` at the last event; a smaller reading means
 *    the device rebooted and the step counter restarted at zero.
 *  - [version] marks the state schema; states written before daily accumulation (version 0) are
 *    migrated on first use so the target app does not see a negative jump.
 */
internal data class DailyState(
    val epochDay: Long,
    val baselineRaw: Float,
    val lastRaw: Float,
    val rawDeltaOffset: Float,
    val dayStartReported: Float,
    val dailyLimit: Int,
    val lastReported: Float,
    val autoCount: Int,
    val version: Int = 0,
    val lastElapsed: Long = 0L
)

/** Persistence boundary for [StepTransformer]; tests use an in-memory implementation. */
internal interface DailyStateStore {
    fun load(): DailyState?
    fun save(state: DailyState)
}
