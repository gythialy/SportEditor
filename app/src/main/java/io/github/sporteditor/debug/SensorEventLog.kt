package io.github.sporteditor.debug

/**
 * Parses the structured step-sensor lines emitted by the hook so the diagnostics screen can
 * show what a target app actually received from its sensor queue, including the calculation that
 * produced the reported counter.
 *
 * Verbose sample:
 * `step sensor=STEP_COUNTER type=19 raw=1307.0 -> reported=1750.0 day=20756 base=1200.0
 * delta=107.0 incr=107.0 limit=20075 day_start=0.0 auto=0 schema=2 mode=MULTIPLY value=1
 * rollover=NONE held=false`
 *
 * Summary (after the verbose budget is used up):
 * `step summary sensor=STEP_COUNTER events=37 raw=1307.0 -> reported=1750.0 ...`
 *
 * Lines written by 1.4.0 and earlier have no calculation fields; [parse] still accepts them and
 * the extra fields stay null.
 */
object SensorEventLog {

    data class Event(
        val sensor: String,
        val raw: Float,
        val reported: Float,
        val events: Int? = null,
        val limit: Int? = null,
        val mode: String? = null,
        val day: Long? = null,
        val baseline: Float? = null,
        val delta: Float? = null,
        val increment: Float? = null,
        val dayStart: Float? = null,
        val auto: Int? = null,
        val schema: Int? = null,
        val rollover: String? = null,
        val held: Boolean? = null,
        val modeValue: Int? = null
    )

    private val SENSOR = Regex("""sensor=(\S+)""")
    private val RAW = Regex("""\braw=(-?[\d.]+)""")
    private val REPORTED = Regex("""\breported=(-?[\d.]+)""")
    private val EVENTS = Regex("""\bevents=(\d+)""")
    private val LIMIT = Regex("""\blimit=(\d+)""")
    private val MODE = Regex("""\bmode=([A-Z]+)""")
    private val DAY = Regex("""\bday=(\d+)""")
    private val BASELINE = Regex("""\bbase=(-?[\d.]+)""")
    private val DELTA = Regex("""\bdelta=(-?[\d.]+)""")
    private val INCREMENT = Regex("""\bincr=(-?[\d.]+)""")
    private val DAY_START = Regex("""\bday_start=(-?[\d.]+)""")
    private val AUTO = Regex("""\bauto=(\d+)""")
    private val SCHEMA = Regex("""\bschema=(\d+)""")
    private val ROLLOVER = Regex("""\brollover=([A-Z_+]+)""")
    private val HELD = Regex("""\bheld=(true|false)""")
    private val MODE_VALUE = Regex("""\bvalue=(\d+)""")

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
            mode = MODE.find(message)?.groupValues?.get(1),
            day = DAY.find(message)?.groupValues?.get(1)?.toLongOrNull(),
            baseline = BASELINE.find(message)?.groupValues?.get(1)?.toFloatOrNull(),
            delta = DELTA.find(message)?.groupValues?.get(1)?.toFloatOrNull(),
            increment = INCREMENT.find(message)?.groupValues?.get(1)?.toFloatOrNull(),
            dayStart = DAY_START.find(message)?.groupValues?.get(1)?.toFloatOrNull(),
            auto = AUTO.find(message)?.groupValues?.get(1)?.toIntOrNull(),
            schema = SCHEMA.find(message)?.groupValues?.get(1)?.toIntOrNull(),
            rollover = ROLLOVER.find(message)?.groupValues?.get(1),
            held = HELD.find(message)?.groupValues?.get(1)?.toBooleanStrictOrNull(),
            modeValue = MODE_VALUE.find(message)?.groupValues?.get(1)?.toIntOrNull()
        )
    }

    /**
     * Compact calculation summary for the diagnostics target card, e.g.
     * `delta=18.0 incr=9000.0 limit=20025 day_start=20025.0 base=2133.0 auto=0 rollover=NONE held=false`.
     * Returns null for lines written before the calculation fields existed.
     */
    fun describeCalculation(event: Event): String? {
        // 1.4.0 lines already carried limit/mode but never the delta/increment pair; only the
        // latter proves the calculation fields were recorded.
        if (event.delta == null && event.increment == null) return null
        val parts = buildList {
            event.delta?.let { add("delta=$it") }
            event.increment?.let { add("incr=$it") }
            event.limit?.let { add("limit=$it") }
            event.dayStart?.let { add("day_start=$it") }
            event.baseline?.let { add("base=$it") }
            event.auto?.let { add("auto=$it") }
            event.rollover?.let { add("rollover=$it") }
            event.held?.let { add("held=$it") }
        }
        return parts.joinToString(" ")
    }
}
