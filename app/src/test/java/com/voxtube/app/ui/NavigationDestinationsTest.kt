package com.voxtube.app.ui

import com.voxtube.app.ui.components.layout.navigation.NavigationVisibility
import com.voxtube.app.ui.components.layout.navigation.VoxTubeTab
import com.voxtube.app.ui.components.layout.navigation.visibleFlowTabs
import com.voxtube.app.ui.screens.update.UPDATE_ROUTE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationDestinationsTest {
    @Test
    fun tabRootsMapToTheirTab() {
        VoxTubeTab.entries.filter { it != VoxTubeTab.Shorts }.forEach { tab ->
            assertEquals(tab, flowTabForDestination(tab.route, shortsSourceArg = null))
        }
    }

    @Test
    fun onlyTheShortsFeedIsTheShortsTab() {
        assertEquals(VoxTubeTab.Shorts, flowTabForDestination(SHORTS_ROUTE_PATTERN, shortsSourceArg = null))
        assertEquals(VoxTubeTab.Shorts, flowTabForDestination(SHORTS_ROUTE_PATTERN, shortsSourceArg = "feed"))
        assertNull(flowTabForDestination(SHORTS_ROUTE_PATTERN, shortsSourceArg = "feed:dQw4w9WgXcQ"))
        assertNull(flowTabForDestination(SHORTS_ROUTE_PATTERN, shortsSourceArg = "saved:"))
    }

    @Test
    fun aVideoOpenedFromShortsPlaysOverTheFirstOtherTab() {
        assertEquals(VoxTubeTab.Music, exitTab(listOf(VoxTubeTab.Shorts, VoxTubeTab.Music, VoxTubeTab.Library), VoxTubeTab.Shorts))
        assertEquals(VoxTubeTab.Home, exitTab(listOf(VoxTubeTab.Home, VoxTubeTab.Shorts), VoxTubeTab.Shorts))
        assertEquals(VoxTubeTab.Home, exitTab(listOf(VoxTubeTab.Shorts), VoxTubeTab.Shorts))
        assertEquals(VoxTubeTab.Home, exitTab(emptyList(), VoxTubeTab.Shorts))
    }

    @Test
    fun backFromSearchAsTheOpeningTabLandsOnAVisibleTab() {
        val homeHidden =
            visibleFlowTabs(listOf(5, 0, 3, 4), NavigationVisibility(home = false, shorts = false, music = false, search = true))
        assertEquals(VoxTubeTab.Search, homeHidden.first())
        assertEquals(VoxTubeTab.Subscriptions, exitTab(homeHidden, VoxTubeTab.Search))
    }

    @Test
    fun detailScreensAreNotTabRoots() {
        listOf("settings", "settings/content", "playlists", "playlist/{playlistId}", "onboarding", null).forEach { route ->
            assertNull(flowTabForDestination(route, shortsSourceArg = null))
        }
    }

    @Test
    fun routesFromOutsideTheGraphResolveToTheirTab() {
        assertEquals(VoxTubeTab.Search, flowTabForRoute("search", downloadsTab = false))
        assertEquals(VoxTubeTab.Music, flowTabForRoute("music", downloadsTab = false))
        listOf("downloads", "history", "musicRecognize", "musicPlayer/abc", "settings").forEach { route ->
            assertNull(flowTabForRoute(route, downloadsTab = false))
        }
    }

    @Test
    fun theDownloadsPageOpensAsTheTabOnlyWhenTheBarHasIt() {
        assertEquals(VoxTubeTab.Downloads, flowTabForRoute("downloads", downloadsTab = true))
        assertNull(flowTabForRoute("downloads", downloadsTab = false))
        assertNull(flowTabForDestination("downloads", shortsSourceArg = null))
    }

    @Test
    fun searchIsTheOnlyTabWithoutTheBar() {
        VoxTubeTab.entries.forEach { tab -> assertEquals(tab != VoxTubeTab.Search, tab.showsNavigationBar()) }
        assertFalse((null as VoxTubeTab?).showsNavigationBar())
        assertTrue(VoxTubeTab.Home.showsNavigationBar())
    }

    @Test
    fun theSwitchPutsTheBarOnSearchOnly() {
        VoxTubeTab.entries.forEach { tab -> assertTrue(tab.showsNavigationBar(onSearch = true)) }
        assertFalse((null as VoxTubeTab?).showsNavigationBar(onSearch = true))
    }

    @Test
    fun onlyChannelIdsAreBrowsedDirectly() {
        assertEquals("UCXuqSBlHAE6Xw-yeJA0Tunw", youtubeChannelBrowseId("UCXuqSBlHAE6Xw-yeJA0Tunw"))
        assertEquals("UCXuqSBlHAE6Xw-yeJA0Tunw", youtubeChannelBrowseId("https://m.youtube.com/channel/UCXuqSBlHAE6Xw-yeJA0Tunw?si=abc"))
        assertNull(youtubeChannelBrowseId("@LinusTechTips"))
        assertNull(youtubeChannelBrowseId("https://www.youtube.com/@LinusTechTips"))
        assertNull(youtubeChannelBrowseId("https://www.youtube.com/c/LinusTechTips"))
        assertNull(youtubeChannelBrowseId(" "))
    }

    @Test
    fun channelHandlesUseHandleUrls() {
        assertEquals("https://www.youtube.com/@flow", youtubeChannelUrl("@flow"))
        assertEquals("https://www.youtube.com/@flow", youtubeChannelUrl("flow"))
        assertEquals(
            "https://www.youtube.com/channel/UC123",
            youtubeChannelUrl("UC123"),
        )
    }

    @Test
    fun malformedHandleChannelUrlsAreRepaired() {
        assertEquals(
            "https://www.youtube.com/@flow",
            youtubeChannelUrl("https://youtube.com/channel/@flow"),
        )
        assertEquals(
            "https://www.youtube.com/@flow",
            youtubeChannelUrl("https://m.youtube.com/@flow/videos"),
        )
    }

    @Test
    fun channelRoutesEncodeCanonicalUrls() {
        assertEquals(
            "channel?url=https%3A%2F%2Fwww.youtube.com%2F%40flow",
            youtubeChannelRoute("@flow"),
        )
    }

    @Test
    fun aTabTappedOnSearchOpenedAsAPageLeavesSearchFirst() {
        VoxTubeTab.entries.filter { it != VoxTubeTab.Search }.forEach { tapped ->
            assertEquals(
                TabTap.LEAVE_SEARCH,
                tabTapAction(tapped, VoxTubeTab.Search, selectedTab = VoxTubeTab.Home, searchIsTab = false, startTab = VoxTubeTab.Home),
            )
        }
    }

    @Test
    fun withASearchTabInTheBarOtherTabsSwitchAndKeepItsResults() {
        assertEquals(
            TabTap.SWITCH,
            tabTapAction(
                VoxTubeTab.Music,
                VoxTubeTab.Search,
                selectedTab = VoxTubeTab.Search,
                searchIsTab = true,
                startTab = VoxTubeTab.Home,
            ),
        )
        assertEquals(
            TabTap.SCROLL_TO_TOP,
            tabTapAction(
                VoxTubeTab.Search,
                VoxTubeTab.Search,
                selectedTab = VoxTubeTab.Search,
                searchIsTab = true,
                startTab = VoxTubeTab.Home,
            ),
        )
    }

    @Test
    fun theStartTabLeavesTheSearchTabLikeBack() {
        assertEquals(
            TabTap.LEAVE_SEARCH,
            tabTapAction(
                VoxTubeTab.Home,
                VoxTubeTab.Search,
                selectedTab = VoxTubeTab.Search,
                searchIsTab = true,
                startTab = VoxTubeTab.Home,
            ),
        )
        assertEquals(
            TabTap.LEAVE_SEARCH,
            tabTapAction(
                VoxTubeTab.Music,
                VoxTubeTab.Search,
                selectedTab = VoxTubeTab.Search,
                searchIsTab = true,
                startTab = VoxTubeTab.Music,
            ),
        )
        assertEquals(
            TabTap.SCROLL_TO_TOP,
            tabTapAction(
                VoxTubeTab.Search,
                VoxTubeTab.Search,
                selectedTab = VoxTubeTab.Search,
                searchIsTab = true,
                startTab = VoxTubeTab.Search,
            ),
        )
    }

    @Test
    fun theTabAlreadyOnItsFirstScreenScrollsToTheTop() {
        assertEquals(
            TabTap.SCROLL_TO_TOP,
            tabTapAction(VoxTubeTab.Home, VoxTubeTab.Home, selectedTab = VoxTubeTab.Home, searchIsTab = false, startTab = VoxTubeTab.Home),
        )
        assertEquals(
            TabTap.SCROLL_TO_TOP,
            tabTapAction(
                VoxTubeTab.Music,
                VoxTubeTab.Music,
                selectedTab = VoxTubeTab.Music,
                searchIsTab = true,
                startTab = VoxTubeTab.Home,
            ),
        )
    }

    @Test
    fun theHighlightedTabOnAPageInsideItGoesBackToItsFirstScreen() {
        assertEquals(
            TabTap.POP_TO_ROOT,
            tabTapAction(
                VoxTubeTab.Home,
                currentTab = null,
                selectedTab = VoxTubeTab.Home,
                searchIsTab = false,
                startTab = VoxTubeTab.Home,
            ),
        )
        assertEquals(
            TabTap.SWITCH,
            tabTapAction(
                VoxTubeTab.Music,
                currentTab = null,
                selectedTab = VoxTubeTab.Home,
                searchIsTab = false,
                startTab = VoxTubeTab.Home,
            ),
        )
    }

    @Test
    fun anotherTabSwitches() {
        assertEquals(
            TabTap.SWITCH,
            tabTapAction(VoxTubeTab.Music, VoxTubeTab.Home, selectedTab = VoxTubeTab.Home, searchIsTab = false, startTab = VoxTubeTab.Home),
        )
    }

    @Test
    fun innerPagesKeepTheBarOnlyWhenAskedFor() {
        assertTrue(navigationBarShown(null, "channel?url={channelUrl}", onSearch = false, onInnerPages = true))
        assertTrue(navigationBarShown(null, MUSIC_ARTIST_ROUTE_PATTERN, onSearch = false, onInnerPages = true))
        assertFalse(navigationBarShown(null, "channel?url={channelUrl}", onSearch = false, onInnerPages = false))
    }

    @Test
    fun settingsPlayersAndFullScreenFlowsNeverGetTheBar() {
        listOf("settings?target={target}", EQUALIZER_ROUTE, UPDATE_ROUTE, MUSIC_PLAYER_ROUTE_PATTERN, "player/{videoId}", "onboarding")
            .forEach { route -> assertFalse(route, navigationBarShown(null, route, onSearch = true, onInnerPages = true)) }
    }

    @Test
    fun tabRootsAndSearchKeepTheirOwnRules() {
        assertTrue(navigationBarShown(VoxTubeTab.Home, "home", onSearch = false, onInnerPages = false))
        assertFalse(navigationBarShown(VoxTubeTab.Search, "search", onSearch = false, onInnerPages = true))
        assertTrue(navigationBarShown(VoxTubeTab.Search, "search", onSearch = true, onInnerPages = false))
    }

    @Test
    fun everySearchPageIsTheSearchTab() {
        assertEquals(
            VoxTubeTab.Search,
            flowTabForDestination(com.voxtube.app.ui.screens.search.SEARCH_ROUTE_PATTERN, shortsSourceArg = null),
        )
    }
}
