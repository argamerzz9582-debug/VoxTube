package com.voxtube.app.ui.screens.music

import androidx.lifecycle.ViewModelStore
import com.voxtube.app.data.contentfilter.aislist.AiListMatcher
import com.voxtube.app.data.contentfilter.aislist.AiListRepository
import com.voxtube.app.data.local.PlayerPreferences
import com.voxtube.app.data.local.PrivacyGate
import com.voxtube.app.data.local.SearchHistoryRepository
import com.voxtube.app.data.local.SearchHistoryScope
import com.voxtube.app.data.local.SearchType
import com.voxtube.app.data.music.DownloadManager
import com.voxtube.app.data.newmusic.InnertubeMusicService
import com.voxtube.app.innertube.YouTube
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MusicSearchHistoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private val history: SearchHistoryRepository = mockk(relaxed = true)
    private var deepFlow = false

    /**
     * The ViewModel loads on the app's own pools, so its work can outlive the test. Clearing it
     * before unmocking cancels that work instead of letting it reach the real services and fail
     * in whichever runTest comes next.
     */
    private val viewModels = ViewModelStore()

    private fun viewModel() =
        MusicSearchViewModel(
            mockk<DownloadManager>(relaxed = true).also { every { it.downloadedTracks } returns flowOf(emptyList()) },
            history,
            PrivacyGate(
                deepFlowActive = { deepFlow },
                watchHistoryPaused = { false },
                scrobbleDuringDeepFlow = { false },
            ),
            mockk(relaxed = true),
            mockk<AiListRepository>().also { every { it.matcher } returns MutableStateFlow(AiListMatcher.NONE) },
            mockk<PlayerPreferences>().also { every { it.deepFlowActive } returns flowOf(false) },
        ).also { viewModels.put("music_search_${viewModels.keys().size}", it) }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { history.getSearchHistoryFlow(any()) } returns flowOf(emptyList())
        mockkObject(InnertubeMusicService, YouTube)
        coEvery { InnertubeMusicService.fetchMoodAndGenres() } returns emptyList()
        coEvery { YouTube.searchSummary(any()) } returns Result.failure(IllegalStateException("offline"))
    }

    @After
    fun tearDown() {
        viewModels.clear()
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `a music search is saved while Deep Flow is off`() =
        runTest(testDispatcher) {
            viewModel().performSearch("  raga  ", SearchType.TEXT)
            testDispatcher.scheduler.runCurrent()

            coVerify(exactly = 1) { history.saveSearchQuery("raga", SearchType.TEXT, SearchHistoryScope.MUSIC) }
        }

    @Test
    fun `Deep Flow keeps a music search out of history`() =
        runTest(testDispatcher) {
            deepFlow = true
            viewModel().performSearch("raga", SearchType.TEXT)
            testDispatcher.scheduler.runCurrent()

            coVerify(exactly = 0) { history.saveSearchQuery(any(), any(), any(), any()) }
        }
}
