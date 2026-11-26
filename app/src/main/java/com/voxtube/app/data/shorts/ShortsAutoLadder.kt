package com.voxtube.app.data.shorts

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.dash.manifest.DashManifest
import com.voxtube.app.innertube.models.response.PlayerResponse
import com.voxtube.app.player.quality.AdaptiveLadder
import com.voxtube.app.player.quality.VideoDecoderSupport
import com.voxtube.app.player.resolver.AdaptiveDashManifest
import com.voxtube.app.player.stream.InnerTubeStreamBridge

/**
 * Auto quality for a reel: every decodable rung of the chosen format's codec, as one DASH source for
 * Media3's adaptive selection, the same ladder the video player builds. Null when fewer than two rungs
 * qualify or the reel has no duration, which leaves Auto on the single chosen stream.
 */
@UnstableApi
internal fun shortsAutoLadder(
    formats: List<PlayerResponse.StreamingData.Format>,
    chosen: PlayerResponse.StreamingData.Format,
    durationMs: Long?,
): DashManifest? {
    val durationSeconds = (durationMs ?: return null) / 1_000L
    if (durationSeconds <= 0L) return null
    val streams = InnerTubeStreamBridge.convertVideoFormats(formats.filter { it.url != null })
    val anchor = streams.firstOrNull { it.itagItem?.id == chosen.itag } ?: return null
    return AdaptiveDashManifest.build(AdaptiveLadder.rungs(anchor, VideoDecoderSupport.forAuto(streams)), durationSeconds)
}
