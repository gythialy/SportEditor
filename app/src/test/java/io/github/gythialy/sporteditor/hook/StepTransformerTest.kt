package io.github.gythialy.sporteditor.hook

import io.github.gythialy.sporteditor.data.StepMode
import io.github.gythialy.sporteditor.data.TargetSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class StepTransformerTest {

    private fun settings(mode: StepMode, value: Int, enabled: Boolean = true, maxSteps: Int = TargetSettings.MAX_VALUE) =
        TargetSettings(enabled = enabled, mode = mode, value = value, maxSteps = maxSteps)

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
        val transformer = StepTransformer()
        val config = settings(StepMode.MULTIPLY, 100, maxSteps = 80_000)

        assertEquals(80_000f, transformer.transform(2_500f, config), 0.001f)
        assertEquals(1_000f, transformer.transform(10f, config), 0.001f)
    }

    @Test
    fun maxSteps_appliesToReplaceMode() {
        val transformer = StepTransformer()
        val config = settings(StepMode.REPLACE, 90_000, maxSteps = 80_000)

        assertEquals(80_000f, transformer.transform(10f, config), 0.001f)
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
