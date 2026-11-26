package com.voxtube.app.ui.screens.music

import com.voxtube.app.data.local.LYRICS_ALIGN_CENTER
import com.voxtube.app.data.local.LyricsAnimationStyle
import com.voxtube.app.data.local.LyricsBackgroundStyle
import com.voxtube.app.data.local.LyricsGlow
import com.voxtube.app.data.lyrics.LyricsCandidate
import com.voxtube.app.data.lyrics.LyricsEntry
import com.voxtube.app.data.music.model.MusicTrack
import com.voxtube.app.player.RepeatMode

data class MusicPlayerUiState(
    val currentTrack: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playWhenReady: Boolean = false,
    val duration: Long = 0,
    val queue: List<MusicTrack> = emptyList(),
    val autoplaySuggestions: List<MusicTrack> = emptyList(),
    val currentQueueIndex: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isLiked: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lyrics: String? = null,
    val syncedLyrics: List<LyricsEntry> = emptyList(),
    val isLyricsLoading: Boolean = false,
    val playingFrom: String = "",
    val endlessRadioEnabled: Boolean = true,
    val relatedContent: List<MusicTrack> = emptyList(),
    val isRelatedLoading: Boolean = false,
    val isRadioLoading: Boolean = false,
    val downloadedTrackIds: Set<String> = emptySet(),
    val lyricsProviderName: String = "",
    val lyricsSyncOffsetMs: Long = 0L,
    val lyricsTextAlign: String = LYRICS_ALIGN_CENTER,
    val lyricsShowTranslation: Boolean = true,
    val lyricsShowRomanization: Boolean = true,
    val lyricsAutoRomanize: Boolean = false,
    val lyricsRomanizationOnly: Boolean = false,
    val lyricsKeepScreenOn: Boolean = false,
    val lyricsBlurOtherLines: Boolean = true,
    val lyricsAnimationStyle: LyricsAnimationStyle = LyricsAnimationStyle.CADENCE,
    val lyricsGlow: LyricsGlow = LyricsGlow.NORMAL,
    val lyricsBackground: LyricsBackgroundStyle = LyricsBackgroundStyle.SOLID,
    val lyricsCandidates: List<LyricsCandidate> = emptyList(),
    val isBrowsingLyrics: Boolean = false,
) {
    /**
     * Playing, or only buffering on the way to playing (a seek buffers even inside the loaded range). Motion
     * that follows playback keys off this, so a seek doesn't stop and restart it.
     */
    val isPlaybackActive: Boolean get() = isPlaying || (playWhenReady && isBuffering)
}
