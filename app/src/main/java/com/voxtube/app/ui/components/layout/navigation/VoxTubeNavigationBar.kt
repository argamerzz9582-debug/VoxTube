package com.voxtube.app.ui.components.layout.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.voxtube.app.R
import com.voxtube.app.data.local.NavBarLabels
import com.voxtube.app.ui.utils.LocalWindowSizeClass
import com.voxtube.app.ui.utils.isMediumWidth

internal const val MAX_BAR_TABS = 5

/**
 * The bottom navigation bar. Material allows at most five destinations in a bar, so a sixth and
 * seventh enabled tab move behind a "More" item.
 */
@Composable
internal fun VoxTubeNavigationBar(
    tabs: List<VoxTubeTab>,
    selectedTab: VoxTubeTab?,
    onTabSelected: (VoxTubeTab) -> Unit,
    modifier: Modifier = Modifier,
    labels: NavBarLabels = NavBarLabels.ALWAYS,
) {
    val horizontalItems = LocalWindowSizeClass.current.isMediumWidth
    val iconPosition = if (horizontalItems) NavigationItemIconPosition.Start else NavigationItemIconPosition.Top
    val (barTabs, overflowTabs) = splitTabsForOverflow(tabs, MAX_BAR_TABS)

    ShortNavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        arrangement =
            if (horizontalItems) ShortNavigationBarArrangement.Centered else ShortNavigationBarArrangement.EqualWeight,
    ) {
        barTabs.forEach { tab ->
            val selected = tab == selectedTab
            VoxTubeTabBarItem(
                tab = tab,
                selected = selected,
                onClick = { onTabSelected(tab) },
                showsLabel = labels.shows(selected),
                iconPosition = iconPosition,
            )
        }
        if (overflowTabs.isNotEmpty()) {
            VoxTubeOverflowBarItem(
                tabs = overflowTabs,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                showsLabel = labels.shows(selectedTab in overflowTabs),
                iconPosition = iconPosition,
            )
        }
    }
}

/** One tab in a bottom bar; without its label the icon carries the tab's name for TalkBack. */
@Composable
internal fun VoxTubeTabBarItem(
    tab: VoxTubeTab,
    selected: Boolean,
    onClick: () -> Unit,
    showsLabel: Boolean,
    iconPosition: NavigationItemIconPosition,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(tab.labelRes)
    ShortNavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(imageVector = tab.icon(selected), contentDescription = if (showsLabel) null else name) },
        label = if (showsLabel) ({ Text(text = name) }) else null,
        iconPosition = iconPosition,
        modifier = modifier,
    )
}

/** The "More" slot of a bottom bar, opening the tabs it had no room for. */
@Composable
internal fun VoxTubeOverflowBarItem(
    tabs: List<VoxTubeTab>,
    selectedTab: VoxTubeTab?,
    onTabSelected: (VoxTubeTab) -> Unit,
    showsLabel: Boolean,
    iconPosition: NavigationItemIconPosition,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val name = stringResource(R.string.nav_more)
    // One Box per bar slot: the bar measures every direct child as an item, and the menu's
    // popup anchor would otherwise count as one.
    Box(modifier = modifier, propagateMinConstraints = true) {
        ShortNavigationBarItem(
            selected = selectedTab in tabs,
            onClick = { expanded = true },
            icon = { Icon(imageVector = Icons.Filled.MoreHoriz, contentDescription = if (showsLabel) null else name) },
            label = if (showsLabel) ({ Text(text = name) }) else null,
            iconPosition = iconPosition,
        )
        VoxTubeTabOverflowMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            tabs = tabs,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
        )
    }
}

internal fun NavBarLabels.shows(selected: Boolean): Boolean =
    when (this) {
        NavBarLabels.ALWAYS -> true
        NavBarLabels.SELECTED -> selected
        NavBarLabels.NEVER -> false
    }
