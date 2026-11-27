package com.voxtube.app.ui.screens.shorts

import com.voxtube.app.data.local.PlayerPreferences
import com.voxtube.app.data.model.ShortLinkedVideo
import com.voxtube.app.data.model.ShortVideo
import com.voxtube.app.data.model.toVideo
import com.voxtube.app.data.repository.YouTubeRepository
import com.voxtube.app.data.shorts.ShortDetails
import com.voxtube.app.data.shorts.ShortsFeedRepository
import com.voxtube.app.data.shorts.ShortsMetadataRepository
import com.voxtube.app.data.shorts.queue.ShortsQueueChange
import com.voxtube.app.data.shorts.queue.ShortsQueueController
import com.voxtube.app.innertube.pages.VideoDescriptionPage
import com.voxtube.app.innertube.pages.reel.ReelOverlay
import com.voxtube.app.utils.OriginalTextPolicy
import com.voxtube.app.utils.PerformanceDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fills in a reel the queue only knows by id or by a feed card: its `/player` details, the overlay's
 * counts and sound, and the description the sheet reads. Every result goes back through the queue,
 * which [publishQueue] mirrors to the screen.
 */
internal class ShortsReelEnricher(
    private val scope: CoroutineScope,
    private val repository: YouTubeRepository,
    private val feed: ShortsFeedRepository,
    private val metadata: ShortsMetadataRepository,
    private val playerPreferences: PlayerPreferences,
    private val shorts: () -> List<ShortVideo>,
    private val queue: () -> ShortsQueueController?,
    private val publishQueue: () -> Unit,
) {
    private val _descriptionPages = MutableStateFlow<Map<String, VideoDescriptionPage>>(emptyMap())

    /** The description panel of the reel whose sheet was opened last, by reel id. */
    val descriptionPages: StateFlow<Map<String, VideoDescriptionPage>> = _descriptionPages.asStateFlow()

    /** Counts, avatar, timestamp and sound for the reel on screen — one overlay request, cached. */
    fun loadDetails(videoId: String) {
        scope.launch(PerformanceDispatcher.networkIO) {
            val overlay = runCatching { metadata.overlayFor(videoId) }.getOrNull() ?: return@launch
            val preferOriginal = playerPreferences.originalTitles.first()
            enrich(videoId) { it.withOverlay(overlay, preferOriginal) }
        }
    }

    /**
     * The description sheet reads the reel's own description panel: body, absolute date and exact
     * counts, off the overlay response already cached. The watch page is the fallback for a reel
     * whose response has no panel.
     */
    fun loadDescription(videoId: String) {
        scope.launch(PerformanceDispatcher.networkIO) {
            val page = runCatching { metadata.descriptionFor(videoId) }.getOrNull()
            if (page != null) {
                _descriptionPages.value = mapOf(videoId to page)
                enrich(videoId) { it.withDescription(page) }
                return@launch
            }
            val existing = shorts().firstOrNull { it.id == videoId } ?: return@launch
            if (existing.description.isNotBlank()) return@launch
            val enriched = runCatching { repository.enrichFromWatchMetadata(existing.toVideo()) }.getOrNull() ?: return@launch
            enrich(videoId) { short ->
                short.copy(
                    description = enriched.description.ifBlank { short.description },
                    uploadDate = enriched.uploadDate.ifBlank { short.uploadDate },
                    likeCount = if (enriched.likeCount > 0) enriched.likeCount else short.likeCount,
                    viewCount = if (enriched.viewCount > 0) enriched.viewCount else short.viewCount,
                    channelThumbnailUrl = enriched.channelThumbnailUrl.ifBlank { short.channelThumbnailUrl },
                )
            }
        }
    }

    /**
     * A sequence reel is an id until `/player` names it, so blocked channels and topics are
     * judged here as well as at page assembly; a reel that fails leaves the queue instead.
     */
    suspend fun applyDetails(
        videoId: String,
        details: ShortDetails,
    ) {
        val dropped =
            feed.isBlocked(details.channelId, details.title, details.channelName) &&
                (queue()?.dropFiltered(videoId) ?: ShortsQueueChange.None) != ShortsQueueChange.None
        if (dropped) {
            publishQueue()
            return
        }
        val preferOriginal = playerPreferences.originalTitles.first()
        enrich(videoId) { short ->
            short.copy(
                title = OriginalTextPolicy.streamTitle(details.title, short.title, preferOriginal).orEmpty(),
                channelName = details.channelName.ifBlank { short.channelName },
                channelId = details.channelId.ifBlank { short.channelId },
                viewCount = details.viewCount ?: short.viewCount,
                durationMs = details.durationMs ?: short.durationMs,
            )
        }
    }

    private fun enrich(
        videoId: String,
        transform: (ShortVideo) -> ShortVideo,
    ) {
        val existing = shorts().firstOrNull { it.id == videoId } ?: return
        val enriched = transform(existing)
        if (enriched == existing) return
        if (queue()?.applyEnrichment(listOf(enriched)) != ShortsQueueChange.None) publishQueue()
    }
}

internal fun ShortVideo.withDescription(page: VideoDescriptionPage): ShortVideo =
    copy(
        description = page.description?.text ?: description,
        uploadDate = page.publishedDateText ?: uploadDate,
        viewCount = page.viewCount ?: viewCount,
    )

/** The overlay is translated like any list, `/player` is not, and the switch picks one (#1230). */
internal fun ShortVideo.withOverlay(
    overlay: ReelOverlay,
    preferOriginal: Boolean,
): ShortVideo =
    copy(
        title = OriginalTextPolicy.streamTitle(title, overlay.title, preferOriginal).orEmpty(),
        channelName = channelName.ifBlank { overlay.channelName.orEmpty() },
        channelId = channelId.ifBlank { overlay.channelId.orEmpty() },
        channelThumbnailUrl = overlay.channelAvatarUrl ?: channelThumbnailUrl,
        likeCount = overlay.likeCount ?: likeCount,
        commentCount = overlay.commentCount ?: commentCount,
        uploadDate = uploadDate.ifBlank { overlay.relativeTimestamp.orEmpty() },
        soundTitle = overlay.soundTitle ?: soundTitle,
        soundThumbnailUrl = overlay.soundThumbnailUrl ?: soundThumbnailUrl,
        linkedVideo =
            overlay.companionVideoId?.let { ShortLinkedVideo(it, overlay.companionVideoTitle) } ?: linkedVideo,
    )
