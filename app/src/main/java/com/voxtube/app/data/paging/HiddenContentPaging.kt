package com.voxtube.app.data.paging

import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.flatMap
import com.voxtube.app.data.engagement.HiddenContent
import com.voxtube.app.innertube.pages.renderer.FeedItem
import com.voxtube.app.innertube.pages.renderer.videoOrNull

/** Loaded search results without what the viewer hid since, by the same rules the load applied. */
internal fun PagingData<SearchResultItem>.withoutHidden(hidden: HiddenContent): PagingData<SearchResultItem> {
    if (hidden.isEmpty) return this
    val exclusions = hidden.asExclusions()
    return flatMap { item -> listOf(item).withoutHidden(exclusions) }
}

/**
 * A channel tab's loaded items without the videos marked "Not interested" since. A block of the
 * channel itself leaves its own page alone: the viewer is looking at it on purpose.
 */
internal fun PagingData<FeedItem>.withoutHiddenVideos(hidden: HiddenContent): PagingData<FeedItem> {
    if (hidden.videoIds.isEmpty()) return this
    return filter { item -> !item.isHiddenBy(hidden) }
}

internal fun FeedItem.isHiddenBy(hidden: HiddenContent): Boolean = videoOrNull()?.let { hidden.hidesVideo(it.id) } ?: false
