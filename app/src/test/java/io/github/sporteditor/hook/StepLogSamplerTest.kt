package io.github.sporteditor.hook

import org.junit.Assert.assertEquals
import org.junit.Test

class StepLogSamplerTest {

    @Test
    fun firstSamplesAreVerbose() {
        val sampler = StepLogSampler(maxVerbose = 20, summaryIntervalMs = 60_000)
        repeat(20) { index ->
            assertEquals("event $index", StepLogSampler.Action.VERBOSE, sampler.record(0))
        }
    }

    @Test
    fun nextEventAfterVerboseBudgetEmitsSummary() {
        val sampler = StepLogSampler(maxVerbose = 20, summaryIntervalMs = 60_000)
        repeat(20) { sampler.record(0) }
        assertEquals(StepLogSampler.Action.SUMMARY, sampler.record(1_000))
    }

    @Test
    fun summariesAreThrottledByInterval() {
        val sampler = StepLogSampler(maxVerbose = 20, summaryIntervalMs = 60_000)
        repeat(20) { sampler.record(0) }
        assertEquals(StepLogSampler.Action.SUMMARY, sampler.record(1_000))
        assertEquals(StepLogSampler.Action.NONE, sampler.record(2_000))
        assertEquals(StepLogSampler.Action.NONE, sampler.record(60_999))
        assertEquals(StepLogSampler.Action.SUMMARY, sampler.record(61_000))
    }

    @Test
    fun summaryWorksEvenWhenVerboseBudgetIsZero() {
        val sampler = StepLogSampler(maxVerbose = 0, summaryIntervalMs = 1_000)
        assertEquals(StepLogSampler.Action.SUMMARY, sampler.record(5_000))
        assertEquals(StepLogSampler.Action.NONE, sampler.record(5_500))
        assertEquals(StepLogSampler.Action.SUMMARY, sampler.record(6_000))
    }
}
