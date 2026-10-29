package io.github.sporteditor.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale shared by all three screens. Four values instead of the 28 loose dp literals
 * the screens used to carry, so a rhythm change is one edit:
 *
 *  - [Page] — page inset.
 *  - [Card] — inner padding of a card or a list row.
 *  - [Gap] — space between cards and between groups.
 *  - [Tight] — space inside a group of related rows.
 */
object Dimensions {
    val Page = 16.dp
    val Card = 16.dp
    val Gap = 12.dp
    val Tight = 8.dp

    /**
     * Beyond this a single-column layout stops stretching. Two and a half phone widths of body text
     * is the point where a tablet stops reading as a form and starts reading as a banner.
     */
    val ContentMaxWidth = 640.dp
}

/** Horizontal inset that centres [Dimensions.ContentMaxWidth] inside a screen of [width]. */
fun contentInset(width: Dp): Dp = ((width - Dimensions.ContentMaxWidth) / 2).coerceAtLeast(0.dp)

/**
 * Width of the window this composition is in, in dp.
 *
 * Not `Configuration.screenWidthDp`: in split-screen, freeform and foldable postures the screen and
 * the window are different sizes, and the screen value would keep a phone layout on a pane that is
 * narrower than a phone.
 */
@Composable
fun windowWidthDp(): Dp {
    val density = LocalDensity.current
    val widthPx = LocalWindowInfo.current.containerSize.width
    return with(density) { widthPx.toDp() }
}
