package com.voxtube.app.ui.tv.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.voxtube.app.R
import com.voxtube.app.data.model.Video
import com.voxtube.app.data.music.DownloadedTrack
import com.voxtube.app.data.music.model.toMusicTrack
import com.voxtube.app.data.music.model.toVideo
import com.voxtube.app.data.video.DownloadedVideo
import com.voxtube.app.player.queue.QueuePageSource
import com.voxtube.app.ui.components.layout.navigation.VoxTubeTab
import com.voxtube.app.ui.components.shared.MediaKind
import com.voxtube.app.ui.screens.channel.ChannelScreen
import com.voxtube.app.ui.screens.history.HistoryScreen
import com.voxtube.app.ui.screens.home.HomeViewModel
import com.voxtube.app.ui.screens.library.DownloadsScreen
import com.voxtube.app.ui.screens.music.MusicPlayerViewModel
import com.voxtube.app.ui.screens.playlists.PlaylistDetailScreen
import com.voxtube.app.ui.screens.playlists.PlaylistsScreen
import com.voxtube.app.ui.screens.search.SearchViewModel
import com.voxtube.app.ui.screens.subscriptions.SubscriptionsViewModel
import com.voxtube.app.ui.tv.components.TvScreenFrame
import com.voxtube.app.ui.tv.screens.TvHomeScreen
import com.voxtube.app.ui.tv.screens.TvLibraryScreen
import com.voxtube.app.ui.tv.screens.TvSearchScreen
import com.voxtube.app.ui.tv.screens.TvSubscriptionsScreen

