package com.voxtube.app.ui.components.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.voxtube.app.ui.theme.glass.LocalGlassAccessibility

private val GlassMenuShape = RoundedCornerShape(22.dp)
private val GlassMenuRim = 0.75.dp
private val GlassMenuShadow = 16.dp
private const val GLASS_MENU_FILL_ALPHA = 0.96f

/**
 * A dropdown menu in the iOS 26 style: a deeply rounded sheet with a specular rim that is bright along
 * the top and fades toward the bottom, floating on a soft shadow.
 *
 * A menu opens in a popup window of its own, which the backdrop library cannot see through, so the
 * sheet is nearly opaque glass rather than refracting; text on it always keeps full contrast. With the
 * system contrast raised the rim becomes a solid outline.
 */
@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < DARK_SURFACE_LUMINANCE
    val rim =
        if (LocalGlassAccessibility.current.increaseContrast) {
            BorderStroke(1.dp, scheme.outline)
        } else {
            val top = if (dark) Color.White.copy(alpha = 0.3f) else Color.White
            val bottom = if (dark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.08f)
            BorderStroke(GlassMenuRim, Brush.verticalGradient(listOf(top, bottom)))
        }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = GlassMenuShape,
        containerColor = scheme.surfaceContainerHigh.copy(alpha = GLASS_MENU_FILL_ALPHA),
        tonalElevation = 0.dp,
        shadowElevation = GlassMenuShadow,
        border = rim,
        content = content,
    )
}

private val GlassMenuRowShape = RoundedCornerShape(14.dp)
private val GlassMenuRowInset = 6.dp
private const val SELECTED_ROW_DARK_ALPHA = 0.18f
private const val SELECTED_ROW_LIGHT_ALPHA = 0.07f

/**
 * A row of a [GlassDropdownMenu]. The [selected] row sits on a soft rounded pill inset from the sheet's
 * edge, the way iOS 26 marks the current choice in a menu, and says it is selected to a screen reader.
 */
@Composable
fun GlassMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE
    val pill =
        when {
            !selected -> Color.Transparent
            dark -> Color.White.copy(alpha = SELECTED_ROW_DARK_ALPHA)
            else -> Color.Black.copy(alpha = SELECTED_ROW_LIGHT_ALPHA)
        }
    DropdownMenuItem(
        text = text,
        onClick = onClick,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        modifier =
            modifier
                .padding(horizontal = GlassMenuRowInset)
                .clip(GlassMenuRowShape)
                .background(pill, GlassMenuRowShape)
                .semantics { this.selected = selected },
    )
}

private const val DARK_SURFACE_LUMINANCE = 0.5f
