package io.github.gythialy.sporteditor.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * The expressive shape scale (4 / 8 / 12 / 16 / 20 / 28 / 32 / 48).
 *
 * [largeIncreased] (20) is the card radius: a card is information-dense, so it takes one step past
 * the standard [large] instead of the 28 dp "large face" the expressive spec reserves for sheets
 * and dialogs. [extraLarge] (28) is therefore a real bottom-sheet/dialog corner again — it used to
 * be flattened to 16 so nothing else in the app contradicted it, which also flattened every modal.
 */
val SportEditorShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    largeIncreased = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(48.dp)
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

/**
 * Container shape for cards.
 *
 * A semantic alias instead of `shapes.large`: the call sites mean "the card radius", and when the
 * scale moves again only this line changes.
 */
@Composable
fun cardShape(): CornerBasedShape = MaterialTheme.shapes.largeIncreased
