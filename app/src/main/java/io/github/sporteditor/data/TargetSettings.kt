package io.github.sporteditor.data

/**
 * Per-application step configuration.
 */
data class TargetSettings(
    val enabled: Boolean,
    val mode: StepMode,
    val value: Int,
    /** Upper bound of the steps added on one local day (not of the absolute sensor counter). */
    val maxSteps: Int = DEFAULT_MAX_STEPS
) {
    fun normalized(): TargetSettings = copy(
        value = clamp(mode, value),
        maxSteps = maxSteps.coerceIn(MIN_MAX_STEPS, MAX_VALUE)
    )

    fun maxValue(): Int = maxValueFor(mode)

    companion object {
        const val MIN_VALUE = 1
        const val MAX_VALUE = 9_999_999
        const val MAX_MULTIPLIER = 10_000
        const val MIN_MAX_STEPS = 1
        const val DEFAULT_MAX_STEPS = 80_000

        fun maxValueFor(mode: StepMode): Int = if (mode == StepMode.MULTIPLY) MAX_MULTIPLIER else MAX_VALUE

        fun clamp(mode: StepMode, value: Int): Int = value.coerceIn(MIN_VALUE, maxValueFor(mode))
    }
}
