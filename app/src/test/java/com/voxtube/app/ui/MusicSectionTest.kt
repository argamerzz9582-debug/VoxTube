package com.voxtube.app.ui

import com.voxtube.app.ui.components.layout.navigation.VoxTubeTab
import com.voxtube.app.ui.screens.search.SEARCH_ROUTE_PATTERN
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSectionTest {
    @Test
    fun musicPagesAreInTheSectionByPatternOrByRoute() {
        listOf(
            "musicSearch?query={query}",
            "musicSearch",
            MUSIC_ARTIST_ROUTE_PATTERN,
            "artist/UC123",
            "artistItems/{channelId}/{browseId}?params={params}",
            MUSIC_PLAYLIST_ROUTE_PATTERN,
            "youtube_browse/{browseId}?params={params}",
            MUSIC_PLAYER_ROUTE_PATTERN,
            EQUALIZER_ROUTE,
            "moodsAndGenres",
        ).forEach { assertTrue(it, isMusicSectionRoute(it)) }
    }

    @Test
    fun otherPagesAreNot() {
        listOf(null, "home", "search", SEARCH_ROUTE_PATTERN, "settings", "channel?url=x", "player/abc", "artistic").forEach {
            assertFalse(it.toString(), isMusicSectionRoute(it))
        }
    }

    @Test
    fun theMusicTabAndPagesOpenedFromItAreTheSection() {
        assertTrue(inMusicSection(VoxTubeTab.Music, VoxTubeTab.Music, "music"))
        assertTrue(inMusicSection(null, VoxTubeTab.Music, "channel?url={url}"))
        assertTrue(inMusicSection(null, VoxTubeTab.Home, MUSIC_ARTIST_ROUTE_PATTERN))
    }

    @Test
    fun anotherTabIsNotEvenWhenMusicWasTheLastOne() {
        assertFalse(inMusicSection(VoxTubeTab.Home, VoxTubeTab.Home, "home"))
        assertFalse(inMusicSection(null, VoxTubeTab.Home, "channel?url={url}"))
        // Search as a page, not a tab, leaves the bar's pick on Music.
        assertFalse(inMusicSection(VoxTubeTab.Search, VoxTubeTab.Music, SEARCH_ROUTE_PATTERN))
    }
}
