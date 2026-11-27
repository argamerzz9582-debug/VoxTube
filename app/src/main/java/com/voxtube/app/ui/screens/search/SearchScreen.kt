package com.voxtube.app.ui.screens.search

import android.app.Activity
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.voxtube.app.R
import com.voxtube.app.data.local.SearchFilter
import com.voxtube.app.data.local.availableWith
import com.voxtube.app.data.model.Channel
import com.voxtube.app.data.model.Playlist
import com.voxtube.app.data.model.Video
import com.voxtube.app.data.paging.SearchResultItem
import com.voxtube.app.data.shorts.queue.ShortsQueueSource
import com.voxtube.app.ui.OnTabReselected
import com.voxtube.app.ui.components.layout.navigation.VoxTubeTab
import com.voxtube.app.ui.components.search.SearchFilterDialog
import com.voxtube.app.ui.components.search.SearchSuggestionsPanel
import com.voxtube.app.ui.components.search.SearchTopBar
import com.voxtube.app.ui.components.search.SearchTopBarActions
import com.voxtube.app.ui.components.shared.MediaPostCommentsSheet
import com.voxtube.app.ui.components.shared.quickactions.QuickActionsViewModel
import com.voxtube.app.ui.components.shared.quickactions.sharedQuickActionsViewModel
import com.voxtube.app.utils.canVoiceSearch
import com.voxtube.app.utils.launchVoiceSearch

/**
 * The route: a bar, then either what the user might be looking for or what they found.
 *
 * Typing is a mode of this screen rather than a surface stacked on it, so back always leaves —
 * it never has a collapse step to fall into first.
 */
@Composable
fun SearchScreen(
    onVideoClick: (Video) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onShortsQueue: (ShortsQueueSource) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNewSearch: ((String, SearchFilter) -> Unit)? = null,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val state = rememberSearchState(viewModel)

    val quickActions: QuickActionsViewModel = sharedQuickActionsViewModel()
    val pagingItems = viewModel.searchResults.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    OnTabReselected(VoxTubeTab.Search.route) { gridState.animateScrollToItem(0) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val submitWith = rememberSearchSubmitter(viewModel, state, quickActions, onNewSearch)
    val submit: (String) -> Unit = { raw -> submitWith(raw, uiState.filters) }

    val voiceSearchAvailable = remember(context) { canVoiceSearch(context) }
    val voiceSearchLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf(String::isNotBlank)
                ?.let { spoken ->
                    state.textFieldState.setTextAndPlaceCursorAtEnd(spoken)
                    submit(spoken)
                }
        }

    val showResults = uiState.query.isNotBlank() && !state.isTyping
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(showResults) {
        if (showResults) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        } else {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    // A new query starts at the top; coming back from a result keeps the grid where it was.
    var scrolledForQuery by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.query) {
        if (uiState.query.isNotBlank() && scrolledForQuery != uiState.query) {
            if (scrolledForQuery != null) gridState.scrollToItem(0)
            scrolledForQuery = uiState.query
        }
    }

    LaunchedEffect(pagingItems.itemSnapshotList.items) {
        pagingItems.itemSnapshotList.items
            .filterIsInstance<SearchResultItem.ChannelResult>()
            .forEach { quickActions.loadSubscriptionState(it.channel.id) }
    }

    // Back leaves the screen, but a query typed over a finished search returns to that search first.
    BackHandler(enabled = state.isTyping && uiState.query.isNotBlank()) { state.stopTyping() }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchTopBar(
                textFieldState = state.textFieldState,
                onSearch = submit,
                onBack = onBack,
                onVoiceSearch =
                    if (voiceSearchAvailable) {
                        { launchVoiceSearch(context.getString(R.string.search_voice_prompt), voiceSearchLauncher::launch) }
                    } else {
                        null
                    },
                focusRequester = focusRequester,
                onFieldFocused = state::startTyping,
                privateKeyboard = state.deepFlow.active,
                actions =
                    if (showResults) {
                        {
                            SearchTopBarActions(
                                activeFilterCount = uiState.filters.activeCount,
                                isGridMode = state.isGridMode,
                                onOpenFilters = { showFilters = true },
                                onToggleGridMode = state::toggleGridMode,
                            )
                        }
                    } else {
                        null
                    },
            )

            if (!showResults) {
                SearchSuggestionsPanel(
                    query = state.query,
                    history = state.matchingHistory,
                    suggestions = state.suggestions,
                    onSubmit = { text ->
                        state.textFieldState.setTextAndPlaceCursorAtEnd(text)
                        submit(text)
                    },
                    onHistorySelect = { item ->
                        state.textFieldState.setTextAndPlaceCursorAtEnd(item.query)
                        submitWith(item.query, item.filters?.availableWith(state.shortsContentEnabled) ?: uiState.filters)
                    },
                    onFill = state.textFieldState::setTextAndPlaceCursorAtEnd,
                    onDeleteHistoryItem = state::deleteHistoryItem,
                    onClearHistory = state::clearHistory,
                    deepFlow = state.deepFlow,
                    onDeepFlowChange = state::setDeepFlowEnabled,
                )
                return@Column
            }

            SearchResultsBody(
                viewModel = viewModel,
                filters = uiState.filters,
                shortsEnabled = state.shortsContentEnabled,
                isGridMode = state.isGridMode,
                feedColumns = state.feedColumns,
                pagingItems = pagingItems,
                gridState = gridState,
                quickActions = quickActions,
                onVideoClick = onVideoClick,
                onChannelClick = onChannelClick,
                onPlaylistClick = onPlaylistClick,
                onShortsQueue = onShortsQueue,
                dismissKeyboard = state::stopTyping,
                onSearch = submit,
            )
        }
        MediaPostCommentsSheet(
            loader = viewModel.postComments,
            onAuthorClick = { channelId -> onChannelClick(Channel(channelId, "", "", 0)) },
        )
    }

    if (showFilters) {
        SearchFilterDialog(
            filter = uiState.filters,
            shortsEnabled = state.shortsContentEnabled,
            onApply = {
                showFilters = false
                viewModel.updateFilters(it)
            },
            onDismiss = { showFilters = false },
        )
    }
}
