package io.github.sporteditor.ui.theme.color

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The vendored colour maths is only worth carrying if it behaves like Material's: these tests pin the
 * contract the brand anchor relies on (hue moves a little, chroma and tone stay put) plus the
 * contrast budget the UI promises.
 */
class BlendTest {

    /** Brand green in the light fallback palette (Color.kt: GreenPrimary). */
    private val brandLight = 0xFF006C4C.toInt()

    /** Brand green in the dark fallback palette (Color.kt: DarkPrimary). */
    private val brandDark = 0xFF7EDCB2.toInt()

    /** A wallpaper-style primary: tonal purple, what dynamic colour commonly hands out. */
    private val wallpaperPurple = 0xFF6750A4.toInt()

    /** A wallpaper-style primary at the other end of the wheel. */
    private val wallpaperOrange = 0xFF8B5000.toInt()

    private fun hueOf(argb: Int): Double = Cam.fromInt(argb).hue.toDouble()

    private fun chromaOf(argb: Int): Double = Cam.fromInt(argb).chroma.toDouble()

    private fun toneOf(argb: Int): Double = CamUtils.lstarFromInt(argb).toDouble()

    @Test
    fun harmonizeRotatesHueByAtMostFifteenDegrees() {
        val harmonized = Blend.harmonize(brandLight, wallpaperPurple)
        val distance = Blend.differenceDegrees(hueOf(brandLight), hueOf(harmonized))
        // The 15 degree cap applies to the requested hue; gamut solving can shift the colour it
        // actually returns by a fraction of a degree, so the bound is rounded out a little.
        assertTrue("hue moved $distance degrees", distance <= 16.0)
    }

    @Test
    fun harmonizeMovesHueTowardTheSource() {
        val harmonized = Blend.harmonize(brandLight, wallpaperPurple)
        val before = Blend.differenceDegrees(hueOf(brandLight), hueOf(wallpaperPurple))
        val after = Blend.differenceDegrees(hueOf(harmonized), hueOf(wallpaperPurple))
        assertTrue("distance grew from $before to $after", after < before)
    }

    @Test
    fun harmonizeKeepsTone() {
        val harmonized = Blend.harmonize(brandDark, wallpaperPurple)
        assertEquals(toneOf(brandDark), toneOf(harmonized), 1.0)
    }

    @Test
    fun harmonizeKeepsChromaWithinTolerance() {
        val harmonized = Blend.harmonize(brandLight, wallpaperOrange)
        // The gamut clip can cost chroma, but the hue must not turn the colour into a neutral.
        assertTrue(
            "chroma collapsed: ${chromaOf(harmonized)} vs ${chromaOf(brandLight)}",
            chromaOf(harmonized) > chromaOf(brandLight) * 0.6
        )
    }

    @Test
    fun harmonizeWithItselfIsStable() {
        val harmonized = Blend.harmonize(brandLight, brandLight)
        assertTrue(Blend.differenceDegrees(hueOf(brandLight), hueOf(harmonized)) < 1.0)
    }

    @Test
    fun harmonizedBrandKeepsIconContrastOnItsSurface() {
        // Icon/role pairs owe 3:1 against the surface they sit on.
        val harmonized = Blend.harmonize(brandLight, wallpaperPurple)
        val lightSurface = 0xFFFFFFFF.toInt()
        assertTrue(
            "contrast ${Blend.contrastRatio(harmonized, lightSurface)}",
            Blend.contrastRatio(harmonized, lightSurface) >= 3.0
        )
    }

    @Test
    fun darkBrandKeepsBodyTextContrast() {
        // Body text owes 4.5:1; the dark brand is used on the dark page colour.
        val harmonized = Blend.harmonize(brandDark, wallpaperPurple)
        val darkBackground = 0xFF0F1512.toInt()
        assertTrue(
            "contrast ${Blend.contrastRatio(harmonized, darkBackground)}",
            Blend.contrastRatio(harmonized, darkBackground) >= 4.5
        )
    }

    @Test
    fun contrastRatioIsSymmetricAndBounded() {
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        assertEquals(21.0, Blend.contrastRatio(white, black), 0.01)
        assertEquals(Blend.contrastRatio(white, black), Blend.contrastRatio(black, white), 0.0001)
        assertEquals(1.0, Blend.contrastRatio(white, white), 0.0001)
    }

    @Test
    fun rotationDirectionPrefersTheShorterArc() {
        assertEquals(1, Blend.rotationDirection(0.0, 90.0))
        assertEquals(-1, Blend.rotationDirection(0.0, 270.0))
        assertEquals(1, Blend.rotationDirection(350.0, 10.0))
    }
}
