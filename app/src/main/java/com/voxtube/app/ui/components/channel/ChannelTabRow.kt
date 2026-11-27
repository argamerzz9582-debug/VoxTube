package com.voxtube.app.ui.components.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voxtube.app.ui.components.shared.LocalDpadNavigation
import com.voxtube.app.ui.theme.extendedColors
import com.voxtube.app.ui.theme.glass.glassHairline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChannelTabRow(
    selectedIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
) {
    val selectedTabFocus = remember { FocusRequester() }
    val dpad = LocalDpadNavigation.current
    // A scrollable tab row sizes its divider to the tabs, which on a tablet ends mid-screen.
    Column(modifier = Modifier.fillMaxWidth()) {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            // A remote comes into the row on the open tab, not on whichever tab is nearest.
            modifier =
                if (dpad) {
                    Modifier.focusProperties { onEnter = { selectedTabFocus.requestFocus() } }.focusGroup()
                } else {
                    Modifier
                },
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
            // The open tab sits in a soft pill of glass that glides between tabs, as in iOS 26's segmented strips.
            indicator = {
                Box(
                    modifier =
                        Modifier
                            .tabIndicatorOffset(selectedIndex, matchContentSize = false)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp, vertical = 5.dp)
                            .then(selectedTabPill()),
                )
            },
            divider = {},
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onTabSelected(index) },
                    modifier =
                        Modifier
                            .height(44.dp)
                            .then(if (selectedIndex == index) Modifier.focusRequester(selectedTabFocus) else Modifier),
                    selectedContentColor = MaterialTheme.colorScheme.onSurface,
                    unselectedContentColor = MaterialTheme.extendedColors.textSecondary,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 0.5.dp,
        )
    }
}

private val TabPillShape = RoundedCornerShape(percent = 50)
private const val TAB_PILL_DARK_ALPHA = 0.12f
private const val TAB_PILL_LIGHT_ALPHA = 0.06f
private const val DARK_SURFACE_LUMINANCE = 0.5f

@Composable
private fun selectedTabPill(): Modifier {
    val scheme = MaterialTheme.colorScheme
    val fill =
        if (scheme.surface.luminance() < DARK_SURFACE_LUMINANCE) {
            Color.White.copy(alpha = TAB_PILL_DARK_ALPHA)
        } else {
            Color.Black.copy(alpha = TAB_PILL_LIGHT_ALPHA)
        }
    return Modifier.background(fill, TabPillShape).then(glassHairline(TabPillShape))
}
