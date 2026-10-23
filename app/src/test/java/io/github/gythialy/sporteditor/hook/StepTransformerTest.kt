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

    private fun detector(extra: Int = 0, today: LocalDate = LocalDate.of(2026, 10, 22), store: DailyStateStore? = null) =
        StepTransformer(clock = { today }, extraSteps = { extra }, store = store, incremental = true)

    private class MemoryStore : DailyStateStore {
        var state: DailyState? = null

        override fun load(): DailyState? = state

        override fun save(state: DailyState) {
            this.state = state
        }
    }

    @Test
    fun multiply_countsOnlyTheDeltaOfTheDay() {
        val transformer = StepTransformer()
        val config = settings(StepMode.MULTIPLY, 100)

        assertEquals(0f, transformer.transform(2_500f, config), 0.001f)
        assertEquals(50_000f, transformer.transform(3_000f, config), 0.001f)
        assertEquals(150_000f, transformer.transform(4_000f, config), 0.001f)
    }

    @Test
    fun add_appendsTheOffsetOncePerDay() {
        val transformer = StepTransformer()
        val config = settings(StepMode.ADD, 80_000)

        assertEquals(80_000f, transformer.transform(2_500f, config), 0.001f)
        assertEquals(80_100f, transformer.transform(2_600f, config), 0.001f)
    }

    @Test
    fun replace_returnsAFixedValueForTheWholeDay() {
        val transformer = StepTransformer()
        val config = settings(StepMode.REPLACE, 88_888)

        assertEquals(88_888f, transformer.transform(12f, config), 0.001f)
        assertEquals(88_888f, transformer.transform(999f, config), 0.001f)
    }

    @Test
    fun auto_addsTheValueOnEveryCallback() {
        val transformer = StepTransformer()
        val config = settings(StepMode.AUTO, 10_000)

        assertEquals(0f, transformer.transform(1_000f, config), 0.001f)
        assertEquals(10_000f, transformer.transform(1_000f, config), 0.001f)
        assertEquals(20_000f, transformer.transform(1_000f, config), 0.001f)
    }

    @Test
    fun auto_restartsAfterCap() {
        val transformer = StepTransformer()
        val config = settings(StepMode.AUTO, 60_000)

        // 0, 60000, then 120000 > AUTO_CAP resets the counter for the day.
        assertEquals(0f, transformer.transform(1_000f, config), 0.001f)
        assertEquals(60_000f, transformer.transform(1_000f, config), 0.001f)
        assertEquals(0f, transformer.transform(1_000f, config), 0.001f)
        assertEquals(0f, transformer.transform(1_000f, config), 0.001f)
        assertEquals(60_000f, transformer.transform(1_000f, config), 0.001f)
    }

    @Test
    fun disabled_passesRawValueThrough() {
        val transformer = StepTransformer()
        assertEquals(321f, transformer.transform(321f, settings(StepMode.MULTIPLY, 100, enabled = false)), 0.001f)
    }

    @Test
    fun maxSteps_capsTodaysIncrement() {
        val transformer = transformer()
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 80_000)

        assertEquals(0f, transformer.transform(2_500f, config), 0.001f)
        assertEquals(80_000f, transformer.transform(100_000f, config), 0.001f)
        // The counter keeps growing in the real world, but today's budget is exhausted.
        assertEquals(80_000f, transformer.transform(200_000f, config), 0.001f)
    }

    @Test
    fun maxSteps_appliesToReplaceMode() {
        val transformer = transformer()
        val config = settings(StepMode.REPLACE, 90_000, maxSteps = 80_000)

        assertEquals(80_000f, transformer.transform(10f, config), 0.001f)
    }

    @Test
    fun loweringMaxSteps_stopsGrowthImmediately() {
        val transformer = transformer()
        val original = settings(StepMode.MULTIPLY, 1, maxSteps = 50_000)
        val lowered = settings(StepMode.MULTIPLY, 1, maxSteps = 30_000)

        assertEquals(0f, transformer.transform(2_500f, original), 0.001f)
        assertEquals(50_000f, transformer.transform(60_000f, original), 0.001f)
        // The user lowers the cap mid-day: today's increment never exceeds the new value.
        assertEquals(30_000f, transformer.transform(90_000f, lowered), 0.001f)
        assertEquals(30_000f, transformer.transform(150_000f, lowered), 0.001f)
    }

    @Test
    fun dailyLimit_isMaxStepsPlusUpTo100() {
        val transformer = transformer(extra = 42)
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 80_000)

        assertEquals(0f, transformer.transform(2_500f, config), 0.001f)
        assertEquals(80_042f, transformer.transform(100_000f, config), 0.001f)
    }

    @Test
    fun dailyLimit_changesWithEachDay() {
        var today = LocalDate.of(2026, 10, 22)
        val extras = mapOf(today.toEpochDay() to 10, today.plusDays(1).toEpochDay() to 90)
        val transformer = StepTransformer(clock = { today }, extraSteps = { extras[it] ?: 0 })
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 80_000)

        assertEquals(0f, transformer.transform(2_500f, config), 0.001f)
        // Day 1: 80_000 + 10.
        assertEquals(80_010f, transformer.transform(100_000f, config), 0.001f)

        today = today.plusDays(1)
        // The absolute counter carries over; the second day earns its own 80_000 + 90 budget.
        assertEquals(80_010f, transformer.transform(100_000f, config), 0.001f)
        assertEquals(160_100f, transformer.transform(200_000f, config), 0.001f)
    }

    @Test
    fun dayRollover_keepsTheAbsoluteCounterContinuous() {
        val store = MemoryStore()
        var today = LocalDate.of(2026, 10, 22)
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 80_000)

        val first = StepTransformer(clock = { today }, extraSteps = { 0 }, store = store)
        assertEquals(0f, first.transform(0f, config), 0.001f)
        assertEquals(80_000f, first.transform(100_000f, config), 0.001f)

        today = today.plusDays(1)
        val second = StepTransformer(clock = { today }, extraSteps = { 0 }, store = store)
        // The first event of the new day has no delta, so the target app never sees a jump.
        assertEquals(80_000f, second.transform(100_000f, config), 0.001f)
        assertEquals(160_000f, second.transform(180_000f, config), 0.001f)
    }

    @Test
    fun state_survivesProcessRestart() {
        val store = MemoryStore()
        val today = LocalDate.of(2026, 10, 22)
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 80_000)

        val first = StepTransformer(clock = { today }, extraSteps = { 0 }, store = store)
        assertEquals(0f, first.transform(0f, config), 0.001f)
        assertEquals(60_000f, first.transform(60_000f, config), 0.001f)

        // A new process restores the day's baseline instead of restarting from zero.
        val second = StepTransformer(clock = { today }, extraSteps = { 0 }, store = store)
        assertEquals(80_000f, second.transform(80_000f, config), 0.001f)
    }

    @Test
    fun deviceReboot_keepsTodaysProgress() {
        val store = MemoryStore()
        val today = LocalDate.of(2026, 10, 22)
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 80_000)

        val transformer = StepTransformer(clock = { today }, extraSteps = { 0 }, store = store)
        assertEquals(0f, transformer.transform(0f, config), 0.001f)
        assertEquals(30_000f, transformer.transform(30_000f, config), 0.001f)

        // STEP_COUNTER restarts at zero after a reboot: no jump, and the rest of the day's
        // budget keeps accumulating from the new baseline.
        assertEquals(30_000f, transformer.transform(0f, config), 0.001f)
        assertEquals(50_000f, transformer.transform(20_000f, config), 0.001f)
        assertEquals(80_000f, transformer.transform(200_000f, config), 0.001f)
    }

    @Test
    fun legacyAbsoluteState_migratesWithoutBackwardJump() {
        val store = MemoryStore()
        val today = LocalDate.of(2026, 10, 22)
        store.state = DailyState(
            epochDay = today.toEpochDay(),
            baselineRaw = 0f,
            lastRaw = 0f,
            rawDeltaOffset = 0f,
            dayStartReported = 0f,
            dailyLimit = 80_000,
            lastReported = 80_000f,
            autoCount = 0,
            version = 0
        )
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 80_000)
        val transformer = StepTransformer(clock = { today }, extraSteps = { 0 }, store = store)

        // The v1.2 state counted from an absolute value; the upgrade keeps it and starts a new
        // baseline so the target app does not see the counter drop.
        assertEquals(80_000f, transformer.transform(50_000f, config), 0.001f)
        assertEquals(130_000f, transformer.transform(100_000f, config), 0.001f)
    }

    @Test
    fun counterRollover_wrapsToZeroWhenTheAbsoluteValueWouldOverflow() {
        val store = MemoryStore()
        var today = LocalDate.of(2026, 10, 22)
        val config = settings(StepMode.MULTIPLY, 1, maxSteps = 9_000_000)

        val first = StepTransformer(clock = { today }, extraSteps = { 0 }, store = store)
        assertEquals(0f, first.transform(0f, config), 0.001f)
        assertEquals(9_000_000f, first.transform(9_000_000f, config), 0.001f)

        today = today.plusDays(1)
        val second = StepTransformer(clock = { today }, extraSteps = { 0 }, store = store)
        // 9_000_000 + 9_000_000 > MAX_VALUE, so the new day restarts the absolute counter.
        assertEquals(0f, second.transform(9_000_000f, config), 0.001f)
    }

    @Test
    fun detector_multiply_accumulatesEveryEvent() {
        val transformer = detector()
        val config = settings(StepMode.MULTIPLY, 100, maxSteps = 80_000)

        // TYPE_STEP_DETECTOR reports 1.0 per step; the delta is the event count.
        assertEquals(100f, transformer.transform(1f, config), 0.001f)
        assertEquals(200f, transformer.transform(1f, config), 0.001f)
        repeat(798) { transformer.transform(1f, config) }
        assertEquals(80_000f, transformer.transform(1f, config), 0.001f)
    }

    @Test
    fun detector_add_appendsTheOffsetOncePerDay() {
        val transformer = detector()
        val config = settings(StepMode.ADD, 80_000)

        assertEquals(80_001f, transformer.transform(1f, config), 0.001f)
        assertEquals(80_002f, transformer.transform(1f, config), 0.001f)
    }

    @Test
    fun detector_auto_ignoresTheEventValue() {
        val transformer = detector()
        val config = settings(StepMode.AUTO, 10_000)

        assertEquals(0f, transformer.transform(1f, config), 0.001f)
        assertEquals(10_000f, transformer.transform(1f, config), 0.001f)
        assertEquals(20_000f, transformer.transform(1f, config), 0.001f)
    }

    @Test
    fun detector_dayRollover_carriesTheAbsoluteCounter() {
        val store = MemoryStore()
        var today = LocalDate.of(2026, 10, 22)
        val config = settings(StepMode.MULTIPLY, 100, maxSteps = 80_000)

        val first = detector(today = today, store = store)
        assertEquals(100f, first.transform(1f, config), 0.001f)
        assertEquals(200f, first.transform(1f, config), 0.001f)

        today = today.plusDays(1)
        val second = detector(today = today, store = store)
        assertEquals(300f, second.transform(1f, config), 0.001f)
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

    @Test
    fun report_isAlwaysWithinTheEffectiveLimit() {
        val today = LocalDate.of(2026, 10, 22)
        val extras = mapOf(today.toEpochDay() to 100)
        val transformer = StepTransformer(clock = { today }, extraSteps = { extras[it] ?: 0 })
        val config = settings(StepMode.MULTIPLY, 3, maxSteps = 80_000)
        val seen = mutableListOf<Float>()

        var raw = 0f
        repeat(50) {
            raw += 1_000f
            seen += transformer.transform(raw, config)
        }

        assertTrue("reports=$seen", seen.all { it >= 0f && it <= 80_100f })
        assertEquals(80_100f, seen.last(), 0.001f)
    }
}
