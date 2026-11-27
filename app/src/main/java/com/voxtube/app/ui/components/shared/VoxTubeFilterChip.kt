/*
 * Copyright (C) 2025-2026 Flow | A-EDev
 *
 * This file is part of Flow (https://github.com/Flow-Tube/Flow).
 */

package com.voxtube.app.ui.components.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voxtube.app.ui.theme.CapsuleShape
import com.voxtube.app.ui.theme.glass.contentGlass

/**
 * The single filter chip for every surface: home feed chips, search filters, browse tabs,
 * library content filters. Pills of Liquid Glass; the picked one is tinted with the accent and ringed
 * in it, with no check, so a chip keeps its width when it is picked. A press sinks the chip a little.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VoxTubeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        shapes = FilterChipDefaults.shapes(shape = CapsuleShape, selectedShape = CapsuleShape),
        colors = glassChipColors(),
        border = glassChipBorder(selected),
        interactionSource = interactionSource,
        modifier = modifier.glassChip(selected, interactionSource),
    )
}

/**
 * A filter chip that opens a menu instead of toggling. [selected] marks that a non-default option is
 * active; the chevron tells it apart from a chip that toggles.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VoxTubeDropdownFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        shapes = FilterChipDefaults.shapes(shape = CapsuleShape, selectedShape = CapsuleShape),
        colors = glassChipColors(),
        border = glassChipBorder(selected),
        interactionSource = interactionSource,
        modifier = modifier.glassChip(selected, interactionSource),
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    )
}

private const val SELECTED_FILL_ALPHA = 0.24f
private const val SELECTED_RIM_ALPHA = 0.65f
private const val PRESSED_CHIP_SCALE = 0.95f
private val SelectedRimWidth = 1.dp

@Composable
private fun glassChipColors(): SelectableChipColors {
    val scheme = MaterialTheme.colorScheme
    return FilterChipDefaults.filterChipColors(
        containerColor = Color.Transparent,
        labelColor = scheme.onSurface,
        iconColor = scheme.onSurfaceVariant,
        selectedContainerColor = scheme.primary.copy(alpha = SELECTED_FILL_ALPHA),
        selectedLabelColor = scheme.onSurface,
        selectedLeadingIconColor = scheme.onSurface,
        selectedTrailingIconColor = scheme.onSurface,
    )
}

/** The resting chip's rim is the glass's own; the picked chip is ringed in the accent. */
@Composable
private fun glassChipBorder(selected: Boolean): BorderStroke =
    FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = selected,
        borderColor = Color.Transparent,
        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_RIM_ALPHA),
        selectedBorderWidth = SelectedRimWidth,
    )

@Composable
private fun Modifier.glassChip(
    selected: Boolean,
    interactionSource: MutableInteractionSource,
): Modifier =
    pressScale(interactionSource, PRESSED_CHIP_SCALE)
        .then(if (selected) Modifier else contentGlass(CapsuleShape))
