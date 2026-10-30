package io.github.sporteditor.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SensorEventLogTest {

    @Test
    fun parsesVerboseSample() {
        val event = SensorEventLog.parse(
            "step sensor=STEP_COUNTER type=19 values=[1307.0] raw=1307.0 -> reported=1750.0 " +
                "mode=MULTIPLY value=50 limit=20075"
        )
        assertEquals("STEP_COUNTER", event?.sensor)
        assertEquals(1307f, event?.raw)
        assertEquals(1750f, event?.reported)
        assertEquals(20075, event?.limit)
        assertEquals("MULTIPLY", event?.mode)
        assertNull(event?.events)
    }

    @Test
    fun parsesSummary() {
        val event = SensorEventLog.parse(
            "step summary sensor=STEP_DETECTOR events=37 raw=1.0 -> reported=1850.0"
        )
        assertEquals("STEP_DETECTOR", event?.sensor)
        assertEquals(37, event?.events)
        assertEquals(1f, event?.raw)
        assertEquals(1850f, event?.reported)
        assertNull(event?.limit)
    }

    @Test
    fun parsesFractionalValues() {
        val event = SensorEventLog.parse(
            "step sensor=STEP_COUNTER type=19 values=[12.5] raw=12.5 -> reported=625.0 mode=ADD value=10 limit=1000"
        )
        assertEquals(12.5f, event?.raw)
        assertEquals(625f, event?.reported)
        assertEquals(1000, event?.limit)
        assertEquals("ADD", event?.mode)
    }

    @Test
    fun ignoresUnrelatedLines() {
        assertNull(SensorEventLog.parse("config at install -> TargetSettings(enabled=true)"))
        assertNull(SensorEventLog.parse("sensor registered type=STEP_COUNTER handle=7"))
        assertNull(SensorEventLog.parse(""))
    }

    @Test
    fun rejectsMalformedStepLine() {
        assertNull(SensorEventLog.parse("step sensor=STEP_COUNTER raw=12.0"))
        assertNull(SensorEventLog.parse("step raw=12.0 -> reported=13.0"))
    }

    @Test
    fun parsesCalculationFields() {
        val event = SensorEventLog.parse(CALCULATED_SAMPLE)
        assertEquals(20_756L, event?.day)
        assertEquals(2_133f, event?.baseline)
        assertEquals(18f, event?.delta)
        assertEquals(9_000f, event?.increment)
        assertEquals(20_025f, event?.dayStart)
        assertEquals(0, event?.auto)
        assertEquals(2, event?.schema)
        assertEquals("NONE", event?.rollover)
        assertEquals(false, event?.held)
        assertEquals(500, event?.modeValue)
    }

    @Test
    fun describesTheCalculationForTheTargetCard() {
        val event = SensorEventLog.parse(CALCULATED_SAMPLE) ?: error("sample must parse")
        assertEquals(
            "delta=18.0 incr=9000.0 limit=20025 day_start=20025.0 base=2133.0 auto=0 rollover=NONE held=false",
            SensorEventLog.describeCalculation(event)
        )
    }

    @Test
    fun olderSamplesHaveNoCalculationSummary() {
        val event = SensorEventLog.parse(
            "step sensor=STEP_COUNTER type=19 raw=1307.0 -> reported=1750.0 mode=MULTIPLY value=50 limit=20075"
        )
        assertNull(event?.delta)
        assertNull(event?.increment)
        assertNull(SensorEventLog.describeCalculation(event!!))
    }

    private companion object {
        const val CALCULATED_SAMPLE =
            "step sensor=STEP_COUNTER type=19 raw=2151.0 -> reported=29025.0 day=20756 " +
                "base=2133.0 delta=18.0 incr=9000.0 limit=20025 day_start=20025.0 auto=0 " +
                "schema=2 mode=MULTIPLY value=500 rollover=NONE held=false"
    }
}
