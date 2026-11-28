package io.github.gythialy.sporteditor.ui.art

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.gythialy.sporteditor.ui.theme.Motion
import io.github.gythialy.sporteditor.ui.theme.Signal
import io.github.gythialy.sporteditor.ui.theme.animationsEnabled
import kotlinx.coroutines.delay

/**
 * The Live Circuit signature: a hub (the hook) wired to four nodes (the target apps).
 *
 * The still frame is the contract — solid line means connected, a gap in the middle means not,
 * hollow nodes mean not enabled. Animation only narrates a transition: on connect a pulse runs
 * from the hub to the enabled nodes; on disconnect the line breaks with one spark and stays broken.
 * No animation is required to understand the state, so a "Remove animations" setting loses nothing.
 *
 * @param connected whether the Xposed framework is live
 * @param enabledCount how many of the four targets are switched on (0..4)
 * @param compact 32 dp app-bar glyph vs 144 dp detail plate; both use the same geometry
 */
@Composable
fun ConnectionMap(
    connected: Boolean,
    enabledCount: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    animateChange: Boolean = true
) {
    val animations = animationsEnabled()
    val signal = Signal.core()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val error = MaterialTheme.colorScheme.error
    val halo = Signal.halo()

    // A single 0..1 value drives both the wire pulse and the break. It is derived from connected,
    // so an interrupted transition just re-targets instead of restarting.
    val target = if (connected) 1f else 0f
    val animatedTravel by animateFloatAsState(
        targetValue = target,
        animationSpec = Motion.pulse(),
        label = "connection-map-travel"
    )
    val shownTravel = if (animations && animateChange) animatedTravel else target

    Canvas(modifier = modifier) {
        // Derive every radius from the short edge: the sheet gives us a wide box, and using the
        // width there would push the side nodes off-canvas and blow up the hub.
        val unit = minOf(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = unit * (if (compact) 0.34f else 0.36f)
        val hubR = unit * (if (compact) 0.09f else 0.07f)
        val nodeR = unit * (if (compact) 0.055f else 0.045f)
        val stroke = if (compact) 1.5f else 2.5f

        val nodes = listOf(
            Offset(cx, cy - r),
            Offset(cx + r, cy),
            Offset(cx, cy + r),
            Offset(cx - r, cy)
        )

        nodes.forEachIndexed { index, node ->
            val enabled = index < enabledCount.coerceIn(0, 4)
            val lit = connected && enabled
            val lineColor = when {
                !connected -> muted.copy(alpha = 0.5f)
                lit -> signal
                else -> muted.copy(alpha = 0.45f)
            }

            // The wire. A broken link leaves a visible gap at the midpoint; a live link is solid
            // and, while the pulse is travelling, shows a brighter leading segment.
            val gap = if (!connected) r * 0.42f else 0f
            val mid = Offset((cx + node.x) / 2f, (cy + node.y) / 2f)
            val pulseHead = if (connected && shownTravel < 1f) {
                Offset(
                    cx + (node.x - cx) * shownTravel,
                    cy + (node.y - cy) * shownTravel
                )
            } else {
                null
            }

            if (gap > 0f) {
                val dx = (node.x - cx) / r * gap
                val dy = (node.y - cy) / r * gap
                drawLine(
                    color = lineColor,
                    start = Offset(cx, cy),
                    end = Offset(mid.x - dx, mid.y - dy),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = lineColor,
                    start = Offset(mid.x + dx, mid.y + dy),
                    end = node,
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                if (index == 0) {
                    // One spark, drawn only at rest on a broken link. It is a shape, not a glow.
                    drawCircle(color = error, radius = nodeR * 0.7f, center = mid)
                }
            } else {
                drawLine(
                    color = lineColor,
                    start = Offset(cx, cy),
                    end = node,
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                if (pulseHead != null && lit) {
                    drawCircle(color = signal, radius = nodeR * 0.9f, center = pulseHead)
                }
            }

            // Node: hollow when off, filled ring when enabled, halo only when live.
            if (lit) {
                drawCircle(color = halo, radius = nodeR * 2f, center = node)
            }
            drawCircle(
                color = if (enabled) lineColor else muted.copy(alpha = 0.4f),
                radius = nodeR,
                center = node,
                style = Stroke(width = stroke)
            )
            if (lit) {
                drawCircle(color = signal, radius = nodeR * 0.5f, center = node)
            }
        }

        // Hub: two quiet rings when connected, a static dot when not. No blur, no shadow.
        if (connected) {
            drawCircle(color = halo, radius = hubR * 2.2f, center = Offset(cx, cy))
            drawCircle(
                color = signal.copy(alpha = 0.45f),
                radius = hubR * 1.5f,
                center = Offset(cx, cy),
                style = Stroke(width = stroke)
            )
        }
        drawCircle(color = if (connected) signal else muted, radius = hubR, center = Offset(cx, cy))
    }
}
