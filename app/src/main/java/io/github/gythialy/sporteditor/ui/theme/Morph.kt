package io.github.gythialy.sporteditor.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/**
 * A [Shape] that is part-way between two Material shapes at [progress] (0 = [from], 1 = [to]).
 *
 * Material's own `MaterialShapes` / `Morph` API only draws a path for one fixed progress, so a
 * morphing shape has to rebuild the path each frame. That cost is only acceptable on small, briefly
 * animated surfaces: the step button during a press, where the path is ~56 dp and the animation is
 * over in 100 ms. Do not reach for this on cards or anything that fills the screen.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberMorphShape(from: RoundedPolygon, to: RoundedPolygon, progress: Float): Shape {
    val morph = remember(from, to) { Morph(from, to) }
    return remember(morph, progress) {
        val morphed = morph.toPath(progress, Path())
        object : Shape {
            // The morph path arrives normalized to 0..1, so it is scaled to the layout size and
            // centred the same way Material's own `toShape` does it.
            private val stored = morphed
            private var workPath: Path? = null
            private var lastSize = Size.Unspecified

            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                if (size != lastSize || workPath == null) {
                    lastSize = size
                    workPath = Path()
                } else {
                    workPath!!.rewind()
                }
                val output = workPath!!
                output.addPath(stored)
                output.transform(Matrix().apply { scale(x = size.width, y = size.height) })
                output.translate(size.center - output.getBounds().center)
                return Outline.Generic(output)
            }
        }
    }
}
