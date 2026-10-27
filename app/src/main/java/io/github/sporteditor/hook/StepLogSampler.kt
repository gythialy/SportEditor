package io.github.sporteditor.hook

/**
 * Decides how many step samples reach the diagnostics log.
 *
 * Every target process emits a verbose sample for the first [maxVerbose] sensor events, then at
 * most one summary per [summaryIntervalMs]. Without the cap a busy pedometer would flood the
 * 500-entry log ring buffer and broadcast a message per step.
 */
internal class StepLogSampler(
    private val maxVerbose: Int = MAX_VERBOSE_SAMPLES,
    private val summaryIntervalMs: Long = SUMMARY_INTERVAL_MS
) {
    enum class Action { VERBOSE, SUMMARY, NONE }

    private var verboseLogged = 0
    private var lastSummaryAt = 0L
    private var summaryStarted = false

    fun record(nowMs: Long): Action {
        if (verboseLogged < maxVerbose) {
            verboseLogged++
            return Action.VERBOSE
        }
        if (!summaryStarted || nowMs - lastSummaryAt >= summaryIntervalMs) {
            summaryStarted = true
            lastSummaryAt = nowMs
            return Action.SUMMARY
        }
        return Action.NONE
    }

    internal companion object {
        const val MAX_VERBOSE_SAMPLES = 20
        const val SUMMARY_INTERVAL_MS = 60_000L
    }
}
