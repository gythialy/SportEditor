package io.github.sporteditor.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.sporteditor.ui.theme.animationsEnabled

/**
 * "Two nodes that never link up" illustration for the disconnected state.
 *
 * The connector dashes keep flowing while a signal dot runs towards the gap and fades out inside
 * it, so the still frame already reads as a broken link when animations are turned off.
 *
 * Drawn with plain [Canvas] primitives instead of a Lottie asset: it inherits the error colors and
 * therefore follows dynamic color, light/dark mode and every theme override without extra mapping.
 *
 * @param color stroke color, defaults to the error container content color
 */
@Composable
fun LinkBrokenArt(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.error) {
    // With animations off the picture still has to read: dashes at rest and the signal dot parked
    // past the fade-out point, instead of a loop frozen at whatever phase the system stopped on.
    val animations = animationsEnabled()
    val phase = if (animations) {
        val transition = rememberInfiniteTransition(label = "link-broken")
        // One dash period per loop, so the repeat seams are invisible.
        val dash by transition.animateFloat(
            initialValue = 0f,
            targetValue = 7f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 1400, easing = LinearEasing)),
            label = "link-broken-dash"
        )
        val travel by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 3200, easing = LinearEasing)),
            label = "link-broken-travel"
        )
        dash to travel
    } else {
        0f to 0.9f
    }
    val dashPhase = phase.first
    val travel = phase.second

    Canvas(modifier = modifier) {
        val centerY = size.height / 2f
        val radius = (size.height * 0.16f).coerceAtMost(size.width * 0.07f)
        val inset = radius + size.height * 0.1f
        val gap = (size.width * 0.05f).coerceAtLeast(radius)
        val stroke = 1.5.dp.toPx()
        val nodeGap = radius + size.height * 0.06f

        val left = Offset(inset, centerY)
        val right = Offset(size.width - inset, centerY)
        val lineStart = inset + nodeGap
        val lineEnd = size.width - inset - nodeGap
        val gapLeft = size.width / 2f - gap
        val gapRight = size.width / 2f + gap

        // The permanent gap in the middle is what makes the "not connected" reading survive
        // without motion.
        if (lineStart < gapLeft) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.5.dp.toPx(), 3.5.dp.toPx()), dashPhase * 3.5.dp.toPx())
            drawLine(
                color = color.copy(alpha = 0.55f),
                start = Offset(lineStart, centerY),
                end = Offset(gapLeft, centerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
                pathEffect = dash
            )
            drawLine(
                color = color.copy(alpha = 0.55f),
                start = Offset(gapRight, centerY),
                end = Offset(lineEnd, centerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
                pathEffect = dash
            )

            // Signal dot: runs to the gap, then fades instead of crossing it.
            val fade = if (travel < 0.78f) 1f else ((1f - travel) / 0.22f).coerceIn(0f, 1f)
            val dotX = lineStart + (gapLeft - lineStart) * travel
            drawCircle(
                color = color,
                radius = 2.dp.toPx() + 0.8.dp.toPx() * fade,
                center = Offset(dotX, centerY),
                alpha = fade
            )
        }

        listOf(left, right).forEach { node ->
            drawCircle(color = color.copy(alpha = 0.12f), radius = radius, center = node)
            drawCircle(color = color.copy(alpha = 0.75f), radius = radius, center = node, style = Stroke(width = stroke))
        }
    }
}
