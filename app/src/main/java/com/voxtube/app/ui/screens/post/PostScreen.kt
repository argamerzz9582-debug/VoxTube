package com.voxtube.app.ui.screens.post

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voxtube.app.R
import com.voxtube.app.data.model.Video
import com.voxtube.app.ui.components.layout.topbar.VoxTubeTopBar
import com.voxtube.app.ui.components.shared.MediaPostCard
import com.voxtube.app.ui.components.shared.MediaPostCommentsSheet
import com.voxtube.app.ui.components.shared.VoxTubeErrorState
import com.voxtube.app.ui.components.shared.VoxTubeLoadingIndicator
import com.voxtube.app.ui.components.shared.rememberPostShareAction

private val PostMaxWidth = 760.dp

/** A single community post opened from a `/post/` link, in the same card and comments sheet as the posts lists. */
@Composable
internal fun PostScreen(
    onBack: () -> Unit,
    onVideoClick: (Video) -> Unit,
    onChannelClick: (String) -> Unit,
    viewModel: PostViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val share = rememberPostShareAction()
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { VoxTubeTopBar(title = stringResource(R.string.post_title), onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                PostUiState.Loading -> {
                    VoxTubeLoadingIndicator()
                }

                PostUiState.Failed -> {
                    VoxTubeErrorState(error = stringResource(R.string.post_load_failed), onRetry = viewModel::load)
                }

                is PostUiState.Loaded -> {
                    val post = current.post
                    LazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        item(key = post.id) {
                            MediaPostCard(
                                post = post,
                                onAuthorClick = { post.authorChannelId?.let(onChannelClick) },
                                onCommentsClick = { viewModel.comments.open(post) },
                                onShareClick = { share(post) },
                                onVideoClick = onVideoClick,
                                showDivider = false,
                                modifier = Modifier.fillMaxWidth().widthIn(max = PostMaxWidth),
                            )
                        }
                    }
                }
            }
        }
    }

    MediaPostCommentsSheet(loader = viewModel.comments, onAuthorClick = onChannelClick)
}