/** Top-level TV navigation graph plus detail routes (channel, …). */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun TvNavHost(
    navController: NavHostController,
    startRoute: String,
    homeViewModel: HomeViewModel,
    musicPlayerViewModel: MusicPlayerViewModel,
    onPlayVideo: (Video) -> Unit,
    onPlayPlaylist: (videos: List<Video>, startIndex: Int, title: String, shuffle: Boolean, more: QueuePageSource?) -> Unit,
    onPlayVideoId: (String) -> Unit,
    onSelectRoot: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Activity scoped like the phone tabs, but created when their page first opens: Home alone is on screen at launch.
    val activity = LocalContext.current as ComponentActivity
    val openChannel: (String) -> Unit = { channelRef ->
        navController.navigate(TvRoutes.channel(channelRef))
    }
    val downloadsTitle = stringResource(R.string.library_downloads_label)
    val playDownloadedVideos: (List<DownloadedVideo>, Int) -> Unit = { videos, index ->
        onPlayPlaylist(videos.map { it.video }, index, downloadsTitle, false, null)
    }
    val playDownloadedMusic: (List<DownloadedTrack>, Int) -> Unit = { tracks, index ->
        val queue = tracks.map { it.track }
        musicPlayerViewModel.loadAndPlayTrack(queue[index], queue, downloadsTitle)
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        modifier = modifier,
    ) {
        composable(VoxTubeTab.Home.route) {
            TvHomeScreen(
                viewModel = homeViewModel,
                onVideoClick = onPlayVideo,
                onPlayPlaylist = { videos, title -> onPlayPlaylist(videos, 0, title, false, null) },
                onOpenLibrary = { onSelectRoot(VoxTubeTab.Library.route) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(VoxTubeTab.Subscriptions.route) {
            TvSubscriptionsScreen(
                viewModel = hiltViewModel<SubscriptionsViewModel>(activity),
                onVideoClick = onPlayVideo,
                onChannelClick = { channel -> openChannel(channel.url.ifBlank { channel.id }) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(VoxTubeTab.Search.route) {
            TvSearchScreen(
                viewModel = hiltViewModel<SearchViewModel>(activity),
                onVideoClick = onPlayVideo,
                onChannelClick = { channel -> openChannel(channel.url.ifBlank { channel.id }) },
                onPlaylistClick = { playlist -> navController.navigate(TvRoutes.playlist(playlist.id)) },
                onPlayTrack = musicPlayerViewModel::loadAndPlayTrack,
                onOpenAlbum = { navController.navigate(TvRoutes.musicCollection(it)) },
                onOpenArtist = { navController.navigate(TvRoutes.musicArtist(it)) },
                onOpenChannel = { openChannel(it) },
                onOpenMusicPlaylist = { navController.navigate(TvRoutes.musicCollection(it)) },
                onOpenMood = { navController.navigate(TvRoutes.musicBrowse(it)) },
                onOpenMoods = { navController.navigate(TvRoutes.MOODS) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(VoxTubeTab.Library.route) {
            TvLibraryScreen(
                onVideoClick = onPlayVideo,
                onMusicClick = musicPlayerViewModel::loadAndPlayTrack,
                onOpenHistory = { navController.navigate(TvRoutes.HISTORY) },
                onOpenPlaylists = { kind -> navController.navigate(TvRoutes.playlists(kind)) },
                onOpenPlaylist = { navController.navigate(TvRoutes.playlist(it)) },
                onOpenMusicPlaylist = { navController.navigate(TvRoutes.musicCollection(it)) },
                onOpenDownloads = { navController.navigate(TvRoutes.DOWNLOADS) },
                onDownloadedVideoClick = playDownloadedVideos,
                onDownloadedMusicClick = playDownloadedMusic,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(TvRoutes.DOWNLOADS) {
            TvScreenFrame {
                DownloadsScreen(
                    onBackClick = { navController.popBackStack() },
                    onVideoClick = playDownloadedVideos,
                    onMusicClick = playDownloadedMusic,
                    onOpenCollection = { summary ->
                        val id = summary.collection.id
                        navController.navigate(if (summary.collection.kind.isMusic) TvRoutes.musicCollection(id) else TvRoutes.playlist(id))
                    },
                    onHomeClick = { onSelectRoot(startRoute) },
                )
            }
        }
        composable(TvRoutes.HISTORY) {
            TvScreenFrame {
                HistoryScreen(
                    onVideoClick = { track -> onPlayVideo(track.toVideo()) },
                    onBackClick = { navController.popBackStack() },
                    onShortsQueue = {},
                    onMusicClick = { track, queue -> musicPlayerViewModel.loadAndPlayTrack(track, queue) },
                )
            }
        }
        composable(
            route = TvRoutes.PLAYLISTS,
            arguments =
                listOf(
                    navArgument(TvRoutes.PLAYLISTS_KIND_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) { entry ->
            val kind =
                entry.arguments?.getString(TvRoutes.PLAYLISTS_KIND_ARG)?.let { name ->
                    MediaKind.entries.firstOrNull {
                        it.name ==
                            name
                    }
                }
            TvScreenFrame {
                PlaylistsScreen(
                    fixedKind = kind,
                    onBackClick = { navController.popBackStack() },
                    onVideoPlaylistClick = { navController.navigate(TvRoutes.playlist(it.id)) },
                    onMusicPlaylistClick = { navController.navigate(TvRoutes.musicCollection(it.id)) },
                )
            }
        }
        composable(
            route = TvRoutes.CHANNEL,
            arguments =
                listOf(
                    navArgument(TvRoutes.CHANNEL_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
        ) { entry ->
            val channelRef =
                entry.arguments
                    ?.getString(TvRoutes.CHANNEL_ARG)
                    ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                    .orEmpty()
            TvScreenFrame {
                ChannelScreen(
                    channelUrl = channelRef,
                    onVideoClick = onPlayVideo,
                    onChannelClick = openChannel,
                    onShortClick = { videoId, _ -> onPlayVideoId(videoId) },
                    onPlaylistClick = { navController.navigate(TvRoutes.playlist(it)) },
                    onBackClick = { navController.popBackStack() },
                    showShorts = false,
                )
            }
        }
        composable(
            route = TvRoutes.PLAYLIST,
            arguments = listOf(navArgument(TvRoutes.PLAYLIST_ARG) { type = NavType.StringType }),
        ) {
            TvScreenFrame {
                PlaylistDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPlayPlaylist = { videos, index, shuffle, title, more ->
                        val start = videos[index]
                        if (start.isMusic) {
                            val tracks = videos.filter { it.isMusic }.map { it.toMusicTrack() }
                            musicPlayerViewModel.loadAndPlayTrack(start.toMusicTrack(), tracks, title)
                        } else {
                            onPlayPlaylist(videos, index, title, shuffle, more)
                        }
                    },
                )
            }
        }
        tvMusicRoutes(
            navController = navController,
            musicPlayerViewModel = musicPlayerViewModel,
            onPlayVideo = onPlayVideo,
            onSelectRoot = onSelectRoot,
        )
        tvSettingsRoutes(navController = navController)
    }
}
