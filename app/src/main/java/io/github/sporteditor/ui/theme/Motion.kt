package io.github.sporteditor.ui.theme

import android.animation.ValueAnimator
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * The app's motion vocabulary. Four roles cover every animation in the app, so call sites name an
 * intent instead of repeating durations:
 *
 *  - [press] — immediate acknowledgement of a touch (button depression, arrow rotation).
 *  - [settle] — a readout finding its resting place (the step number counting to its new value).
 *  - [enter] — content arriving or reshaping (card expansion, log rows, screen transitions in).
 *  - [leave] — content going away; exits run faster so they never hold the next state back.
 *
 * [press] and [leave] own their timing (100 ms and 150 ms tweens) because their durations are the
 * point. [settle] takes the Material 3 expressive *effects* spring (critically damped, so a number
 * never overshoots) and [enter] takes the expressive *spatial* spring (a little weight, still no
 * visible bounce). They are plain values, not composable reads, so they also work inside
 * `transitionSpec` and `Modifier.animateItem`.
 */
object Motion {

    /** M3 standard easing: decelerate into place, no overshoot. */
    private val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** M3 emphasized-accelerate: leaves quickly and quietly. */
    private val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    // Material 3 expressive springs, as damping-ratio / stiffness pairs from the expressive motion
    // tokens. material3 1.4.0 ships `MotionScheme.expressive()` but keeps both the interface and the
    // theme function that feeds it internal, so the two specs this app needs are restated here;
    // once the project moves to material3 1.5 the scheme is public and this block can go away.
    //
    // Spatial specs move things (default 0.8 / 380): a 0.8 damping ratio reads as "no overshoot" for
    // layout, so cards and screens can land on their final size. Effects specs do not move anything
    // (default 1.0 / 1600): critically damped, so a step readout that re-targets every 80 ms tracks
    // the button instead of bouncing past the value and back.
    private const val EXPRESSIVE_SPATIAL_DAMPING = 0.8f
    private const val EXPRESSIVE_SPATIAL_STIFFNESS = 380f
    private const val EXPRESSIVE_EFFECTS_DAMPING = 1.0f
    private const val EXPRESSIVE_EFFECTS_STIFFNESS = 1600f

    /** Acknowledgement of a touch: short, on the effects channel, never blocks the next input. */
    fun <T> press(): FiniteAnimationSpec<T> = tween(durationMillis = 100, easing = Standard)

    /**
     * A readout moving to a new value. A spring rather than a tween because the step buttons can
     * repeat every 80 ms: each new value re-targets the same animation, and a spring absorbs that
     * interruption as a settle instead of restarting from zero.
     *
     * Uses the expressive *effects* spec on purpose: it is critically damped, so the number never
     * counts past its target and back.
     */
    fun settle(): FiniteAnimationSpec<Float> = spring(
        dampingRatio = EXPRESSIVE_EFFECTS_DAMPING,
        stiffness = EXPRESSIVE_EFFECTS_STIFFNESS
    )

    /** Content appearing or changing layout (expansion, list insertion, forward navigation). */
    fun <T> enter(): FiniteAnimationSpec<T> = spring(
        dampingRatio = EXPRESSIVE_SPATIAL_DAMPING,
        stiffness = EXPRESSIVE_SPATIAL_STIFFNESS
    )

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
    val view = LocalView.current
    return remember(view) { ValueAnimator.areAnimatorsEnabled() }
}
