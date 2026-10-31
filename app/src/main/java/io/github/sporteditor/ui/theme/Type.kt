package io.github.sporteditor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/**
 * Material 3 defaults; kept as a single extension point for future typography tweaks.
 *
 * The DESIGN.md type scale (24/22/16/14/12/11 with matching line heights) is the M3 token scale, so
 * the defaults already are the spec — nothing is restated here on purpose.
 */
val SportEditorTypography = Typography()

/**
 * The reading face for anything the user compares character by character: log lines, timestamps,
 * package names and environment values.
 *
 * System monospace rather than a bundled Roboto Mono: a 12 sp diagnostic row does not justify
 * ~200 KB of font assets in a 1.5 MB module APK.
 */
val ReadingMono = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.sp
)

/** [ReadingMono] at the label scale: badges, log tags and the target state block. */
val ReadingMonoLabel = ReadingMono.copy(fontSize = 11.sp)

/**
 * Figures that keep their advance width, so a counter growing from 9 to 10 does not nudge the row
 * it sits in. Applied to counts that change while the user is watching them.
 */
fun TextStyle.withTabularNumerals(): TextStyle = copy(fontFeatureSettings = "tnum")
