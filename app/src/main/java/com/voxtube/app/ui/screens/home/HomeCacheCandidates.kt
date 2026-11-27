package com.voxtube.app.ui.screens.home

import com.voxtube.app.data.local.CachedHomeVideo
import com.voxtube.app.data.model.Video

internal fun cacheCandidates(
    source: FeedSource,
    videos: List<Video>,
    excludedIds: Set<String> = emptySet(),
): List<CachedHomeVideo> =
    videos
        .asSequence()
        .filterNot { it.id in excludedIds }
        .distinctBy { it.id }
        .map { CachedHomeVideo(it, source.name) }
        .toList()

internal fun cacheRelatedCandidates(
    videos: List<Video>,
    metadata: Map<String, GraphCandidate>,
    excludedIds: Set<String> = emptySet(),
): List<CachedHomeVideo> =
    videos
        .asSequence()
        .filterNot { it.id in excludedIds }
        .distinctBy { it.id }
        .map { video ->
            CachedHomeVideo(
                video = video,
                source = FeedSource.RELATED.name,
                relatedSeedId = metadata[video.id]?.seedId,
            )
        }.toList()
