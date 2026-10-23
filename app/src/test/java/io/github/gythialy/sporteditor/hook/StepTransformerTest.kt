package io.github.gythialy.sporteditor.hook

import io.github.gythialy.sporteditor.data.StepMode
import io.github.gythialy.sporteditor.data.TargetSettings
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepTransformerTest {

    private fun settings(mode: StepMode, value: Int, enabled: Boolean = true, maxSteps: Int = TargetSettings.MAX_VALUE) =
        TargetSettings(enabled = enabled, mode = mode, value = value, maxSteps = maxSteps)

    private fun transformer(extra: Int = 0, today: LocalDate = LocalDate.of(2026, 10, 22)) =
        StepTransformer(clock = { today }, extraSteps = { extra })

    private class MemoryStore : DailyStateStore {
        var state: DailyState? = null

        override fun load(): DailyState? = state

        override fun save(state: DailyState) {
            this.state = state
        }
    }

    @Test
    fun multiply_scalesRawValue() {
        val transformer = StepTransformer()
        assertEquals(250_000f, transformer.transform(2_500f, settings(StepMode.MULTIPLY, 100)), 0.001f)
    }

    @Test
    fun add_appendsFixedOffset() {
        val transformer = StepTransformer()
        assertEquals(82_500f, transformer.transform(2_500f, settings(StepMode.ADD, 80_000)), 0.001f)
    }

    @Test
    fun replace_returnsFixedValue() {
        val transformer = StepTransformer()
        assertEquals(88_888f, transformer.transform(12f, settings(StepMode.REPLACE, 88_888)), 0.001f)
    }

    @Test
    fun auto_incrementsOnEveryCallback() {
        val transformer = StepTransformer()
        val config = settings(StepMode.AUTO, 10_000)

        assertEquals(1_000f, transformer.transform(1_000f, config), 0.001f)
        assertEquals(11_000f, transformer.transform(1_000f, config), 0.001f)
        assertEquals(21_000f, transformer.transform(1_000f, config), 0.001f)
    }

    @Test
    fun auto_restartsAfterCap() {
        val transformer = StepTransformer()
        val config = settings(StepMode.AUTO, 60_000)

        // 0, 60000, then 120000 > 100000 resets the counter and reports the raw value.
        assertEquals(1_000f, transformer.transform(1_000f, config), 0.001f)
        assertEquals(61_000f, transformer.transform(1_000f, config), 0.001f)
        assertEquals(1_000f, transformer.transform(1_000f, config), 0.001f)
        // The counter restarted at zero, so the next callback adds nothing again.
        assertEquals(1_000f, transformer.transform(1_000f, config), 0.001f)
        assertEquals(61_000f, transformer.transform(1_000f, config), 0.001f)
    }

    @Test
    fun disabled_passesRawValueThrough() {
        val transformer = StepTransformer()
        assertEquals(321f, transformer.transform(321f, settings(StepMode.MULTIPLY, 100, enabled = false)), 0.001f)
    }

    @Test
    fun maxSteps_capsTheReportedValue() {
        val transformer = transformer()
        val config = settings(StepMode.MULTIPLY, 100, maxSteps = 80_000)

        val first = transformer.transform(2_500f, config)
        assertTrue("first=$first", first >= 80_000f && first <= 80_100f)
        assertEquals(1_000f, transformer.transform(10f, config), 0.001f)
    }

    @Test
    fun maxSteps_appliesToReplaceMode() {
        val transformer = transformer()
        val config = settings(StepMode.REPLACE, 90_000, maxSteps = 80_000)

        val reported = transformer.transform(10f, config)
        assertTrue("reported=$reported", reported >= 80_000f && reported <= 80_100f)
    }

    @Test
    fun maxSteps_capsTheAbsoluteReportedValue() {
        val today = LocalDate.of(2026, 10, 22)
        val transformer = StepTransformer(clock = { today }, extraSteps = { 0 })
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 50_000)

        // Below the cap the real value passes through; at or above it the report is clamped.
        assertEquals(30_000f, transformer.transform(30_000f, config), 0.001f)
        assertEquals(50_000f, transformer.transform(60_000f, config), 0.001f)
        assertEquals(50_000f, transformer.transform(120_000f, config), 0.001f)
    }

    @Test
    fun loweringMaxSteps_stopsGrowthImmediately() {
        val today = LocalDate.of(2026, 10, 22)
        val transformer = StepTransformer(clock = { today }, extraSteps = { 0 })
        val original = settings(StepMode.MULTIPLY, 1, maxSteps = 50_000)
        val lowered = settings(StepMode.MULTIPLY, 1, maxSteps = 30_000)

        assertEquals(50_000f, transformer.transform(60_000f, original), 0.001f)
        // The user lowers the cap mid-day: further reports do not exceed the new value.
        assertEquals(30_000f, transformer.transform(90_000f, lowered), 0.001f)
        assertEquals(30_000f, transformer.transform(150_000f, lowered), 0.001f)
    }

    @Test
    fun dailyLimit_isMaxStepsPlusUpTo100() {
        val transformer = transformer(extra = 42)
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 80_000)

        assertEquals(80_042f, transformer.transform(100_000f, config), 0.001f)
    }

    @Test
    fun dailyLimit_changesWithEachDay() {
        var today = LocalDate.of(2026, 10, 22)
        val extras = mapOf(today.toEpochDay() to 10, today.plusDays(1).toEpochDay() to 90)
        val transformer = StepTransformer(clock = { today }, extraSteps = { extras[it] ?: 0 })
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 80_000)

        // Day 1: 80_000 + 10.
        assertEquals(80_010f, transformer.transform(100_000f, config), 0.001f)

        today = today.plusDays(1)
        // A new day restarts from zero; today's budget is 80_000 + 90.
        assertEquals(80_090f, transformer.transform(100_000f, config), 0.001f)
    }

    @Test
    fun dailyLimit_isStableForTheSameDay() {
        val today = LocalDate.of(2026, 10, 22)
        val first = StepTransformer(clock = { today }).transform(100_000f, settings(StepMode.MULTIPLY, 1, maxSteps = 80_000))
        val second = StepTransformer(clock = { today }).transform(100_000f, settings(StepMode.MULTIPLY, 1, maxSteps = 80_000))
        assertEquals(first, second, 0.001f)
    }

    @Test
    fun newDay_resetsTheReportedValueToZero() {
        val today = LocalDate.of(2026, 10, 22)
        val transformer = StepTransformer(clock = { today + java.time.Period.ofDays(0) }, extraSteps = { 0 })
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 50_000)

        assertEquals(50_000f, transformer.transform(80_000f, config), 0.001f)
    }

    @Test
    fun state_survivesProcessRestart() {
        val store = MemoryStore()
        val today = LocalDate.of(2026, 10, 22)
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 80_000)

        val first = StepTransformer(clock = { today }, extraSteps = { 0 }, store = store)
        assertEquals(80_000f, first.transform(100_000f, config), 0.001f)

        // A new process restores the day's baseline and keeps the cap enforced.
        val second = StepTransformer(clock = { today }, extraSteps = { 0 }, store = store)
        assertEquals(80_000f, second.transform(190_000f, config), 0.001f)
    }

    @Test
    fun dayRollover_restartsFromZero() {
        val store = MemoryStore()
        var today = LocalDate.of(2026, 10, 22)
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 80_000)

        val first = StepTransformer(clock = { today }, extraSteps = { 0 }, store = store)
        assertEquals(80_000f, first.transform(100_000f, config), 0.001f)

        today = today.plusDays(1)
        // Yesterday's total must not carry over; the new day counts from zero again.
        val second = StepTransformer(clock = { today }, extraSteps = { 0 }, store = store)
        assertEquals(80_000f, second.transform(100_000f, config), 0.001f)
        assertEquals(80_000f, second.transform(260_000f, config), 0.001f)
    }

    @Test
    fun result_isClampedAndSanitized() {
        val transformer = StepTransformer()
        assertEquals(0f, transformer.transform(-5f, settings(StepMode.MULTIPLY, 100)), 0.001f)
        assertEquals(
            StepTransformer.MAX_STEPS,
            transformer.transform(Float.MAX_VALUE, settings(StepMode.ADD, 80_000)),
            0.001f
        )
    }
}
