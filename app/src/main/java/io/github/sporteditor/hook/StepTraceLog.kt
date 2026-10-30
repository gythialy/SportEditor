package io.github.sporteditor.hook

/**
 * Formats [StepTrace] values into the structured key=value lines shown by the diagnostics screen.
 *
 * The lines are plain single-line text so they stay copyable and greppable from the diagnostics
 * page; every number that produced the reported counter is included, which means a step issue
 * can be diagnosed without attaching logcat.
 */
internal object StepTraceLog {

    /** Verbose per-event sample; emitted for the first few events of every hooked process. */
    fun sample(sensorName: String, sensorType: Int, trace: StepTrace): String = buildString {
        append("step sensor=").append(sensorName)
        append(" type=").append(sensorType)
        append(" raw=").append(trace.raw)
        append(" -> reported=").append(trace.reported)
        appendCalculation(trace)
        append(" rollover=").append(rolloverText(trace.rollovers))
        append(" held=").append(trace.held)
    }

    /** Low-rate aggregate emitted after the verbose per-process budget is used up. */
    fun summary(sensorName: String, events: Int, trace: StepTrace): String = buildString {
        append("step summary sensor=").append(sensorName)
        append(" events=").append(events)
        append(" raw=").append(trace.raw)
        append(" -> reported=").append(trace.reported)
        appendCalculation(trace)
        append(" rollover=").append(rolloverText(trace.rollovers))
        append(" held=").append(trace.held)
    }

    /** Emitted whenever the day was rebased (new day, reboot, counter reset or migration). */
    fun rollover(sensorName: String, sensorType: Int, trace: StepTrace): String = buildString {
        append("step rollover sensor=").append(sensorName)
        append(" type=").append(sensorType)
        append(" day=").append(trace.epochDay)
        append(" reason=").append(rolloverText(trace.rollovers))
        append(" raw=").append(trace.raw)
        append(" base=").append(trace.baselineRaw)
        append(" delta=").append(trace.rawDelta)
        append(" day_start=").append(trace.dayStartReported)
        append(" limit=").append(trace.limit)
        append(" schema=").append(trace.schemaVersion)
    }

    private fun StringBuilder.appendCalculation(trace: StepTrace) {
        append(" day=").append(trace.epochDay)
        append(" base=").append(trace.baselineRaw)
        append(" delta=").append(trace.rawDelta)
        append(" incr=").append(trace.increment)
        append(" limit=").append(trace.limit)
        append(" day_start=").append(trace.dayStartReported)
        append(" auto=").append(trace.autoCount)
        append(" schema=").append(trace.schemaVersion)
        append(" mode=").append(trace.mode.name)
        append(" value=").append(trace.value)
    }

    private fun rolloverText(rollovers: List<StepRollover>): String =
        if (rollovers.isEmpty()) NONE else rollovers.joinToString("+") { it.name }

    private const val NONE = "NONE"
}
