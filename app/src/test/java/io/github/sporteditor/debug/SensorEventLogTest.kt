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
}
