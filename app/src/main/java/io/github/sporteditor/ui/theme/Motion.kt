package io.github.sporteditor.ui.theme

import android.animation.ValueAnimator
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
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
 * Timings and curves mirror the Material 3 standard motion tokens (emphasized-decelerate easing
 * `cubic-bezier(0.2, 0, 0, 1)`, short4 200 ms / medium2 300 ms) rather than inventing new ones.
 * They are plain values, not composable reads, so they also work inside `transitionSpec` and
 * `Modifier.animateItem`.
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
     */
    fun settle(): FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Content appearing or changing layout (expansion, list insertion, forward navigation). */
    fun <T> enter(): FiniteAnimationSpec<T> = tween(durationMillis = 300, easing = Standard)

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
