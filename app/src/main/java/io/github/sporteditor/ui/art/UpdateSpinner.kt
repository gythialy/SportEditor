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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import io.github.sporteditor.ui.theme.animationsEnabled

/**
 * Indeterminate spinner used while the update check is running.
 *
 * Replacement for a `CircularProgressIndicator` in an icon slot: the stock indicator has a fixed
 * size ladder and its own colors, while this one matches the 18dp icon next to the button label.
 */
@Composable
fun UpdateSpinner(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    // With animations off the arc stays still: the surrounding label already says "checking", and a
    // loop frozen mid-rotation would read as a broken control rather than a resting one.
    val animations = animationsEnabled()
    val angle = if (animations) {
        val transition = rememberInfiniteTransition(label = "update-spinner")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
            label = "update-spinner-angle"
        ).value
    } else {
        0f
    }

    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val inset = stroke / 2f
        rotate(angle) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}
