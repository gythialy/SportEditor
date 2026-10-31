package io.github.sporteditor.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The DESIGN.md "16 / 12 rule" mapped onto the five Material 3 shape slots: 4 dp field, 8 dp chip,
 * 12 dp block, 16 dp card and group.
 *
 * `extraLarge` is pulled down to 16 dp on purpose. M3's 28 dp default would open bottom sheets and
 * dialogs with a corner the rest of the app never uses, and the spec allows exactly one "large
 * face" radius.
 */
val SportEditorShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

/**
 * The shape exceptions DESIGN.md allows, plus the pill. They live in one object so they cannot
 * spread: the log row is 14 dp, the level tag 7 dp and the tab indicator 2 dp because their own
 * heights make the rounded steps of the main scale read as wrong.
 */
object AppShapes {
    val logRow = RoundedCornerShape(14.dp)
    val levelTag = RoundedCornerShape(7.dp)
    val tabIndicator = RoundedCornerShape(2.dp)
    val pill = CircleShape
}
