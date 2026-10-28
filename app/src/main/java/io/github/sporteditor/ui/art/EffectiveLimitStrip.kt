package io.github.sporteditor.ui.art

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * Gauge for the daily step budget: the base value the user configures and the deterministic
 * 0..100 random offset that applies to today.
 *
 * This is deliberately a scale with ticks instead of a progress bar: the value is not "progress",
 * it is a position inside the random range the hook applies, and a filled bar would read as usage.
 *
 * @param fraction position inside the range, 0 for the base value and 1 for base + max offset
 */
@Composable
fun EffectiveLimitStrip(baseLabel: String, offsetLabel: String, fraction: Float, modifier: Modifier = Modifier) {
    val position by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "limit-strip-position"
    )
    val track = MaterialTheme.colorScheme.outlineVariant
    val tick = MaterialTheme.colorScheme.outline
    val marker = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = baseLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = offsetLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .padding(horizontal = 2.dp)
        ) {
            val centerY = size.height / 2f
            val startX = 3.dp.toPx()
            val endX = size.width - 3.dp.toPx()
            val span = endX - startX

            drawLine(
                color = track,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Ticks mark the full 0..100 range so the marker position stays readable.
            repeat(5) { index ->
                val x = startX + span * index / 4f
                drawLine(
                    color = tick.copy(alpha = 0.7f),
                    start = Offset(x, centerY - 3.dp.toPx()),
                    end = Offset(x, centerY + 3.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val markerX = startX + span * position
            drawCircle(
                color = marker.copy(alpha = 0.16f),
                radius = 6.dp.toPx(),
                center = Offset(markerX, centerY)
            )
            drawCircle(
                color = marker,
                radius = 3.dp.toPx(),
                center = Offset(markerX, centerY)
            )
        }
    }
}
