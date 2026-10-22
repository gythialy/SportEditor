package io.github.gythialy.sporteditor.hook

import io.github.gythialy.sporteditor.data.StepMode
import io.github.gythialy.sporteditor.data.TargetSettings

/**
 * Applies a [TargetSettings] to a raw sensor step value.
 *
 * One instance belongs to one hooked process; it keeps the auto-increment counter.
 * The class is intentionally free of Android dependencies so it can be unit tested.
 */
class StepTransformer {
    private var autoCount = 0

    @Synchronized
    fun transform(raw: Float, settings: TargetSettings): Float {
        if (!settings.enabled) return raw

        val reported = when (settings.mode) {
            StepMode.MULTIPLY -> raw * settings.value

            StepMode.ADD -> raw + settings.value

            StepMode.REPLACE -> settings.value.toFloat()

            StepMode.AUTO -> {
                val increment = settings.value.toLong() * autoCount
                if (increment > TargetSettings.AUTO_CAP) {
                    autoCount = 0
                    raw
                } else {
                    autoCount++
                    raw + increment
                }
            }
        }
        return sanitize(reported.coerceAtMost(settings.maxSteps.toFloat()))
    }

    private fun sanitize(value: Float): Float = when {
        value.isNaN() || value.isInfinite() -> 0f
        value < 0f -> 0f
        value > MAX_STEPS -> MAX_STEPS
        else -> value
    }

    companion object {
        /** Upper bound reported to target apps to avoid overflow in their own math. */
        const val MAX_STEPS = 9_999_999f
    }
}
