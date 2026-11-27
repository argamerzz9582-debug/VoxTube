package com.voxtube.app.ui.tv.navigation

import com.google.common.truth.Truth.assertThat
import com.voxtube.app.ui.components.layout.navigation.NavigationVisibility
import com.voxtube.app.ui.components.layout.navigation.VoxTubeTab
import org.junit.Test

class TvTabsTest {
    private val allOn = NavigationVisibility(home = true, shorts = true, music = true, search = true, categories = true)

    @Test
    fun `follows the user's order and leaves Shorts and Explore off`() {
        val order = listOf(VoxTubeTab.Library, VoxTubeTab.Shorts, VoxTubeTab.Music, VoxTubeTab.Explore, VoxTubeTab.Home).map(VoxTubeTab::id)

        val tabs = tvNavigation(order, defaultTabId = VoxTubeTab.Home.id, visibility = allOn).tabs

        assertThat(tabs.take(3)).containsExactly(VoxTubeTab.Library, VoxTubeTab.Music, VoxTubeTab.Home).inOrder()
        assertThat(tabs).containsNoneOf(VoxTubeTab.Shorts, VoxTubeTab.Explore)
    }

    @Test
    fun `always shows Search, which TV has nowhere else to put`() {
        val tabs = tvNavigation(emptyList(), VoxTubeTab.Home.id, NavigationVisibility(search = false)).tabs

        assertThat(tabs).contains(VoxTubeTab.Search)
    }

    @Test
    fun `hides the tabs the user hid`() {
        val tabs = tvNavigation(emptyList(), VoxTubeTab.Home.id, NavigationVisibility(home = false, music = false)).tabs

        assertThat(tabs).containsNoneOf(VoxTubeTab.Home, VoxTubeTab.Music)
    }

    @Test
    fun `opens on the user's default tab, or the first tab when TV leaves it off`() {
        assertThat(tvNavigation(emptyList(), VoxTubeTab.Library.id, allOn).startTab).isEqualTo(VoxTubeTab.Library)

        val fromShorts = tvNavigation(emptyList(), VoxTubeTab.Shorts.id, allOn)
        assertThat(fromShorts.startTab).isEqualTo(fromShorts.tabs.first())
    }

    @Test
    fun `roots are the tabs plus Settings`() {
        val navigation = tvNavigation(emptyList(), VoxTubeTab.Home.id, allOn)

        assertThat(navigation.rootRoutes).containsExactlyElementsIn(navigation.tabs.map(VoxTubeTab::route) + TV_SETTINGS_ROUTE)
    }
}
