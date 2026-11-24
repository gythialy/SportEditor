package io.github.sporteditor.ui.theme.color

import kotlin.math.abs
import kotlin.math.min

/**
 * A cut-down port of Material Color Utilities' `Blend.harmonize`.
 *
 * Material's own `Blend` is not shipped in material3: the library keeps the colour maths in
 * `material3.internal.colorUtil` and exposes only `dynamicLightColorScheme`/`dynamicDarkColorScheme`.
 * `Hct`/`Cam16`/`ViewingConditions`/`Blend` are vendored into this package from the same AOSP
 * sources (Apache-2.0) so the app can anchor one brand colour inside a wallpaper-derived palette.
 *
 * Harmonising pulls a design colour's hue toward a source colour's hue by at most 15 degrees, keeping
 * its chroma and tone: the result still reads as the brand green rather than as the wallpaper colour,
 * but no longer fights the tones around it.
 */
object Blend {

    /** Maximum hue rotation applied to the design colour, in degrees. */
    private const val MAX_HUE_ROTATION = 15.0

    /**
     * Rotates [designColor]'s hue toward [sourceColor]'s hue by at most 15 degrees, keeping the
     * design colour's chroma and tone.
     *
     * Both inputs are opaque ARGB ints; the result is opaque. Passing the same colour twice returns
     * that colour unchanged.
     */
    fun harmonize(designColor: Int, sourceColor: Int): Int {
        val design = Cam.fromInt(designColor)
        val source = Cam.fromInt(sourceColor)
        val difference = differenceDegrees(design.hue.toDouble(), source.hue.toDouble())
        val rotation = min(difference * 0.5, MAX_HUE_ROTATION)
        val hue = sanitizeDegrees(
            design.hue + rotation * rotationDirection(design.hue.toDouble(), source.hue.toDouble())
        )
        return Cam.getInt(hue.toFloat(), design.chroma, CamUtils.lstarFromInt(designColor))
    }

    /** Shortest angular distance between two hues, in degrees (0..180). */
    fun differenceDegrees(from: Double, to: Double): Double = 180.0 - abs(abs(sanitizeDegrees(to - from)) - 180.0)

    /** 1 when [to] lies clockwise of [from] within 180 degrees, -1 otherwise. */
    fun rotationDirection(from: Double, to: Double): Int {
        val delta = sanitizeDegrees(to - from)
        return if (delta < 180.0) 1 else -1
    }

    /** Normalises an angle to 0..360. */
    fun sanitizeDegrees(degrees: Double): Double {
        val normalised = degrees % 360.0
        return if (normalised < 0.0) normalised + 360.0 else normalised
    }

    /** WCAG relative luminance of an opaque ARGB colour. */
    fun relativeLuminance(argb: Int): Double {
        fun channel(value: Int): Double {
            val v = value / 255.0
            return if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }

        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** WCAG contrast ratio between two opaque ARGB colours, 1.0..21.0. */
    fun contrastRatio(foreground: Int, background: Int): Double {
        val a = relativeLuminance(foreground)
        val b = relativeLuminance(background)
        val lighter = maxOf(a, b)
        val darker = minOf(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
