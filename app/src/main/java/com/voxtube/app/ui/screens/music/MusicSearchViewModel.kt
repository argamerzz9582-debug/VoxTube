package com.voxtube.app.ui.screens.music

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxtube.app.R
import com.voxtube.app.data.contentfilter.aislist.AiListRepository
import com.voxtube.app.data.local.PlayerPreferences
import com.voxtube.app.data.local.PrivacyGate
import com.voxtube.app.data.local.SearchHistoryItem
import com.voxtube.app.data.local.SearchHistoryRepository
import com.voxtube.app.data.local.SearchHistoryScope
import com.voxtube.app.data.local.SearchType
import com.voxtube.app.data.model.distinctByNonBlankKey
import com.voxtube.app.data.music.DownloadManager
import com.voxtube.app.data.newmusic.InnertubeMusicService
import com.voxtube.app.innertube.YouTube
import com.voxtube.app.innertube.YouTube.SearchFilter
import com.voxtube.app.innertube.models.SearchSuggestions
import com.voxtube.app.innertube.models.YTItem
import com.voxtube.app.innertube.pages.ArtistSectionKind
import com.voxtube.app.innertube.pages.MoodAndGenres
import com.voxtube.app.innertube.pages.SearchSummaryPage
import com.voxtube.app.ui.components.search.SUGGESTION_TIMEOUT_MS
import com.voxtube.app.ui.components.search.collectLatestSuggestions
import com.voxtube.app.ui.components.search.recentOrMatching
import com.voxtube.app.utils.PerformanceDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MusicSearchViewModel
    @Inject
    constructor(
        private val downloadManager: DownloadManager,
        private val searchHistory: SearchHistoryRepository,
        private val privacyGate: PrivacyGate,
        @ApplicationContext private val context: Context,
        private val aiList: AiListRepository,
        preferences: PlayerPreferences,
    ) : ViewModel() {
        /** Deep Flow asks the keyboard not to learn what is typed here. */
        val deepFlowActive: StateFlow<Boolean> =
            preferences.deepFlowActive.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        private val _query = MutableStateFlow("")
        val query: StateFlow<String> = _query.asStateFlow()

        val matchingHistory: StateFlow<List<SearchHistoryItem>> =
            combine(searchHistory.getSearchHistoryFlow(SearchHistoryScope.MUSIC), _query) { history, typed ->
                history.recentOrMatching(typed)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        private val _moods = MutableStateFlow<List<MoodAndGenres>>(emptyList())
        val moods: StateFlow<List<MoodAndGenres>> = _moods.asStateFlow()

        private val _uiState = MutableStateFlow(MusicSearchUiState())

        // One result request at a time: an answer for an older query or chip must not land over a newer one.
        private var resultsJob: Job? = null
        val uiState: StateFlow<MusicSearchUiState> =
            combine(_uiState, aiList.matcher) { state, ai -> state.withoutAiMusic(ai) }
                .flowOn(PerformanceDispatcher.parsing)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), _uiState.value)

        init {
            // A blank query still passes through so it cancels the request out for the text before it.
            viewModelScope.launch(PerformanceDispatcher.networkIO) {
                _query.collectLatestSuggestions { q ->
                    if (q.isNotBlank() && searchHistory.isSearchSuggestionsEnabled()) fetchSuggestions(q)
                }
            }

            viewModelScope.launch(PerformanceDispatcher.networkIO) {
                _moods.value = InnertubeMusicService.fetchMoodAndGenres()
            }

            viewModelScope.launch {
                downloadManager.downloadedTracks.collect { tracks ->
                    _uiState.update { state ->
                        state.copy(downloadedTrackIds = tracks.map { it.track.videoId }.toSet())
                    }
                }
            }
        }

        fun onQueryChange(newQuery: String) {
            _query.value = newQuery
            if (newQuery.isBlank()) {
                _uiState.update { it.copy(suggestions = emptyList(), recommendedItems = emptyList()) }
            }
        }

        // Runs inside collectLatest: a newer query cancels it, and a cancelled request never writes.
        private suspend fun fetchSuggestions(q: String) {
            val result = withTimeoutOrNull(SUGGESTION_TIMEOUT_MS) { YouTube.searchSuggestions(q) }
            currentCoroutineContext().ensureActive()
            result
                ?.onSuccess { suggestions ->
                    _uiState.update {
                        it.copy(
                            suggestions =
                                suggestions.queries.distinctByNonBlankKey { query ->
                                    query.trim().lowercase()
                                },
                            recommendedItems = suggestions.recommendedItems.distinctByStableIdentity(),
                        )
                    }
                }?.onFailure { throwable ->
                    android.util.Log.w("MusicSearchViewModel", "Suggestions failed: ${throwable.message}")
                }
        }

        /**
         *  PERFORMANCE OPTIMIZED: Perform search with timeout protection
         */
        fun performSearch(
            q: String = _query.value,
            type: SearchType = SearchType.TEXT,
        ) {
            if (q.isBlank()) return

            _query.value = q
            viewModelScope.launch {
                if (privacyGate.allowsSearchHistory()) {
                    searchHistory.saveSearchQuery(q.trim(), type, SearchHistoryScope.MUSIC)
                }
            }
            _uiState.update { it.copy(isLoading = true, isSearching = true, activeFilter = null, error = null, isMoreLoading = false) }
            resultsJob?.cancel()
            resultsJob = viewModelScope.launch(PerformanceDispatcher.networkIO) { loadSummary(q) }
        }

        private suspend fun loadSummary(q: String) {
            val result =
                withTimeoutOrNull(15_000L) {
                    YouTube.searchSummary(q)
                }

            result
                ?.onSuccess { summaryPage ->
                    _uiState.update { state ->
                        state.copy(
                            searchSummary = summaryPage.distinctItemsForLazyKeys(),
                            isLoading = false,
                            isSearching = true,
                            continuation = summaryPage.continuation,
                        )
                    }
                }?.onFailure { throwable ->
                    android.util.Log.w("MusicSearchViewModel", "Search failed", throwable)
                    _uiState.update { state -> state.copy(isLoading = false, error = context.getString(R.string.search_failed)) }
                } ?: run {
                _uiState.update { state -> state.copy(isLoading = false, error = context.getString(R.string.error_search_timed_out)) }
            }
        }

        /**
         *  PERFORMANCE OPTIMIZED: Apply filter with timeout protection
         */
        fun applyFilter(filter: SearchFilter?) {
            val q = _query.value
            if (q.isBlank()) return

            if (filter == null) {
                performSearch(q)
                return
            }
            _uiState.update { state ->
                state.copy(isLoading = true, isSearching = true, activeFilter = filter, error = null, isMoreLoading = false)
            }
            resultsJob?.cancel()
            resultsJob = viewModelScope.launch(PerformanceDispatcher.networkIO) { loadFiltered(q, filter) }
        }

        private suspend fun loadFiltered(
            q: String,
            filter: SearchFilter,
        ) {
            val result =
                withTimeoutOrNull(12_000L) {
                    YouTube.search(q, filter)
                }

            result
                ?.onSuccess { searchResult ->
                    _uiState.update { state ->
                        state.copy(
                            filteredResults = searchResult.items.distinctByStableIdentity(),
                            isLoading = false,
                            continuation = searchResult.continuation,
                        )
                    }
                }?.onFailure { throwable ->
                    android.util.Log.w("MusicSearchViewModel", "Filtered search failed", throwable)
                    _uiState.update { state -> state.copy(isLoading = false, error = context.getString(R.string.search_failed)) }
                } ?: run {
                _uiState.update { state ->
                    state.copy(isLoading = false, error = context.getString(R.string.error_filter_search_timed_out))
                }
            }
        }

        fun deleteHistoryItem(item: SearchHistoryItem) {
            viewModelScope.launch { searchHistory.deleteSearchItem(item.id, SearchHistoryScope.MUSIC) }
        }

        fun clearHistory() {
            viewModelScope.launch { searchHistory.clearSearchHistory(SearchHistoryScope.MUSIC) }
        }

        fun clearSearch() {
            resultsJob?.cancel()
            _query.value = ""
            _uiState.value = MusicSearchUiState()
        }

        /**
         *  PERFORMANCE OPTIMIZED: Get artist tracks with timeout
         */
        fun getArtistTracks(
            artistId: String,
            callback: (List<YTItem>) -> Unit,
        ) {
            viewModelScope.launch(PerformanceDispatcher.networkIO) {
                val result =
                    withTimeoutOrNull(10_000L) {
                        YouTube.artist(artistId)
                    }

                result?.onSuccess { artistPage ->
                    val songsSection = artistPage.sections.find { it.kind == ArtistSectionKind.TOP_SONGS }
                    val items = songsSection?.items ?: artistPage.sections.firstOrNull()?.items ?: emptyList()
                    withContext(Dispatchers.Main) {
                        callback(items.distinctByStableIdentity())
                    }
                }
            }
        }

        fun loadMore() {
            val token = _uiState.value.continuation ?: return
            if (_uiState.value.isMoreLoading || _uiState.value.isLoading) return

            resultsJob =
                viewModelScope.launch(PerformanceDispatcher.networkIO) {
                    _uiState.update { it.copy(isMoreLoading = true) }
                    val result = YouTube.searchContinuation(token)

                    result
                        .onSuccess { searchResult ->
                            _uiState.update { state ->
                                if (state.activeFilter == null) {
                                    val newSummary =
                                        com.voxtube.app.innertube.pages.SearchSummary(
                                            title = context.getString(R.string.fallback_more_results),
                                            items = searchResult.items.distinctByStableIdentity(),
                                        )
                                    state.copy(
                                        searchSummary =
                                            state.searchSummary
                                                ?.copy(
                                                    summaries = state.searchSummary.summaries + newSummary,
                                                )?.distinctItemsForLazyKeys(),
                                        continuation = searchResult.continuation,
                                        isMoreLoading = false,
                                    )
                                } else {
                                    state.copy(
                                        filteredResults =
                                            (
                                                state.filteredResults + searchResult.items
                                            ).distinctByStableIdentity(),
                                        continuation = searchResult.continuation,
                                        isMoreLoading = false,
                                    )
                                }
                            }
                        }.onFailure {
                            _uiState.update { it.copy(isMoreLoading = false) }
                        }
                }
        }
    }

data class MusicSearchUiState(
    val suggestions: List<String> = emptyList(),
    val recommendedItems: List<YTItem> = emptyList(),
    val searchSummary: SearchSummaryPage? = null,
    val filteredResults: List<YTItem> = emptyList(),
    val activeFilter: SearchFilter? = null,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null,
    val continuation: String? = null,
    val isMoreLoading: Boolean = false,
    val downloadedTrackIds: Set<String> = emptySet(),
)
