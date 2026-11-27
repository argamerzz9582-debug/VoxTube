package com.voxtube.app.ui.screens.music

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.voxtube.app.R
import com.voxtube.app.ui.components.layout.topbar.VoxTubeTopBar
import com.voxtube.app.ui.components.shared.VoxTubeErrorState

/** The artist route when the page could not be loaded: the error with Retry, under a bar that leads back. */
@Composable
fun ArtistPageError(
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { VoxTubeTopBar(title = "", onBack = onBackClick) },
    ) { padding ->
        VoxTubeErrorState(
            error = stringResource(R.string.error_failed_to_load_artist),
            onRetry = onRetry,
            modifier = Modifier.padding(padding),
        )
    }
}
