package com.voxtube.app.ui.screens.post

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxtube.app.data.community.PostCommentsLoader
import com.voxtube.app.innertube.YouTube
import com.voxtube.app.innertube.pages.renderer.CommunityPost
import com.voxtube.app.innertube.pages.renderer.SinglePostPage
import com.voxtube.app.utils.PerformanceDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val POST_ROUTE_ARG = "postId"

internal sealed interface PostUiState {
    data object Loading : PostUiState

    data object Failed : PostUiState

    data class Loaded(
        val post: CommunityPost,
    ) : PostUiState
}

/** One community post opened from its link, and its comments. */
@HiltViewModel
internal class PostViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val postId: String = savedStateHandle.get<String>(POST_ROUTE_ARG).orEmpty()

        private val _state = MutableStateFlow<PostUiState>(PostUiState.Loading)
        val state: StateFlow<PostUiState> = _state.asStateFlow()

        private var detail: SinglePostPage? = null
        private var loadJob: Job? = null

        /** The post's page already holds its comments' first token, so opening them does not fetch the page again. */
        val comments =
            PostCommentsLoader(
                scope = viewModelScope,
                fetchFirstPage = { id, params ->
                    detail?.takeIf { it.post.id == id }?.let { YouTube.communityPostComments(it.comments) }
                        ?: YouTube.communityPostComments(id, params)
                },
            )

        fun load() {
            if (_state.value is PostUiState.Loaded || loadJob?.isActive == true) return
            _state.value = PostUiState.Loading
            loadJob =
                viewModelScope.launch(PerformanceDispatcher.networkIO) {
                    YouTube.communityPost(postId).fold(
                        onSuccess = { page ->
                            detail = page
                            _state.value = PostUiState.Loaded(page.post)
                        },
                        onFailure = { error ->
                            Log.w(TAG, "Post $postId failed: ${error.message}")
                            _state.value = PostUiState.Failed
                        },
                    )
                }
        }

        private companion object {
            const val TAG = "PostViewModel"
        }
    }
