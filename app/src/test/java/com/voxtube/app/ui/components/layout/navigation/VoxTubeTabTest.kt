package com.voxtube.app.ui.components.layout.navigation

import com.voxtube.app.data.local.DEFAULT_NAV_TAB_ORDER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoxTubeTabTest {
    @Test
    fun hiddenHomeFallsBackToFirstVisibleDestination() {
        val visibility = NavigationVisibility(home = false, shorts = false, music = false)

        val resolved =
            resolveDefaultFlowTab(
                preferredId = VoxTubeTab.Home.id,
                order = listOf(0, 4, 3, 1, 2, 5, 6),
                visibility = visibility,
            )

        assertEquals(VoxTubeTab.Library, resolved)
        assertFalse(visibleFlowTabs(listOf(0, 4, 3), visibility).contains(VoxTubeTab.Home))
    }

    @Test
    fun reEnabledHomeRestoresAHomeDefault() {
        val resolved =
            resolveDefaultFlowTab(
                preferredId = VoxTubeTab.Home.id,
                order = listOf(3, 0, 4),
                visibility = NavigationVisibility(home = true),
            )

        assertEquals(VoxTubeTab.Home, resolved)
    }

    @Test
    fun tabsFollowTheSavedOrder() {
        val tabs =
            visibleFlowTabs(
                order = listOf(4, 3, 2, 1, 0),
                visibility = NavigationVisibility(),
            )

        assertEquals(
            listOf(VoxTubeTab.Library, VoxTubeTab.Subscriptions, VoxTubeTab.Music, VoxTubeTab.Shorts, VoxTubeTab.Home),
            tabs,
        )
    }

    @Test
    fun aSavedOrderMissingNewerTabsAppendsThemInDefaultOrder() {
        val tabs =
            visibleFlowTabs(
                order = listOf(3, 0),
                visibility = NavigationVisibility(search = true, categories = true),
            )

        assertEquals(
            listOf(
                VoxTubeTab.Subscriptions,
                VoxTubeTab.Home,
                VoxTubeTab.Shorts,
                VoxTubeTab.Music,
                VoxTubeTab.Library,
                VoxTubeTab.Search,
                VoxTubeTab.Explore,
            ),
            tabs,
        )
    }

    @Test
    fun unknownPersistedIdsAreIgnored() {
        val tabs = visibleFlowTabs(order = listOf(42, 4, -1, 3), visibility = NavigationVisibility())

        assertEquals(VoxTubeTab.Library, tabs.first())
        assertEquals(tabs.distinct(), tabs)
    }

    @Test
    fun persistedIdsNeverChange() {
        assertEquals(DEFAULT_NAV_TAB_ORDER, VoxTubeTab.entries.map(VoxTubeTab::id))
        VoxTubeTab.entries.forEach { tab -> assertEquals(tab, VoxTubeTab.fromId(tab.id)) }
    }

    /**
     * Settings and Notifications live in the top bar of every root destination, which only works if
     * a root destination always exists. Subscriptions and Library are unconditional in
     * [visibleFlowTabs]; this pins that down so hiding tabs can never orphan those screens.
     */
    @Test
    fun everyVisibilityCombinationKeepsAnUnhideableRootDestination() {
        val orders =
            listOf(
                DEFAULT_NAV_TAB_ORDER,
                listOf(6, 5, 4, 3, 2, 1, 0),
                listOf(4, 3),
                emptyList(),
            )

        for (bits in 0 until 32) {
            val visibility =
                NavigationVisibility(
                    home = bits and 1 != 0,
                    shorts = bits and 2 != 0,
                    music = bits and 4 != 0,
                    search = bits and 8 != 0,
                    categories = bits and 16 != 0,
                )

            for (order in orders) {
                val visible = visibleFlowTabs(order, visibility)
                assertTrue(
                    "no unhideable root destination for $visibility / $order",
                    visible.contains(VoxTubeTab.Subscriptions) || visible.contains(VoxTubeTab.Library),
                )

                val resolved = resolveDefaultFlowTab(VoxTubeTab.Home.id, order, visibility)
                assertTrue(
                    "resolved default $resolved is not visible for $visibility / $order",
                    visible.contains(resolved),
                )
            }
        }
    }

    @Test
    fun theDownloadsTabIsOffUntilTurnedOn() {
        assertFalse(VoxTubeTab.Downloads in visibleFlowTabs(DEFAULT_NAV_TAB_ORDER, NavigationVisibility()))
        assertEquals(
            VoxTubeTab.Downloads,
            visibleFlowTabs(listOf(3, 0), NavigationVisibility(downloads = true)).last(),
        )
    }
}
