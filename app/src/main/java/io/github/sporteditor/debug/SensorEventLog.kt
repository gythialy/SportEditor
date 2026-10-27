package io.github.sporteditor.debug

/**
 * Parses the structured step-sensor lines emitted by the hook so the diagnostics screen can
 * show what a target app actually received from its sensor queue.
 *
 * Verbose sample:
 * `step sensor=STEP_COUNTER type=19 values=[1307.0] raw=1307.0 -> reported=1750.0 mode=MULTIPLY value=50 limit=20075`
 * Summary (after the verbose budget is used up):
 * `step summary sensor=STEP_COUNTER events=37 raw=1307.0 -> reported=1750.0`
 */
object SensorEventLog {

    data class Event(
        val sensor: String,
        val raw: Float,
        val reported: Float,
        val events: Int? = null,
        val limit: Int? = null,
        val mode: String? = null
    )

    private val SENSOR = Regex("""sensor=(\S+)""")
    private val RAW = Regex("""\braw=(-?[\d.]+)""")
    private val REPORTED = Regex("""\breported=(-?[\d.]+)""")
    private val EVENTS = Regex("""\bevents=(\d+)""")
    private val LIMIT = Regex("""\blimit=(\d+)""")
    private val MODE = Regex("""\bmode=([A-Z]+)""")

    fun parse(message: String): Event? {
        if (!message.startsWith("step ")) return null
        val sensor = SENSOR.find(message)?.groupValues?.get(1) ?: return null
        val raw = RAW.find(message)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        val reported = REPORTED.find(message)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        return Event(
            sensor = sensor,
            raw = raw,
            reported = reported,
            events = EVENTS.find(message)?.groupValues?.get(1)?.toIntOrNull(),
            limit = LIMIT.find(message)?.groupValues?.get(1)?.toIntOrNull(),
            mode = MODE.find(message)?.groupValues?.get(1)
        )
    }
}
