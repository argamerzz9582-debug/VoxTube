package com.voxtube.app.ui.components.shared

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voxtube.app.ui.theme.CapsuleShape
import com.voxtube.app.ui.theme.glass.contentGlass

/** One half of a [VoxTubeActionButtonPair]. */
class VoxTubePairAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

/**
 * A page's two main actions as a pair of capsules sharing the width, after iOS 26: [primary] filled
 * in the accent, [secondary] a pane of glass. Play all and Shuffle on a playlist; Resume and Edit on a
 * note. A [tertiary] action joins the row as an icon-only glass capsule at the end.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VoxTubeActionButtonPair(
    primary: VoxTubePairAction,
    secondary: VoxTubePairAction,
    modifier: Modifier = Modifier,
    tertiary: VoxTubePairAction? = null,
) {
    val height = ButtonDefaults.MediumContainerHeight
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PillActionSpacing),
    ) {
        Button(
            onClick = primary.onClick,
            shapes = PillButtonShapes,
            contentPadding = ButtonDefaults.contentPaddingFor(height),
            modifier = Modifier.weight(1f).heightIn(min = height),
        ) {
            PairActionLabel(primary.icon, primary.label, height)
        }
        GlassPillButton(
            onClick = secondary.onClick,
            height = height,
            modifier = Modifier.weight(1f),
        ) {
            PairActionLabel(secondary.icon, secondary.label, height)
        }
        if (tertiary != null) {
            GlassPillButton(onClick = tertiary.onClick, height = height) {
                Icon(
                    imageVector = tertiary.icon,
                    contentDescription = tertiary.label,
                    modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
                )
            }
        }
    }
}

/** Space between a page's pill actions. */
val PillActionSpacing = 10.dp

/** Capsule at rest and pressed, so a pill keeps its outline and only sinks under the finger. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val PillButtonShapes = ButtonShapes(CapsuleShape, CapsuleShape)

/**
 * A secondary action as a capsule of glass: the page shows through it and its label keeps the page's
 * text colour. A press sinks it a little, like the rest of the app's glass.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlassPillButton(
    onClick: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shapes = PillButtonShapes,
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        contentPadding = ButtonDefaults.contentPaddingFor(height),
        interactionSource = interactionSource,
        modifier =
            modifier
                .heightIn(min = height)
                .pressScale(interactionSource)
                .then(contentGlass(CapsuleShape)),
        content = content,
    )
}

@Composable
private fun PairActionLabel(
    icon: ImageVector,
    label: String,
    height: Dp,
) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)))
    Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
    Text(text = label, style = ButtonDefaults.textStyleFor(height), maxLines = 1)
}
