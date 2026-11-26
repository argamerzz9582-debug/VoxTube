package com.voxtube.app.ui.screens.search

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.voxtube.app.R
import com.voxtube.app.data.local.ContentType
import com.voxtube.app.data.local.HomeFeedColumns
import com.voxtube.app.data.local.SearchFilter
import com.voxtube.app.data.local.WatchState
import com.voxtube.app.data.model.Channel
import com.voxtube.app.data.model.Playlist
import com.voxtube.app.data.model.Video
import com.voxtube.app.data.paging.SearchResultItem
import com.voxtube.app.data.paging.supportsVideoFilters
import com.voxtube.app.data.shorts.queue.ShortsQueueSource
import com.voxtube.app.ui.components.FEED_MAX_AUTO_COLUMNS
import com.voxtube.app.ui.components.FeedCardMinWidth
import com.voxtube.app.ui.components.layout.navigation.LocalMediaNavigator
import com.voxtube.app.ui.components.rememberFeedGridLayout
import com.voxtube.app.ui.components.search.SearchCorrectionRow
import com.voxtube.app.ui.components.search.SearchFilterBar
import com.voxtube.app.ui.components.search.SearchResultActions
import com.voxtube.app.ui.components.search.SearchResults
import com.voxtube.app.ui.components.search.SearchShortsGrid
import com.voxtube.app.ui.components.shared.FeedGridSkeleton
import com.voxtube.app.ui.components.shared.FlowEmptyState
import com.voxtube.app.ui.components.shared.FlowErrorState
import com.voxtube.app.ui.components.shared.RetryWhenBackOnline
import com.voxtube.app.ui.components.shared.SubscriptionBellViewModel
import com.voxtube.app.ui.components.shared.quickactions.QuickActionsViewModel
import com.voxtube.app.ui.components.shared.sharedSubscriptionBellViewModel
import com.voxtube.app.utils.LoadErrorKind
import com.voxtube.app.utils.YouTubeLink
import com.voxtube.app.utils.parseYouTubeLink

private val FilterBarVerticalPadding = 4.dp

/**
 * Runs typed text as a search with the given filters, or opens it when it is a YouTube link, the way
 * every search field in the app treats a pasted link.
 */
@Composable
internal fun rememberSearchSubmitter(
    viewModel: SearchViewModel,
    state: SearchScreenState,
    quickActions: QuickActionsViewModel,
    onNewSearch: ((String, SearchFilter) -> Unit)? = null,
): (String, SearchFilter) -> Unit {
    val mediaNavigator = LocalMediaNavigator.current
    return remember(viewModel, state, quickActions, mediaNavigator, onNewSearch) {
        { raw, filters ->
            val text = raw.trim()
            val link = parseYouTubeLink(text)
            val search: (String) -> Unit = { query ->
                val shown = viewModel.uiState.value.query
                if (onNewSearch == null || shown.isBlank() || query.trim() == shown.trim()) {
                    state.onSubmit(query)
                    viewModel.submit(query, filters)
                } else {
                    // A new search gets its own page; this one goes back to its own results for Back to find.
                    state.returnTo(shown)
                    onNewSearch(query, filters)
                }
            }
            when {
                text.isEmpty() -> Unit
                link == null -> search(text)
                link is YouTubeLink.Search -> search(link.query)
                !mediaNavigator.openLink(link) -> quickActions.announce(R.string.link_not_supported)
            }
        }
    }
}

/**
 * A finished search: the filter bar over the results, through their loading, error and empty states.
 * [maxAutoColumns] and [minCellWidth] size the grid the way the caller's canvas needs.
 */
@Composable
internal fun ColumnScope.SearchResultsBody(
    viewModel: SearchViewModel,
    filters: SearchFilter,
    shortsEnabled: Boolean,
    isGridMode: Boolean,
    feedColumns: HomeFeedColumns,
    pagingItems: LazyPagingItems<SearchResultItem>,
    gridState: LazyGridState,
    quickActions: QuickActionsViewModel,
    onVideoClick: (Video) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onShortsQueue: (ShortsQueueSource) -> Unit,
    dismissKeyboard: () -> Unit,
    onSearch: (String) -> Unit,
    maxAutoColumns: Int = FEED_MAX_AUTO_COLUMNS,
    minCellWidth: Dp = FeedCardMinWidth,
    bell: SubscriptionBellViewModel = sharedSubscriptionBellViewModel(),
) {
    val subscribedIds by quickActions.subscribedChannelIds.collectAsStateWithLifecycle()
    val notifyingIds by bell.notifyingChannelIds.collectAsStateWithLifecycle()
    val currentOnVideoClick by rememberUpdatedState(onVideoClick)

    SearchFilterBar(
        filter = filters,
        shortsEnabled = shortsEnabled,
        onFilterChange = viewModel::updateFilters,
        modifier = Modifier.padding(vertical = FilterBarVerticalPadding),
    )

    RetryWhenBackOnline(pagingItems)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    uiState.header.correction
        ?.takeUnless { uiState.exactQuery }
        ?.let { correction ->
            SearchCorrectionRow(
                correction = correction,
                onSearchExactly = viewModel::searchExactly,
                onSearchCorrected = onSearch,
            )
        }

    val refreshState = pagingItems.loadState.refresh
    BoxWithConstraints(modifier = Modifier.weight(1f)) {
        val feedLayout = rememberFeedGridLayout(maxWidth, feedColumns, maxAutoColumns, minCellWidth)
        val actions =
            remember(feedLayout, subscribedIds, notifyingIds) {
                SearchResultActions(
                    onVideoClick = { currentOnVideoClick(it) },
                    onShortsClick = { shelf, tapped ->
                        onShortsQueue(viewModel.shortsShelfSource(shelf, tapped))
                    },
                    onChannelClick = onChannelClick,
                    onPlaylistClick = onPlaylistClick,
                    dismissKeyboard = dismissKeyboard,
                    isSubscribed = { it in subscribedIds },
                    onSubscribeToggle = { channel ->
                        quickActions.toggleSubscription(channel.id, channel.name, channel.thumbnailUrl)
                    },
                    isNotifying = { it in notifyingIds },
                    onNotificationChange = { channel, enabled -> bell.setNotifications(channel.id, enabled) },
                    onPostClick = viewModel.postComments::open,
                )
            }

        when {
            refreshState is LoadState.Loading -> {
                FeedGridSkeleton(layout = feedLayout, listMode = isGridMode)
            }

            refreshState is LoadState.Error && pagingItems.itemCount == 0 -> {
                FlowErrorState(
                    error = stringResource(LoadErrorKind.of(refreshState.error).messageRes(R.string.search_failed)),
                    onRetry = pagingItems::retry,
                )
            }

            pagingItems.itemCount == 0 -> {
                FlowEmptyState(
                    title = stringResource(filters.emptyResultsRes()),
                    icon = Icons.Rounded.Search,
                )
            }

            filters.contentType == ContentType.SHORTS -> {
                SearchShortsGrid(pagingItems, gridState, feedLayout, actions)
            }

            else -> {
                SearchResults(pagingItems, gridState, feedLayout, isGridMode, actions)
            }
        }
    }
}

private fun SearchFilter.emptyResultsRes(): Int =
    when {
        !contentType.supportsVideoFilters() -> R.string.no_results_found
        watchState == WatchState.WATCHED -> R.string.search_watch_empty_watched
        watchState == WatchState.UNWATCHED -> R.string.search_watch_empty_unwatched
        else -> R.string.no_results_found
    }
