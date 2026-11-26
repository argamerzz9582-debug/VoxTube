@file:Suppress("ktlint:standard:backing-property-naming")

package com.voxtube.app.ui.screens.search

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.voxtube.app.data.community.PostCommentsLoader
import com.voxtube.app.data.engagement.HiddenContentStore
import com.voxtube.app.data.local.HomeFeedColumns
import com.voxtube.app.data.local.PlayerPreferences
import com.voxtube.app.data.local.PrivacyGate
import com.voxtube.app.data.local.SearchFilter
import com.voxtube.app.data.local.SearchHistoryItem
import com.voxtube.app.data.local.SearchHistoryRepository
import com.voxtube.app.data.local.SearchHistoryScope
import com.voxtube.app.data.local.dao.WatchHistoryDao
import com.voxtube.app.data.model.Video
import com.voxtube.app.data.paging.SearchPagingSource
import com.voxtube.app.data.paging.SearchResultItem
import com.voxtube.app.data.paging.WatchedVideoLookup
import com.voxtube.app.data.paging.watchedVideoIds
import com.voxtube.app.data.paging.withoutHidden
import com.voxtube.app.data.recommendation.FlowNeuroEngine
import com.voxtube.app.data.search.SearchSuggestionsRepository
import com.voxtube.app.data.shorts.ShortsContentFilter
import com.voxtube.app.data.shorts.queue.ShortsQueueHandoff
import com.voxtube.app.data.shorts.queue.ShortsQueueSource
import com.voxtube.app.data.stats.VideoStatsRecorder
import com.voxtube.app.innertube.pages.search.SearchHeader
import com.voxtube.app.innertube.pages.search.SearchSuggestion
import com.voxtube.app.ui.components.shared.DeepFlowState
import com.voxtube.app.utils.PerformanceDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val filters: SearchFilter = SearchFilter.DEFAULT,
    val header: SearchHeader = SearchHeader(),
    /** The query runs as typed, without YouTube's spelling correction. */
    val exactQuery: Boolean = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val suggestionsRepository: SearchSuggestionsRepository,
        private val shortsContentFilter: ShortsContentFilter,
        private val shortsQueueHandoff: ShortsQueueHandoff,
        private val searchHistory: SearchHistoryRepository,
        private val videoStats: VideoStatsRecorder,
        private val neuroEngine: FlowNeuroEngine,
        private val watchHistoryDao: WatchHistoryDao,
        private val preferences: PlayerPreferences,
        private val privacyGate: PrivacyGate,
        private val savedState: SavedStateHandle,
        hiddenContent: HiddenContentStore,
    ) : ViewModel() {
        val postComments = PostCommentsLoader(viewModelScope)

        // Signal each distinct submitted query once — typing and filter churn stay silent.
        private var lastSignaledQuery: String? = null
        private val _uiState = MutableStateFlow(SearchUiState())
        val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

        private data class SearchKey(
            val query: String,
            val filter: SearchFilter,
            val exactQuery: Boolean = false,
        )

        private val _searchKey = MutableStateFlow<SearchKey?>(null)

        val searchResults: Flow<PagingData<SearchResultItem>> =
            _searchKey
                .filterNotNull()
                .filter { it.query.isNotBlank() }
                .combine(shortsContentFilter.enabled) { key, shortsEnabled -> key to shortsEnabled }
                .flatMapLatest { (key, shortsEnabled) ->
                    Pager(
                        config =
                            PagingConfig(
                                pageSize = 20,
                                prefetchDistance = 6,
                                enablePlaceholders = false,
                                initialLoadSize = 20,
                            ),
                        pagingSourceFactory = {
                            SearchPagingSource(
                                query = key.query,
                                filter = key.filter,
                                exactQuery = key.exactQuery,
                                shortsEnabled = shortsEnabled,
                                onHeader = ::onHeader,
                                watchedVideos = watchedVideos,
                                exclusions = { neuroEngine.feedExclusions() },
                            )
                        },
                    ).flow
                }.cachedIn(viewModelScope)
                .combine(hiddenContent.hidden) { results, hidden -> results.withoutHidden(hidden) }

        private val watchedVideos =
            WatchedVideoLookup { videos ->
                if (videos.isEmpty()) return@WatchedVideoLookup emptySet()
                val progress = watchHistoryDao.getProgressFor(videos.map { it.id }.distinct())
                watchedVideoIds(videos, progress, preferences.watchedThreshold.first(), preferences.countImportedAsWatched.first())
            }

        fun shortsShelfSource(
            shelf: List<Video>,
            tapped: Video,
        ): ShortsQueueSource = shortsQueueHandoff.sourceForShelf(shelf, tapped)

        fun search(
            query: String,
            filters: SearchFilter = SearchFilter.DEFAULT,
        ) {
            if (query.isBlank()) {
                clearSearch()
                return
            }
            _uiState.value = SearchUiState(query = query, filters = filters)
            _searchKey.value = SearchKey(query, filters)

            // A typed search is the most explicit interest statement the user makes.
            val normalized = query.trim().lowercase()
            if (normalized != lastSignaledQuery) {
                lastSignaledQuery = normalized
                viewModelScope.launch {
                    runCatching { FlowNeuroEngine.onSearchQuery(context, query) }
                    videoStats.onSearch(query.takeIf { searchHistory.isSearchHistoryEnabled() })
                }
            }
        }

        /** A search the user asked for, kept in history with its filters. Typing on TV goes through [search] and is not kept. */
        fun submit(
            query: String,
            filters: SearchFilter,
        ) {
            search(query, filters)
            rememberSearch(query, filters)
        }

        fun updateFilters(filters: SearchFilter) {
            val current = _uiState.value
            _uiState.value = current.copy(filters = filters)
            if (current.query.isNotBlank()) {
                _searchKey.value = SearchKey(current.query, filters, current.exactQuery)
                rememberSearch(current.query, filters)
            }
        }

        /** "Search instead for": the shown query again, as typed, keeping the filters. */
        fun searchExactly(query: String) {
            if (query.isBlank()) return
            val filters = _uiState.value.filters
            _uiState.value = SearchUiState(query = query, filters = filters, exactQuery = true)
            _searchKey.value = SearchKey(query, filters, exactQuery = true)
        }

        private fun rememberSearch(
            query: String,
            filters: SearchFilter,
        ) {
            if (query.isBlank()) return
            viewModelScope.launch {
                if (privacyGate.allowsSearchHistory()) searchHistory.saveSearchQuery(query, filters = filters)
            }
        }

        fun clearSearch() {
            _uiState.value = SearchUiState()
            _searchKey.value = null
        }

        /** Rows for [query], or null when the request failed and the rows on screen should stay. */
        suspend fun getSearchSuggestions(query: String): List<SearchSuggestion>? = suggestionsRepository.suggestions(query)

        fun warmSearchSuggestions() {
            viewModelScope.launch(PerformanceDispatcher.networkIO) { suggestionsRepository.warm() }
        }

        val searchHistoryItems: Flow<List<SearchHistoryItem>> = searchHistory.getSearchHistoryFlow()
        val suggestionsEnabled: Flow<Boolean> = searchHistory.isSearchSuggestionsEnabledFlow()
        val gridMode: Flow<Boolean> = preferences.searchIsGridMode
        val shortsContentEnabled: Flow<Boolean> = preferences.shortsContentEnabled
        val feedColumns: Flow<HomeFeedColumns> = preferences.homeFeedColumns
        val deepFlow: Flow<DeepFlowState> =
            combine(
                preferences.deepFlowOnSearch,
                preferences.deepFlowActive,
                preferences.deepFlowActivatedAt,
                preferences.deepFlowExpireHours,
            ) { onSearch, active, activatedAt, expireHours ->
                DeepFlowState(active = active, activatedAt = activatedAt, expireHours = expireHours, showOnSearch = onSearch)
            }

        fun deleteHistoryItem(item: SearchHistoryItem) {
            viewModelScope.launch { searchHistory.deleteSearchItem(item.id) }
        }

        fun clearSearchHistory() {
            viewModelScope.launch { searchHistory.clearSearchHistory(SearchHistoryScope.VIDEO) }
            videoStats.onSearchHistoryCleared()
        }

        fun setGridMode(enabled: Boolean) {
            viewModelScope.launch { preferences.setSearchIsGridMode(enabled) }
        }

        private fun onHeader(header: SearchHeader) {
            _uiState.update { it.copy(header = header) }
        }

        // A search page opened for a query runs it once; a recreated process shows it again without
        // adding it to history a second time.
        init {
            savedState.get<String>(SEARCH_QUERY_ARG)?.takeIf(String::isNotBlank)?.let { query ->
                val filters = searchFilterFromRouteArg(savedState.get<String>(SEARCH_FILTERS_ARG))
                if (savedState.get<Boolean>(RAN_KEY) == true) {
                    search(query, filters)
                } else {
                    submit(query, filters)
                    savedState[RAN_KEY] = true
                }
            }
        }

        private companion object {
            const val RAN_KEY = "search_ran"
        }
    }
