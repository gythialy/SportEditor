package io.github.gythialy.sporteditor.ui.theme

import android.animation.ValueAnimator
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The app's motion vocabulary. Four roles cover every animation in the app, so call sites name an
 * intent instead of repeating durations:
 *
 *  - [press] — immediate acknowledgement of a touch (button depression, arrow rotation).
 *  - [settle] — a readout finding its resting place (the step number counting to its new value).
 *  - [enter] — content arriving or reshaping (card expansion, log rows, screen transitions in).
 *  - [leave] — content going away; exits run faster so they never hold the next state back.
 *
 * [settle] and [enter] read the theme's [androidx.compose.material3.MotionScheme]: the expressive
 * *effects* spring for readouts (critically damped, so a number never overshoots) and the
 * expressive *spatial* spring for layout (a little weight, still no visible bounce). The scheme is
 * a composable read, so these two are `@Composable` and must be captured outside coroutine blocks
 * (`LaunchedEffect`, `transitionSpec`).
 *
 * [press] and [leave] own their timing (100 ms and 150 ms tweens) because their durations are the
 * point, not a physical response: a touch acknowledgement has to be over before the next input, and
 * an exit has to finish before the next state arrives. They stay plain values so they also work
 * inside non-composable lambdas.
 */
object Motion {

    /** M3 standard easing: decelerate into place, no overshoot. */
    private val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** M3 emphasized-accelerate: leaves quickly and quietly. */
    private val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Acknowledgement of a touch: short, on the effects channel, never blocks the next input. */
    fun <T> press(): FiniteAnimationSpec<T> = tween(durationMillis = 100, easing = Standard)

    /**
     * A readout moving to a new value. A spring rather than a tween because the step buttons can
     * repeat every 80 ms: each new value re-targets the same animation, and a spring absorbs that
     * interruption as a settle instead of restarting from zero.
     *
     * The theme's effects spring is critically damped, so the number never counts past its target.
     */
    @Composable
    fun settle(): FiniteAnimationSpec<Float> = MaterialTheme.motionScheme.defaultEffectsSpec()

    /** Content appearing or changing layout (expansion, list insertion, forward navigation). */
    @Composable
    fun <T> enter(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultSpatialSpec()

    /** Content leaving. Deliberately faster than [enter]. */
    fun <T> leave(): FiniteAnimationSpec<T> = tween(durationMillis = 150, easing = Accelerate)

    /**
     * Delay before a card expands after its switch flipped, so the switch thumb leads and the
     * layout follows instead of both competing for the same frame.
     */
    const val SWITCH_LEAD_MS = 80L

    /** Per-row delay when a card builds itself up; the caller caps the row count. */
    const val STAGGER_STEP_MS = 60L
}

/**
 * False when the user turned animations off (accessibility "Remove animations", or a developer
 * option set to 0).
 *
 * Tween and spring specs already collapse to an instant cut through Compose's duration scale, so
 * this only guards the two things that scale does not reach: coroutine `delay()` choreography and
 * infinite illustration loops, which would otherwise freeze at an arbitrary frame.
 */
@Composable
fun animationsEnabled(): Boolean {
    // Read on every composition: caching this by view made a runtime "Remove animations" toggle
    // invisible until the process restarted, while Compose's own duration scale already follows it.
    return ValueAnimator.areAnimatorsEnabled()
}
