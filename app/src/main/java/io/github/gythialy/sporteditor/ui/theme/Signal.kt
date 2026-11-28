package io.github.gythialy.sporteditor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Live Circuit's light vocabulary. Every role is derived from the theme scheme, so it follows
 * dynamic colour and both themes without a second palette to keep in sync.
 *
 * Light is earned, never decorative: at most two animated light sources on one screen, and never
 * on top of text. Keep the count in mind when adding a new one.
 */
object Signal {
    /** The single signal hue: the brand green under fallback, the harmonised one under Monet. */
    @Composable
    fun core(): Color = brandAccent()

    /** Low-opacity wash for a live surface such as the limit scale. */
    @Composable
    fun wash(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        .compositeOver(cardContainerColor())

    /** Edge of a lit rail: a real edge in light mode, a faint rim in dark mode. */
    @Composable
    fun rim(): Color = cardBorderColor()

    /** Halo around a live marker. Two alpha steps keep it readable without a blur. */
    @Composable
    fun halo(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
}
