package io.github.gythialy.sporteditor.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import io.github.gythialy.sporteditor.ui.ThemeMode
import io.github.gythialy.sporteditor.ui.theme.color.Blend

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = TealSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightBackground,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceContainer,
    surfaceContainerLow = LightSurfaceContainer,
    surfaceContainer = Color(0xFFEAF1EC),
    surfaceContainerHigh = Color(0xFFE4ECE7),
    surfaceContainerHighest = Color(0xFFDEE7E1),
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkBackground,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF0A0F0C),
    surfaceContainerLow = DarkSurfaceContainer,
    surfaceContainer = Color(0xFF1F2823),
    surfaceContainerHigh = Color(0xFF232C27),
    surfaceContainerHighest = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

/** Resolves the effective dark/light state for [themeMode]. */
@Composable
fun ThemeMode.resolveDarkTheme(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * The one earned alert wash: the alert hue laid over a card at low opacity instead of used as a
 * fill. A filled `errorContainer` reads as pale paper in light and as a saturated slab in dark,
 * and under dynamic colour it becomes whatever the wallpaper's error role happens to be — the
 * disconnected notice was the loudest surface in the app in dark mode. A wash keeps the notice
 * recognisable in both themes while the alert hue itself stays on the icon, the title and the
 * status dot.
 */
@Composable
fun alertWash(): Color {
    val scheme = MaterialTheme.colorScheme
    return scheme.error.copy(alpha = ALERT_WASH_ALPHA).compositeOver(scheme.surfaceContainerLowest)
}

private const val ALERT_WASH_ALPHA = 0.12f

/**
 * Card edge that replaces the shadow as the depth cue. `outlineVariant` is only ~1.2:1 against the
 * card surface, so the rule uses a dedicated 3:1+ tone: a muted ink in light, `outline` in dark.
 */
@Composable
fun cardBorderColor(): Color {
    val scheme = MaterialTheme.colorScheme
    // Match the same luminance test cardContainerColor() uses, so the two never disagree.
    return if (scheme.background.luminance() < 0.5f) scheme.outline else LightCardRule
}

/**
 * Card surface that keeps the page/card hierarchy in both themes: white cards on the tinted
 * page in light mode, raised tonal containers in dark mode.
 */
@Composable
fun cardContainerColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() < 0.5f) {
        scheme.surfaceContainerLow
    } else {
        scheme.surfaceContainerLowest
    }
}

/**
 * The brand green, pulled toward the host palette so it stops fighting it.
 *
 * Under dynamic colour the system hands out a wallpaper palette and the brand hue is the only
 * identity left; harmonising rotates it by at most 15 degrees toward the palette's primary, keeping
 * its chroma and tone. Without dynamic colour the scheme primary already is the brand, so
 * harmonising it is a no-op.
 *
 * Use it for brand anchors only — the status dot, the about row, a version marker — not as a
 * replacement for every `primary` use: most primary roles belong to the surface palette.
 */
@Composable
fun brandAccent(): Color {
    val scheme = MaterialTheme.colorScheme
    val brand = if (scheme.background.luminance() < 0.5f) DarkPrimary else GreenPrimary
    return Color(Blend.harmonize(brand.toArgb(), scheme.primary.toArgb()))
}

@Composable
fun SportEditorTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val darkTheme = themeMode.resolveDarkTheme()
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors

        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // Every Material component that animates (switch, sheet, dialog, tab, FAB, date picker…)
        // reads its specs from this scheme, so one line switches the whole library to springs.
        // Deliberately not MaterialExpressiveTheme: its defaults fall back to a light-only
        // expressive palette, which would flatten the dark theme and the dynamic palette.
        motionScheme = MotionScheme.expressive(),
        typography = SportEditorTypography,
        shapes = SportEditorShapes,
        content = content
    )
}
