package io.github.sporteditor.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import io.github.sporteditor.ui.ThemeMode

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
        typography = SportEditorTypography,
        content = content
    )
}
