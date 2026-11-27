package com.voxtube.app.di

import com.voxtube.app.data.localmedia.LocalMediaIds
import com.voxtube.app.data.localmedia.LocalSubtitles
import com.voxtube.app.data.recommendation.VoxTubeNeuroEngine
import com.voxtube.app.data.video.OfflineSubtitleStore
import com.voxtube.app.data.video.VideoDownloadManager
import com.voxtube.app.player.EnhancedPlayerManager
import com.voxtube.app.player.FeedExclusionsSource
import com.voxtube.app.player.LocalCaptionSource
import com.voxtube.app.player.LocalCaptions
import com.voxtube.app.player.LocalCopySource
import com.voxtube.app.player.WatchedSuggestions
import com.voxtube.app.player.WatchedSuggestionsSource
import com.voxtube.app.player.shorts.ShortsPlayerPool
import com.voxtube.app.utils.PerformanceDispatcher
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NetworkIoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Transitional providers around the player-path singletons so consumers can inject them instead of
 * reaching for the static accessors. Every provider delegates to the existing singleton, so object
 * identity and first-creation timing are unchanged (see CLAUDE.md, DI rule 10).
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerManagerModule {
    @Provides
    fun provideEnhancedPlayerManager(
        videoDownloadManager: VideoDownloadManager,
        neuroEngine: Lazy<VoxTubeNeuroEngine>,
        localSubtitles: Lazy<LocalSubtitles>,
        offlineSubtitleStore: Lazy<OfflineSubtitleStore>,
        watchedSuggestions: Lazy<WatchedSuggestions>,
    ): EnhancedPlayerManager =
        EnhancedPlayerManager.getInstance().also {
            it.localCopySource =
                LocalCopySource { videoId ->
                    LocalMediaIds.videoUri(videoId)?.toString() ?: videoDownloadManager.localCopyPath(videoId)
                }
            it.localCaptionSource =
                LocalCaptionSource { videoId, path ->
                    val subtitles = localSubtitles.get()
                    val own =
                        if (LocalMediaIds.isLocal(videoId)) subtitles.beside(path) else offlineSubtitleStore.get().load(videoId)
                    LocalCaptions(own + subtitles.picked(videoId), subtitles.offsetMs(videoId))
                }
            it.feedExclusionsSource = FeedExclusionsSource { neuroEngine.get().feedExclusions() }
            it.watchedSuggestionsSource = WatchedSuggestionsSource { videos -> watchedSuggestions.get().watchedAmong(videos) }
        }

    @Provides
    fun provideShortsPlayerPool(): ShortsPlayerPool = ShortsPlayerPool.getInstance()

    @Provides
    @NetworkIoDispatcher
    fun provideNetworkIoDispatcher(): CoroutineDispatcher = PerformanceDispatcher.networkIO

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = PerformanceDispatcher.diskIO
}
