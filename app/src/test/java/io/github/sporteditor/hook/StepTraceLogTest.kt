package io.github.sporteditor.hook

import io.github.sporteditor.data.StepMode
import org.junit.Assert.assertEquals
import org.junit.Test

class StepTraceLogTest {

    private val trace = StepTrace(
        epochDay = 20_756,
        raw = 2_151f,
        reported = 29_025f,
        dayStartReported = 20_025f,
        baselineRaw = 2_133f,
        rawDelta = 18f,
        increment = 9_000f,
        limit = 20_025,
        autoCount = 0,
        mode = StepMode.MULTIPLY,
        value = 500,
        schemaVersion = 2,
        rollovers = emptyList(),
        held = false,
        incremental = false
    )

    @Test
    fun sampleLineContainsTheWholeCalculation() {
        assertEquals(
            "step sensor=STEP_COUNTER type=19 raw=2151.0 -> reported=29025.0 " +
                "day=20756 base=2133.0 delta=18.0 incr=9000.0 limit=20025 day_start=20025.0 " +
                "auto=0 schema=2 mode=MULTIPLY value=500 rollover=NONE held=false",
            StepTraceLog.sample("STEP_COUNTER", 19, trace)
        )
    }

    @Test
    fun summaryLineCarriesTheCalculationOfTheLatestEvent() {
        assertEquals(
            "step summary sensor=STEP_COUNTER events=37 raw=2151.0 -> reported=29025.0 " +
                "day=20756 base=2133.0 delta=18.0 incr=9000.0 limit=20025 day_start=20025.0 " +
                "auto=0 schema=2 mode=MULTIPLY value=500 rollover=NONE held=false",
            StepTraceLog.summary("STEP_COUNTER", 37, trace)
        )
    }

    @Test
    fun rolloverLineJoinsEveryReason() {
        val rollover = trace.copy(rollovers = listOf(StepRollover.NEW_DAY, StepRollover.REBOOT))
        assertEquals(
            "step rollover sensor=STEP_COUNTER type=19 day=20756 reason=NEW_DAY+REBOOT " +
                "raw=2151.0 base=2133.0 delta=18.0 day_start=20025.0 limit=20025 schema=2",
            StepTraceLog.rollover("STEP_COUNTER", 19, rollover)
        )
    }

    @Test
    fun heldReportIsVisible() {
        val held = trace.copy(held = true)
        assertEquals(
            "step sensor=STEP_COUNTER type=19 raw=2151.0 -> reported=29025.0 " +
                "day=20756 base=2133.0 delta=18.0 incr=9000.0 limit=20025 day_start=20025.0 " +
                "auto=0 schema=2 mode=MULTIPLY value=500 rollover=NONE held=true",
            StepTraceLog.sample("STEP_COUNTER", 19, held)
        )
    }
}
