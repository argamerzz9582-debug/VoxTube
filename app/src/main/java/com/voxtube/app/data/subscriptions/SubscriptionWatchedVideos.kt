package com.voxtube.app.data.subscriptions

import com.voxtube.app.data.local.AppDatabase
import com.voxtube.app.data.local.PlayerPreferences
import com.voxtube.app.data.local.WatchProgressStore
import com.voxtube.app.data.local.isWatched
import com.voxtube.app.data.shorts.finishedShortIds
import com.voxtube.app.di.DeviceDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionWatchedVideos
    @Inject
    constructor(
        private val watchProgress: WatchProgressStore,
        private val playerPreferences: PlayerPreferences,
        @DeviceDb private val database: AppDatabase,
    ) {
        val ids: Flow<Set<String>> =
            combine(
                watchProgress.videoProgress,
                playerPreferences.hideWatchedVideosFromSubscriptions,
                playerPreferences.watchedThreshold,
                playerPreferences.countImportedAsWatched,
                database.downloadDao().getVideoDownloads(),
            ) { history, hideWatched, threshold, countImported, downloads ->
                if (!hideWatched) return@combine emptySet<String>()
                val downloadedIds = downloads.mapTo(HashSet()) { it.download.videoId }
                history
                    .asSequence()
                    .filter { threshold.isWatched(it.position, it.duration, countImported) || it.videoId in downloadedIds }
                    .map { it.videoId }
                    .toHashSet()
            }.distinctUntilChanged()
                .flowOn(Dispatchers.Default)

        /** Every video with a history entry, finished or only started; their channels get no Quick access dot. */
        val startedIds: Flow<Set<String>> =
            watchProgress.videoProgress
                .map { history -> history.mapTo(HashSet()) { it.videoId } }
                .distinctUntilChanged()
                .flowOn(Dispatchers.Default)

        /** Watched reels, for the Shorts shelf and the Shorts queues; governed by the Shorts setting alone. */
        val shortIds: Flow<Set<String>> =
            combine(
                watchProgress.videoProgress,
                playerPreferences.hideWatchedShorts,
            ) { history, hideWatched ->
                if (hideWatched) history.finishedShortIds() else emptySet()
            }.distinctUntilChanged()
                .flowOn(Dispatchers.Default)
    }
