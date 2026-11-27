package com.voxtube.app.ui

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.voxtube.app.data.local.PlaylistRepository
import com.voxtube.app.data.localmedia.LocalMediaIds
import com.voxtube.app.data.localmedia.toMusicTrack
import com.voxtube.app.data.localmedia.toVideo
import com.voxtube.app.data.model.Video
import com.voxtube.app.data.music.model.toMusicTrack
import com.voxtube.app.data.music.model.toVideo
import com.voxtube.app.data.shorts.queue.ShortsQueueSource
import com.voxtube.app.ui.components.layout.LocalFlowBottomInsets
import com.voxtube.app.ui.components.layout.navigation.MediaNavigator
import com.voxtube.app.ui.components.layout.navigation.VoxTubeTab
import com.voxtube.app.ui.components.musicplayer.sheet.MusicPlayerSheetState
import com.voxtube.app.ui.components.settings.LocalNewSettings
import com.voxtube.app.ui.components.settings.SettingsDestination
import com.voxtube.app.ui.components.settings.SettingsTarget
import com.voxtube.app.ui.components.videoplayer.PlayerDraggableState
import com.voxtube.app.ui.screens.channel.ChannelScreen
import com.voxtube.app.ui.screens.equalizer.EqualizerScreen
import com.voxtube.app.ui.screens.history.HistoryScreen
import com.voxtube.app.ui.screens.home.HomeScreen
import com.voxtube.app.ui.screens.home.HomeViewModel
import com.voxtube.app.ui.screens.library.LibraryScreen
import com.voxtube.app.ui.screens.music.sharedMusicPlayerViewModel
import com.voxtube.app.ui.screens.notifications.NotificationScreen
import com.voxtube.app.ui.screens.onboarding.OnboardingScreen
import com.voxtube.app.ui.screens.player.VideoPlayerViewModel
import com.voxtube.app.ui.screens.player.state.VideoPlayerUiState
import com.voxtube.app.ui.screens.playlists.PlaylistDetailScreen
import com.voxtube.app.ui.screens.playlists.PlaylistsScreen
import com.voxtube.app.ui.screens.recap.RecapRoutes
import com.voxtube.app.ui.screens.recap.RecapScreen
import com.voxtube.app.ui.screens.recap.story.RecapStoryScreen
import com.voxtube.app.ui.screens.search.MAX_SEARCH_DEPTH
import com.voxtube.app.ui.screens.search.SEARCH_DEPTH_ARG
import com.voxtube.app.ui.screens.search.SEARCH_FILTERS_ARG
import com.voxtube.app.ui.screens.search.SEARCH_QUERY_ARG
import com.voxtube.app.ui.screens.search.SEARCH_ROUTE_PATTERN
import com.voxtube.app.ui.screens.search.SearchScreen
import com.voxtube.app.ui.screens.search.searchRoute
import com.voxtube.app.ui.screens.settings.SettingsHost
import com.voxtube.app.ui.screens.settings.rememberNewSettings
import com.voxtube.app.ui.screens.settings.wellbeing.TIMES_UP_ROUTE
import com.voxtube.app.ui.screens.settings.wellbeing.TimesUpScreen
import com.voxtube.app.ui.screens.shorts.ShortsScreen
import com.voxtube.app.ui.screens.subscriptions.SubscriptionsScreen
import com.voxtube.app.ui.screens.update.UPDATE_ROUTE
import com.voxtube.app.ui.screens.update.UpdateScreen
import kotlinx.coroutines.flow.first

