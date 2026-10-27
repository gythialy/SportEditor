package io.github.sporteditor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLimitTest {

    @Test
    fun jitter_isWithinConfiguredRange() {
        repeat(200) { offset ->
            val value = DailyLimit.jitter(offset.toLong())
            assertTrue("jitter out of range: $value", value in 0..DailyLimit.JITTER_LIMIT)
        }
    }

    @Test
    fun jitter_isStableForTheSameDay() {
        val epochDay = 20_670L
        assertEquals(DailyLimit.jitter(epochDay), DailyLimit.jitter(epochDay))
    }

    @Test
    fun jitter_variesAcrossDays() {
        val values = (0L until 30L).map(DailyLimit::jitter).toSet()
        assertTrue("expected the jitter to change across days, got $values", values.size > 1)
    }

    @Test
    fun effective_addsTheJitterToTheBaseLimit() {
        assertEquals(80_000, DailyLimit.effective(80_000, 0))
        assertEquals(80_100, DailyLimit.effective(80_000, 100))
        assertEquals(DailyLimit.effective(80_000, 42), DailyLimit.effective(80_000, 42))
    }

    @Test
    fun effective_clampsInvalidBaseLimits() {
        assertEquals(1, DailyLimit.effective(0, 0))
        assertEquals(1, DailyLimit.effective(-5, 0))
        assertEquals(TargetSettings.MAX_VALUE, DailyLimit.effective(TargetSettings.MAX_VALUE, 100))
    }

    @Test
    fun effectiveToday_matchesTheEpochDayComputation() {
        val today = DailyLimit.effectiveToday(50_000)
        assertTrue(
            "effective limit cannot be below the base: $today",
            today >= 50_000
        )
        assertTrue(today <= 50_000 + DailyLimit.JITTER_LIMIT)
    }
}
