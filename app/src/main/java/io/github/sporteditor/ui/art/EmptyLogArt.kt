package io.github.sporteditor.ui.art

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.sporteditor.ui.theme.animationsEnabled

/**
 * Illustration for an empty log list: a sheet of paper with a folded corner that drifts gently.
 *
 * @param searching draws a magnifier next to the sheet, used when a filter or a query hides every
 *   entry instead of the log being genuinely empty
 */
@Composable
fun EmptyLogArt(searching: Boolean, modifier: Modifier = Modifier) {
    // An infinite drift can only be frozen, not collapsed, so with animations off the sheet simply
    // rests at its neutral position instead of freezing at an arbitrary offset and opacity.
    val animations = animationsEnabled()
    val drift = if (animations) {
        val transition = rememberInfiniteTransition(label = "empty-log")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "empty-log-drift"
        ).value
    } else {
        0.5f
    }

    val outline = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier.graphicsLayer {
            translationY = (drift - 0.5f) * 3.dp.toPx()
            alpha = 0.88f + 0.12f * drift
        }
    ) {
        val stroke = 1.5.dp.toPx()
        val left = size.width * 0.12f
        val right = size.width * if (searching) 0.60f else 0.74f
        val top = size.height * 0.08f
        val bottom = size.height * 0.92f
        val fold = (right - left) * 0.24f

        drawRoundRect(
            color = outline,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = stroke)
        )

        // Folded corner.
        val foldPath = Path().apply {
            moveTo(right - fold, top)
            lineTo(right - fold, top + fold)
            lineTo(right, top + fold)
        }
        drawPath(path = foldPath, color = outline, style = Stroke(width = stroke))

        // Text lines.
        val lineHeight = bottom - top
        val lineInset = 6.dp.toPx()
        listOf(
            0.34f to 0.62f,
            0.52f to 0.74f,
            0.70f to 0.48f
        ).forEach { (y, widthFactor) ->
            val lineY = top + lineHeight * y
            drawLine(
                color = outline.copy(alpha = 0.6f),
                start = Offset(left + lineInset, lineY),
                end = Offset(left + lineInset + (right - left - lineInset * 2f) * widthFactor, lineY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        if (searching) {
            val center = Offset(size.width * 0.74f, size.height * 0.62f)
            val radius = size.height * 0.15f
            drawCircle(color = accent, radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))
            val handleStart = Offset(center.x + radius * 0.707f, center.y + radius * 0.707f)
            drawLine(
                color = accent,
                start = handleStart,
                end = Offset(handleStart.x + radius * 0.75f, handleStart.y + radius * 0.75f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