@UnstableApi
fun NavGraphBuilder.flowAppGraph(
    navController: NavHostController,
    mediaNavigator: MediaNavigator,
    currentRoute: MutableState<String>,
    playerSheetState: PlayerDraggableState,
    musicPlayerSheetState: MusicPlayerSheetState,
    homeViewModel: HomeViewModel,
    playerViewModel: VideoPlayerViewModel,
    playerUiStateResult: State<VideoPlayerUiState>,
    playerVisibleState: MutableState<Boolean>,
    disableShortsPlayer: Boolean = false,
    defaultStartRoute: String = "home",
    onMusicStarted: () -> Unit = {},
    openDownloads: () -> Unit = {},
    searchExitTab: () -> VoxTubeTab = { VoxTubeTab.Home },
    searchIsTab: () -> Boolean = { false },
) {
    // =============================================
    // ONBOARDING (First-time user experience)
    // =============================================
    page("onboarding") {
        OnboardingScreen(
            onComplete = {
                // Navigate to the selected default tab and clear the backstack so user can't go back to onboarding
                navController.navigate(defaultStartRoute) {
                    popUpTo("onboarding") { inclusive = true }
                }
            },
        )
    }

    page("home") {
        HomeScreen(
            onVideoClick = { video ->
                navController.openVideoOrShorts(video, disableShortsPlayer) {
                    playerViewModel.playVideo(it)
                }
            },
            onOpenPreview = { video, positionMs -> playerViewModel.playVideo(video, startPositionMs = positionMs) },
            onShortClick = { source ->
                navController.openShortsOrPlayer(source, disableShortsPlayer)
            },
            onSearchClick = {
                // With a Search tab in the bar, the icon opens that tab, so Search keeps its state like any other tab.
                if (searchIsTab()) navController.navigateToTab(VoxTubeTab.Search, defaultStartRoute) else navController.navigate("search")
            },
            onNavigateToHistory = {
                navController.navigate("history")
            },
            onOpenShortsFeed = {
                navController.openShorts(ShortsQueueSource.Feed)
            },
            onPlayMix = { videos, title -> playerViewModel.playPlaylist(videos, 0, title, false) },
            onOpenDownloads = openDownloads,
            viewModel = homeViewModel,
        )
    }

    page(UPDATE_ROUTE) {
        UpdateScreen(onClose = { navController.popBackStack() })
    }

    page(TIMES_UP_ROUTE) {
        TimesUpScreen(onClose = { navController.popBackStack() })
    }

    // Notifications Screen
    page("notifications") {
        NotificationScreen(
            onBackClick = { navController.popBackStack() },
            onNotificationClick = { videoId ->
                navController.navigateToPlayer(videoId)
            },
            onOpenSettings = {
                navController.navigate("settings?target=${SettingsTarget(SettingsDestination.NOTIFICATIONS).encode()}")
            },
        )
    }

    page(
        route = SHORTS_ROUTE_PATTERN,
        arguments =
            listOf(
                navArgument(SHORTS_ROUTE_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val source = ShortsQueueSource.decode(backStackEntry.arguments?.getString(SHORTS_ROUTE_ARG))
        ShortsScreen(
            source = source,
            // The bar, folded to one button, also rests over Shorts opened from a feed; zero while it is hidden.
            bottomNavOverlayPadding = LocalFlowBottomInsets.current.barBottom,
            onBack = {
                navController.popBackStack()
            },
            onChannelClick = { channelId ->
                mediaNavigator.openChannel(channelId)
            },
        )
    }

    page("subscriptions") { backStackEntry ->
        val openMusic by backStackEntry.savedStateHandle
            .getStateFlow(OPEN_MUSIC_SUBSCRIPTIONS, false)
            .collectAsStateWithLifecycle()
        SubscriptionsScreen(
            openMusicSubscriptions = openMusic,
            onMusicSubscriptionsOpened = { backStackEntry.savedStateHandle[OPEN_MUSIC_SUBSCRIPTIONS] = false },
            onVideoClick = { video ->
                navController.openVideoOrShorts(video, disableShortsPlayer) {
                    playerViewModel.playVideo(it)
                }
            },
            onShortClick = { source ->
                navController.openShortsOrPlayer(source, disableShortsPlayer)
            },
            onChannelClick = { channel ->
                if (channel.isMusic && channel.id.isNotBlank()) {
                    mediaNavigator.openArtist(channel.id)
                } else {
                    mediaNavigator.openChannel(channel.url.ifBlank { channel.id })
                }
            },
        )
    }

    page("library") {
        val musicPlayerViewModel = sharedMusicPlayerViewModel()
        val downloadsSourceName =
            androidx.compose.ui.res.stringResource(
                com.voxtube.app.R.string.library_downloads_label,
            )
        LibraryScreen(
            onOpenRecap = { period -> navController.navigate(period?.let { RecapRoutes.story(it) } ?: RecapRoutes.stats()) },
            onNavigateToHistory = {
                navController.navigate("history")
            },
            onNavigateToPlaylists = { kind ->
                navController.navigate(if (kind == null) "playlists" else "playlists?kind=${kind.name}")
            },
            onNavigateToLikedVideos = {
                navController.navigate("playlist/${PlaylistRepository.LIKED_VIDEOS_ID}")
            },
            onNavigateToLikedMusic = {
                mediaNavigator.openMusicPlaylist(PlaylistRepository.LIKED_MUSIC_ID)
            },
            onNavigateToWatchLater = {
                navController.navigate("playlist/${PlaylistRepository.WATCH_LATER_ID}")
            },
            onNavigateToSavedShorts = {
                navController.navigate("savedShorts")
            },
            onNavigateToDownloads = openDownloads,
            onNavigateToLocalMedia = {
                navController.navigate("localMedia")
            },
            onNavigateToNotes = {
                navController.navigate("notes")
            },
            onManageData = {
                navController.navigate("settings")
            },
            onVideoClick = { video ->
                navController.openVideoOrShorts(video, disableShortsPlayer) { playerViewModel.playVideo(it) }
            },
            onMusicClick = { track, queue, sourceName ->
                musicPlayerViewModel.loadAndPlayTrack(track, queue, sourceName)
                onMusicStarted()
            },
            onPlaylistClick = { playlistId ->
                navController.navigate("playlist/$playlistId")
            },
            onMusicPlaylistClick = { playlistId ->
                mediaNavigator.openMusicPlaylist(playlistId)
            },
            onDownloadedVideoClick = { videos, index ->
                val videoList = videos.map { it.video }
                playerViewModel.playPlaylist(videoList, index, downloadsSourceName)
            },
            onDownloadedMusicClick = { tracks, index ->
                val musicTracks = tracks.map { it.track }
                val selectedTrack = musicTracks[index]
                musicPlayerViewModel.loadAndPlayTrack(selectedTrack, musicTracks, downloadsSourceName)
                onMusicStarted()
            },
            onSavedShortClick = { video ->
                navController.openShortsOrPlayer(ShortsQueueSource.Saved(video.id), disableShortsPlayer)
            },
        )
    }

    page(
        route = SEARCH_ROUTE_PATTERN,
        arguments =
            listOf(
                navArgument(SEARCH_QUERY_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(SEARCH_FILTERS_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(SEARCH_DEPTH_ARG) {
                    type = NavType.IntType
                    defaultValue = 1
                },
            ),
    ) { entry ->
        val depth = entry.arguments?.getInt(SEARCH_DEPTH_ARG) ?: 1
        // Search owns the whole screen, the way YouTube's does.
        SearchScreen(
            onNewSearch = { query, filters ->
                navController.navigate(searchRoute(query, filters, minOf(depth + 1, MAX_SEARCH_DEPTH))) {
                    if (depth >= MAX_SEARCH_DEPTH) popUpTo(entry.destination.id) { inclusive = true }
                }
            },
            onVideoClick = { video ->
                navController.openVideoOrShorts(video, disableShortsPlayer) { playerViewModel.playVideo(it) }
            },
            onShortsQueue = { source ->
                navController.openShortsOrPlayer(source, disableShortsPlayer)
            },
            onChannelClick = { channel ->
                mediaNavigator.openChannel(channel.url.ifBlank { channel.id })
            },
            onPlaylistClick = { playlist ->
                navController.navigate("playlist/${playlist.id}")
            },
            onBack = {
                // As the opening tab Search has nothing under it, and Home may be hidden (#1697).
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else {
                    navController.navigateToTab(searchExitTab(), defaultStartRoute)
                }
            },
        )
    }

    page(EQUALIZER_ROUTE) {
        CompositionLocalProvider(LocalNewSettings provides rememberNewSettings()) {
            EqualizerScreen(onBack = { navController.popBackStack() })
        }
    }

    page("categories") {
        com.voxtube.app.ui.screens.categories.CategoriesScreen(
            onVideoClick = { video ->
                navController.openVideoOrShorts(video, disableShortsPlayer) { playerViewModel.playVideo(it) }
            },
            onShortClick = { videoId ->
                navController.openShortsOrPlayer(ShortsQueueSource.SeededFeed(videoId), disableShortsPlayer)
            },
            onPlaylistClick = { playlistId ->
                navController.navigate("playlist/$playlistId")
            },
        )
    }

    page(
        route = "settings?target={target}",
        arguments =
            listOf(
                navArgument("target") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        SettingsHost(
            start = SettingsTarget.decode(backStackEntry.arguments?.getString("target")),
            onExit = { navController.popBackStack() },
            onOpenRecap = { navController.navigate(RecapRoutes.stats()) },
            onOpenUpdate = { navController.navigate(UPDATE_ROUTE) },
        )
    }

    page(
        route = RecapRoutes.STATS,
        arguments =
            listOf(
                navArgument(RecapRoutes.ARG_PERIOD) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        RecapScreen(
            onBack = { navController.popBackStack() },
            onPlayStory = { period, source -> navController.navigate(RecapRoutes.story(period, source)) },
            startAt = RecapRoutes.decode(backStackEntry.arguments?.getString(RecapRoutes.ARG_PERIOD)),
        )
    }

    page(
        route = RecapRoutes.STORY,
        arguments =
            listOf(
                navArgument(RecapRoutes.ARG_PERIOD) { type = NavType.StringType },
                navArgument(RecapRoutes.ARG_SOURCE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val period = RecapRoutes.decode(backStackEntry.arguments?.getString(RecapRoutes.ARG_PERIOD))
        if (period == null) {
            LaunchedEffect(Unit) { navController.popBackStack() }
        } else {
            RecapStoryScreen(
                period = period,
                source = RecapRoutes.decodeSource(backStackEntry.arguments?.getString(RecapRoutes.ARG_SOURCE)),
                onClose = { navController.popBackStack() },
            )
        }
    }

    page(
        route = "channel?url={channelUrl}",
        arguments = listOf(navArgument("channelUrl") { type = NavType.StringType }),
    ) { backStackEntry ->
        val channelUrl =
            backStackEntry.arguments?.getString("channelUrl")?.let {
                java.net.URLDecoder.decode(it, "UTF-8")
            } ?: ""

        ChannelScreen(
            channelUrl = channelUrl,
            onVideoClick = { video ->
                navController.openVideoOrShorts(video, disableShortsPlayer) { playerViewModel.playVideo(it) }
            },
            onChannelClick = { channelId ->
                mediaNavigator.openChannel(channelId)
            },
            onShortClick = { videoId, sortIndex ->
                navController.openShortsOrPlayer(
                    ShortsQueueSource.Channel(channelUrl = channelUrl, startVideoId = videoId, sortIndex = sortIndex),
                    disableShortsPlayer,
                )
            },
            onPlaylistClick = { playlistId ->
                navController.navigate("playlist/$playlistId")
            },
            onBackClick = { navController.popBackStack() },
        )
    }

    // History Screen
    page("history") {
        val musicPlayerViewModel = sharedMusicPlayerViewModel()
        HistoryScreen(
            onVideoClick = { track ->
                val deviceFile = LocalMediaIds.videoUri(track.videoId)
                if (deviceFile != null) {
                    val video =
                        com.voxtube.app.data.model.Video(
                            id = track.videoId,
                            title = track.title,
                            channelName = track.artist,
                            channelId = "",
                            thumbnailUrl = deviceFile.toString(),
                            duration = track.duration,
                            viewCount = 0,
                            uploadDate = "",
                        )
                    playerViewModel.playLocalVideo(video, deviceFile.toString())
                } else {
                    playerViewModel.playVideo(track.toVideo())
                }
            },
            onShortsQueue = { source ->
                navController.openShortsOrPlayer(source, disableShortsPlayer)
            },
            onMusicClick = { track, queue ->
                musicPlayerViewModel.loadAndPlayTrack(track, queue, "History")
                onMusicStarted()
            },
            onBackClick = { navController.popBackStack() },
        )
    }

    // Playlists Screen
    page(
        route = "playlists?kind={kind}",
        arguments =
            listOf(
                navArgument("kind") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val fixedKind =
            backStackEntry.arguments?.getString("kind")?.let { name ->
                com.voxtube.app.ui.components.shared.MediaKind.entries
                    .firstOrNull { it.name == name }
            }
        PlaylistsScreen(
            fixedKind = fixedKind,
            onBackClick = { navController.popBackStack() },
            onVideoPlaylistClick = { playlist ->
                navController.navigate("playlist/${playlist.id}")
            },
            onMusicPlaylistClick = { playlist ->
                mediaNavigator.openMusicPlaylist(playlist.id)
            },
        )
    }

    // Playlist Detail Screen
    page(
        "playlist/{playlistId}",
        arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
    ) { _ ->
        val musicPlayerViewModel = sharedMusicPlayerViewModel()
        PlaylistDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onPlayPlaylist = { videos, index, shuffle, title, more ->
                val start = videos[index]
                if (start.isMusic) {
                    // A YouTube Music playlist plays in the music player, like any other song list.
                    val tracks = videos.filter { it.isMusic }.map { it.toMusicTrack() }
                    musicPlayerViewModel.loadAndPlayTrack(start.toMusicTrack(), tracks, title)
                    onMusicStarted()
                } else {
                    playerViewModel.playPlaylist(videos, index, title, shuffle, more)
                }
            },
        )
    }

    // Saved Shorts Grid
    page("savedShorts") {
        com.voxtube.app.ui.screens.library.SavedShortsGridScreen(
            onBackClick = { navController.popBackStack() },
            onVideoClick = { videoId ->
                navController.openShortsOrPlayer(ShortsQueueSource.Saved(videoId), disableShortsPlayer)
            },
        )
    }

    page("notes") {
        com.voxtube.app.ui.screens.notes.NotesScreen(
            onBackClick = { navController.popBackStack() },
            onPlay = { video, startPositionMs -> playerViewModel.playVideo(video, startPositionMs = startPositionMs) },
        )
    }
    page("localMedia") {
        val musicPlayerViewModel = sharedMusicPlayerViewModel()
        val localTitle =
            androidx.compose.ui.res
                .stringResource(com.voxtube.app.R.string.local_media_title)
        com.voxtube.app.ui.screens.library.LocalMediaScreen(
            onBackClick = { navController.popBackStack() },
            onPlayVideos = { items, index, shuffle ->
                playerViewModel.playPlaylist(items.map { it.toVideo() }, index, localTitle, shuffle)
            },
            onPlayMusic = { items, index, shuffle ->
                val tracks = items.map { it.toMusicTrack() }.let { if (shuffle) it.shuffled() else it }
                val start = if (shuffle) tracks.first() else tracks[index]
                musicPlayerViewModel.loadAndPlayTrack(start, tracks, localTitle)
                onMusicStarted()
            },
            onOpenSettings = {
                navController.navigate("settings?target=${SettingsTarget(SettingsDestination.LOCAL_MEDIA).encode()}")
            },
        )
    }

    downloadsRoutes(navController, mediaNavigator, playerViewModel, onMusicStarted)

    postRoutes(navController, mediaNavigator)

    musicRoutes(navController, mediaNavigator, currentRoute, playerViewModel, defaultStartRoute, onMusicStarted)

    widgetPlaybackRoutes(navController, defaultStartRoute, playerViewModel, onMusicStarted)

    linkPlaybackRoutes(
        navController = navController,
        playerViewModel = playerViewModel,
        playerUiStateResult = playerUiStateResult,
        playerSheetState = playerSheetState,
        playerVisibleState = playerVisibleState,
        defaultStartRoute = defaultStartRoute,
        onMusicStarted = onMusicStarted,
    )
}
