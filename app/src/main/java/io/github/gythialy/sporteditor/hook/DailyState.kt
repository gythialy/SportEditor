package io.github.gythialy.sporteditor.hook

/**
 * Per-app, per-sensor state that must survive a target process restart.
 *
 * [epochDay] is a [java.time.LocalDate] epoch day, [baselineRaw] the raw sensor value
 * at the start of that day, and [lastReported] the last transformed value written to the app.
 */
internal data class DailyState(
    val epochDay: Long,
    val baselineRaw: Float,
    val dayStartReported: Float,
    val dailyLimit: Int,
    val lastReported: Float,
    val autoCount: Int
)

/** Persistence boundary for [StepTransformer]; tests use an in-memory implementation. */
internal interface DailyStateStore {
    fun load(): DailyState?
    fun save(state: DailyState)
}
