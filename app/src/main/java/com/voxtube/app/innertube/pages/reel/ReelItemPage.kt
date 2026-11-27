package com.voxtube.app.innertube.pages.reel

import com.voxtube.app.innertube.pages.ReelCommentsPanel
import com.voxtube.app.innertube.pages.VideoDescriptionPage
import com.voxtube.app.innertube.pages.reelCommentsPanel
import com.voxtube.app.innertube.pages.toReelDescriptionPage
import kotlinx.serialization.json.JsonObject

/** What one WEB `reel_item_watch` response holds for a reel: its overlay, comment panel and description. */
data class ReelItem(
    val overlay: ReelOverlay? = null,
    val comments: ReelCommentsPanel? = null,
    val description: VideoDescriptionPage? = null,
)

/** Null when the response holds nothing readable, so a failed parse is not remembered as an answer. */
fun JsonObject.toReelItem(videoId: String): ReelItem? {
    val item =
        ReelItem(
            overlay = toReelOverlay(),
            comments = reelCommentsPanel(),
            description = toReelDescriptionPage(videoId),
        )
    return item.takeIf { it.overlay != null || it.comments != null || it.description != null }
}
