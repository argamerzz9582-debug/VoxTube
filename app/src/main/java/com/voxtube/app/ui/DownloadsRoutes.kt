package com.voxtube.app.ui

import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.voxtube.app.R
import com.voxtube.app.player.GlobalPlayerState
import com.voxtube.app.ui.components.layout.navigation.DOWNLOADS_ROUTE
import com.voxtube.app.ui.components.layout.navigation.DOWNLOADS_TAB_ROUTE
import com.voxtube.app.ui.components.layout.navigation.MediaNavigator
import com.voxtube.app.ui.screens.library.DownloadsScreen
import com.voxtube.app.ui.screens.music.sharedMusicPlayerViewModel
import com.voxtube.app.ui.screens.player.VideoPlayerViewModel

/** The downloads page, opened over a screen with a back arrow, and as the Downloads tab without one. */
internal fun NavGraphBuilder.downloadsRoutes(
    navController: NavHostController,
    mediaNavigator: MediaNavigator,
    playerViewModel: VideoPlayerViewModel,
    onMusicStarted: () -> Unit,
) {
    listOf(DOWNLOADS_ROUTE, DOWNLOADS_TAB_ROUTE).forEach { route ->
        page(route) {
            val musicPlayerViewModel = sharedMusicPlayerViewModel()
            val downloadsTitle = stringResource(R.string.library_downloads_label)
            DownloadsScreen(
                onBackClick = if (route == DOWNLOADS_ROUTE) ({ navController.popBackStack() }) else null,
                onVideoClick = { videos, index ->
                    val videoList = videos.map { it.video }
                    playerViewModel.playPlaylist(videoList, index, downloadsTitle)
                    GlobalPlayerState.setCurrentVideo(videoList[index])
                },
                onMusicClick = { tracks, index ->
                    val musicTracks = tracks.map { it.track }
                    musicPlayerViewModel.loadAndPlayTrack(musicTracks[index], musicTracks, downloadsTitle)
                    onMusicStarted()
                },
                onOpenCollection = { summary ->
                    if (summary.collection.kind.isMusic) {
                        mediaNavigator.openMusicPlaylist(summary.collection.id)
                    } else {
                        navController.navigate("playlist/${summary.collection.id}")
                    }
                },
                onHomeClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
            )
        }
    }
}
