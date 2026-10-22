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
        // A new day starts from the previous report; today's budget is 80_000 + 90.
        assertEquals(80_010f, transformer.transform(100_000f, config), 0.001f)
        assertEquals(160_100f, transformer.transform(180_090f, config), 0.001f)
    }

    @Test
    fun dailyLimit_isStableForTheSameDay() {
        val today = LocalDate.of(2026, 10, 22)
        val first = StepTransformer(clock = { today }).transform(100_000f, settings(StepMode.MULTIPLY, 1, maxSteps = 80_000))
        val second = StepTransformer(clock = { today }).transform(100_000f, settings(StepMode.MULTIPLY, 1, maxSteps = 80_000))
        assertEquals(first, second, 0.001f)
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
