package io.github.gythialy.sporteditor.ui.theme

import android.animation.ValueAnimator
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
 *  - [glide] — a selected indicator travelling between positions (mode segment, tab signal head).
 *  - [pulse] — one-shot energy travelling along a live connection, then decaying to a still frame.
 *
 * [settle] and [enter] read the theme's [androidx.compose.material3.MotionScheme]: the expressive
 * *effects* spring for readouts (critically damped, so a number never overshoots) and the
 * expressive *spatial* spring for layout (a little weight, still no visible bounce). The scheme is
 * a composable read, so these two are `@Composable` and must be captured outside coroutine blocks
 * (`LaunchedEffect`, `transitionSpec`).
 *
 * [press], [leave] and [pulse] own their timing (100 ms, 150 ms and 520 ms tweens) because their
 * durations are the point, not a physical response: a touch acknowledgement has to be over before
 * the next input, an exit has to finish before the next state arrives, and a pulse has to decay to
 * a readable still frame. They stay plain values so they also work inside non-composable lambdas.
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
     * A selected indicator travelling between positions. No bounce: an indicator that overshoots
     * reads as undecided. Stiffer than [enter] because the user's finger is already there.
     */
    fun <T> glide(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** One-shot energy travelling along a live connection, then decaying to a still frame. */
    fun <T> pulse(): FiniteAnimationSpec<T> = tween(durationMillis = 520, easing = Standard)

    /** Delay before a card expands after its switch flipped, so the switch thumb leads and the
     * layout follows instead of both competing for the same frame. */
    const val SWITCH_LEAD_MS = 80L

    /** Per-row delay when a card builds itself up; the caller caps the row count. */
    const val STAGGER_STEP_MS = 60L

    /** Per-card delay of the Live Circuit entry chain. Four cards finish within 210 ms. */
    const val ENERGY_STAGGER_MS = 70L

    /** How long a value-commit pulse stays visible before it decays to the static rail. */
    const val PULSE_DECAY_MS = 900L

    /** Period of the connected-state beacon. Only the beacon may loop. */
    const val BEACON_CYCLE_MS = 2400L
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
