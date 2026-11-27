package com.voxtube.app.ui.components.shorts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.NotInterested
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voxtube.app.R
import com.voxtube.app.data.local.PlayerPreferences
import com.voxtube.app.data.model.ShortVideo
import com.voxtube.app.data.shorts.ShortVideoQuality
import com.voxtube.app.player.shorts.ShortsCaptionChoice
import com.voxtube.app.player.shorts.ShortsCaptionState
import com.voxtube.app.player.shorts.ShortsPlayerPool
import com.voxtube.app.player.state.SubtitleOption
import com.voxtube.app.player.stream.VideoCodecUtils
import com.voxtube.app.ui.components.shared.MediaAudioTrackRow
import com.voxtube.app.ui.components.shared.MediaPlaybackSpeedPicker
import com.voxtube.app.ui.components.shared.VoxTubeBottomSheet
import com.voxtube.app.ui.components.shared.VoxTubeNavRow
import com.voxtube.app.ui.components.shared.VoxTubeRowGroup
import com.voxtube.app.ui.components.shared.VoxTubeSectionHeader
import com.voxtube.app.ui.components.shared.VoxTubeSheetHeader
import com.voxtube.app.ui.components.shared.VoxTubeSpinner
import com.voxtube.app.ui.components.shared.VoxTubeSwitchRow
import com.voxtube.app.ui.components.shared.audioTrackBitrateLabel
import com.voxtube.app.ui.components.shared.audioTrackFallbackLabel
import com.voxtube.app.ui.components.shared.defaultSheetExpandedHeight
import com.voxtube.app.ui.components.shared.flowRowGroupShape
import com.voxtube.app.ui.components.shared.playbackSpeedLabel
import com.voxtube.app.ui.components.shared.rememberFlowBottomSheetState
import com.voxtube.app.ui.components.videoplayer.settings.PlayerSettingsQualityPage
import com.voxtube.app.ui.components.videoplayer.settings.PlayerSettingsSubtitlesPage
import com.voxtube.app.ui.components.videoplayer.subtitle.SubtitleCustomizer
import com.voxtube.app.ui.screens.shorts.ShortsViewModel
import kotlinx.coroutines.launch

private val SheetContentVerticalPadding = 12.dp
private val HeaderContentPadding = PaddingValues(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 8.dp)
private val LoadingPadding = 32.dp
private val EmptyPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
private const val HEADER_DIVIDER_ALPHA = 0.4f
private const val THIS_SHORT_ROWS = 4
private const val VIDEO_ROWS = 2

/**
 * The reel's settings: the same paged sheet the video player uses, one instance whose pages swap in
 * place behind a header with a back arrow. Hosted by the screen beside the pager.
 */
@Composable
internal fun ShortsSettingsSheet(
    short: ShortVideo,
    settings: ShortsReelSettings,
    state: ShortsSettingsSheetState,
    playerPool: ShortsPlayerPool,
    viewModel: ShortsViewModel,
    playerPreferences: PlayerPreferences,
    onWantMore: () -> Unit,
    onNotInterested: () -> Unit,
    onBlockChannel: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    expandedHeight: Dp? = null,
    onSheetProgressChange: (Float) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
) {
    val sheetState = rememberFlowBottomSheetState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val pageIndex = state.targetIndex
    val title =
        when (state.page) {
            ShortsSettingsPage.Main -> stringResource(R.string.cd_more_options)
            ShortsSettingsPage.Quality -> stringResource(R.string.shorts_quality)
            ShortsSettingsPage.Speed -> stringResource(R.string.shorts_playback_speed)
            ShortsSettingsPage.Audio -> stringResource(R.string.shorts_audio_track)
            ShortsSettingsPage.Captions -> stringResource(R.string.captions)
            ShortsSettingsPage.CaptionStyle -> stringResource(R.string.subtitle_style)
        }
    val backPage =
        when (state.page) {
            ShortsSettingsPage.Main -> null
            ShortsSettingsPage.CaptionStyle -> ShortsSettingsPage.Captions
            else -> ShortsSettingsPage.Main
        }
    val captionState by playerPool.captionState.collectAsState()
    val captions = captionState.takeIf { it.videoId == short.id } ?: ShortsCaptionState()

    fun pickCaption(option: SubtitleOption?) {
        playerPool.selectCaption(option?.url)
        scope.launch { playerPreferences.setShortsCaptions(ShortsCaptionChoice.stored(option)) }
        sheetState.dismiss()
    }

    fun withStreams(block: suspend () -> Unit) {
        if (state.isLoadingStreams) return
        state.isLoadingStreams = true
        scope.launch {
            try {
                block()
            } finally {
                state.isLoadingStreams = false
            }
        }
    }

    fun openQuality() {
        state.page = ShortsSettingsPage.Quality
        if (state.availableQualities.isNotEmpty()) return
        withStreams {
            state.availableQualities = viewModel.availableQualities(short.id)
            val activeFormat = playerPool.ownedPlayer(pageIndex, short.id)?.videoFormat
            val active =
                findActiveShortQuality(
                    qualities = state.availableQualities,
                    currentVideoUrl = playerPool.getVideoUrlForIndex(pageIndex, short.id),
                    activeVideoWidth = activeFormat?.width ?: 0,
                    activeVideoHeight = activeFormat?.height ?: 0,
                    activeCodecKey = activeFormat?.let { VideoCodecUtils.codecKeyFromMimeType(it.fullMimeType()) },
                )
            state.selectedQualityHeight = active?.heightClass ?: -1
            state.selectedQualityUrl = active?.videoUrl
        }
    }

    fun openAudio() {
        state.page = ShortsSettingsPage.Audio
        if (state.availableAudioTracks.isNotEmpty()) return
        withStreams { state.availableAudioTracks = viewModel.availableAudioTracks(short.id) }
    }

    VoxTubeBottomSheet(
        onDismiss = onDismiss,
        state = sheetState,
        expandedHeight = expandedHeight ?: defaultSheetExpandedHeight(),
        dismissOnOutsideTap = true,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        onBack = backPage?.let { page -> { state.page = page } },
        onProgressChange = onSheetProgressChange,
        header = { dragModifier ->
            VoxTubeSheetHeader(
                title = title,
                onClose = { sheetState.dismiss() },
                modifier = dragModifier,
                onBack = backPage?.let { page -> { state.page = page } },
                contentPadding = HeaderContentPadding,
                closeButtonSize = null,
                dividerAlpha = HEADER_DIVIDER_ALPHA,
            )
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = SheetContentVerticalPadding, bottom = SheetContentVerticalPadding + bottomContentPadding),
        ) {
            when (state.page) {
                ShortsSettingsPage.Main -> {
                    ShortsSettingsMainPage(
                        settings = settings,
                        onWantMore = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            sheetState.dismiss(onWantMore)
                        },
                        onNotInterested = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            sheetState.dismiss(onNotInterested)
                        },
                        onBlockChannel = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            sheetState.dismiss(onBlockChannel)
                        },
                        onDownload = { sheetState.dismiss(onDownload) },
                        onQuality = ::openQuality,
                        onSpeed = { state.page = ShortsSettingsPage.Speed },
                        onAudio = ::openAudio,
                        captionLabel = captions.selected?.label ?: stringResource(R.string.off),
                        onCaptions = { state.page = ShortsSettingsPage.Captions },
                        onAmbientModeToggle = { enabled -> scope.launch { playerPreferences.setVideoAmbientModeEnabled(enabled) } },
                    )
                }

                ShortsSettingsPage.Quality -> {
                    ShortsQualityPage(
                        state = state,
                        groupedByResolution = settings.groupedQualitySelectorEnabled,
                        onQualitySelected = { quality ->
                            playerPool.reloadWithVideoUrl(pageIndex, short.id, quality.videoUrl, quality.dashManifest)
                            state.selectedQualityHeight = quality.heightClass
                            state.selectedQualityUrl = quality.videoUrl
                            sheetState.dismiss()
                        },
                    )
                }

                ShortsSettingsPage.Speed -> {
                    MediaPlaybackSpeedPicker(
                        currentSpeed = settings.playbackSpeed,
                        sliderEnabled = settings.speedSliderEnabled,
                        customSpeedsEnabled = settings.customSpeedsEnabled,
                        customSpeedPresetsRaw = settings.customSpeedPresetsRaw,
                        onSpeedSelected = playerPool::setBasePlaybackSpeed,
                        onSliderSelectionFinished = { speed -> scope.launch { playerPreferences.setShortsPlaybackSpeed(speed) } },
                        onSpeedRowSelected = { speed ->
                            scope.launch { playerPreferences.setShortsPlaybackSpeed(speed) }
                            sheetState.dismiss()
                        },
                    )
                }

                ShortsSettingsPage.Audio -> {
                    ShortsAudioPage(
                        state = state,
                        onTrackSelected = { index ->
                            val track = state.availableAudioTracks[index]
                            playerPool.reloadWithAudioUrl(pageIndex, short.id, track.url, track.dashManifest, track.loudnessGainDb)
                            state.selectedAudioIndex = index
                            sheetState.dismiss()
                        },
                    )
                }

                ShortsSettingsPage.Captions -> {
                    ShortsCaptionsPage(
                        captions = captions,
                        onPick = ::pickCaption,
                        onStyle = { state.page = ShortsSettingsPage.CaptionStyle },
                    )
                }

                ShortsSettingsPage.CaptionStyle -> {
                    SubtitleCustomizer(
                        currentStyle = settings.subtitleStyle,
                        onStyleChange = { style -> scope.launch { playerPreferences.setSubtitleStyle(style) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortsSettingsMainPage(
    settings: ShortsReelSettings,
    onWantMore: () -> Unit,
    onNotInterested: () -> Unit,
    onBlockChannel: () -> Unit,
    onDownload: () -> Unit,
    onQuality: () -> Unit,
    onSpeed: () -> Unit,
    onAudio: () -> Unit,
    captionLabel: String,
    onCaptions: () -> Unit,
    onAmbientModeToggle: (Boolean) -> Unit,
) {
    VoxTubeSectionHeader(stringResource(R.string.shorts_settings_this_short))
    VoxTubeRowGroup {
        VoxTubeNavRow(
            leadingIcon = Icons.Rounded.ThumbUp,
            title = stringResource(R.string.action_want_more),
            shape = flowRowGroupShape(0, THIS_SHORT_ROWS),
            onClick = onWantMore,
        )
        VoxTubeNavRow(
            leadingIcon = Icons.Rounded.NotInterested,
            title = stringResource(R.string.action_not_interested),
            shape = flowRowGroupShape(1, THIS_SHORT_ROWS),
            onClick = onNotInterested,
        )
        VoxTubeNavRow(
            leadingIcon = Icons.Rounded.Block,
            title = stringResource(R.string.dont_show_channel),
            supportingText = stringResource(R.string.dont_show_channel_desc),
            shape = flowRowGroupShape(2, THIS_SHORT_ROWS),
            onClick = onBlockChannel,
        )
        VoxTubeNavRow(
            leadingIcon = Icons.Filled.Download,
            title = stringResource(R.string.download_video),
            shape = flowRowGroupShape(3, THIS_SHORT_ROWS),
            onClick = onDownload,
        )
    }

    VoxTubeSectionHeader(stringResource(R.string.video))
    VoxTubeRowGroup {
        VoxTubeNavRow(
            leadingIcon = Icons.Filled.HighQuality,
            title = stringResource(R.string.shorts_quality),
            shape = flowRowGroupShape(0, VIDEO_ROWS),
            onClick = onQuality,
        )
        VoxTubeNavRow(
            leadingIcon = Icons.Filled.ClosedCaption,
            title = stringResource(R.string.captions),
            trailingText = captionLabel,
            shape = flowRowGroupShape(1, VIDEO_ROWS),
            onClick = onCaptions,
        )
    }

    VoxTubeSectionHeader(stringResource(R.string.playback_header))
    VoxTubeRowGroup {
        VoxTubeNavRow(
            leadingIcon = Icons.Filled.Speed,
            title = stringResource(R.string.shorts_playback_speed),
            trailingText = playbackSpeedLabel(settings.playbackSpeed),
            shape = flowRowGroupShape(0, 1),
            onClick = onSpeed,
        )
    }

    VoxTubeSectionHeader(stringResource(R.string.audio_settings_title))
    VoxTubeRowGroup {
        VoxTubeNavRow(
            leadingIcon = Icons.Filled.AudioFile,
            title = stringResource(R.string.shorts_audio_track),
            shape = flowRowGroupShape(0, 1),
            onClick = onAudio,
        )
    }

    VoxTubeSectionHeader(stringResource(R.string.player_settings_display))
    VoxTubeRowGroup {
        VoxTubeSwitchRow(
            leadingIcon = ImageVector.vectorResource(R.drawable.ic_ambient_mode),
            title = stringResource(R.string.player_settings_ambient_mode),
            checked = settings.ambientModeEnabled,
            shape = flowRowGroupShape(0, 1),
            onCheckedChange = onAmbientModeToggle,
        )
    }
}

@Composable
private fun ShortsCaptionsPage(
    captions: ShortsCaptionState,
    onPick: (SubtitleOption?) -> Unit,
    onStyle: () -> Unit,
) {
    if (captions.options.isEmpty()) {
        Text(
            text = stringResource(R.string.shorts_no_captions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(EmptyPadding),
        )
    }
    PlayerSettingsSubtitlesPage(
        availableSubtitles = captions.options,
        selectedSubtitleUrl = captions.selected?.url,
        subtitlesEnabled = captions.selected != null,
        onSubtitleSelected = { index -> onPick(captions.options[index]) },
        onDisableSubtitles = { onPick(null) },
        onShowStyleCustomizer = onStyle,
    )
}

@Composable
private fun ShortsQualityPage(
    state: ShortsSettingsSheetState,
    groupedByResolution: Boolean,
    onQualitySelected: (ShortVideoQuality) -> Unit,
) {
    if (state.isLoadingStreams) {
        SheetLoading()
        return
    }
    val qualities = state.availableQualities
    val options = remember(qualities) { qualities.map(ShortVideoQuality::toQualityOption) }
    val selectedKey = state.selectedQualityUrl ?: qualities.firstOrNull { it.heightClass == state.selectedQualityHeight }?.videoUrl
    PlayerSettingsQualityPage(
        availableQualities = options,
        currentQuality = -1,
        currentQualityKey = selectedKey,
        useGroupedQualitySelector = groupedByResolution,
        onQualitySelected = { option -> qualities.firstOrNull { it.videoUrl == option.streamKey }?.let(onQualitySelected) },
    )
}

@Composable
private fun ShortsAudioPage(
    state: ShortsSettingsSheetState,
    onTrackSelected: (Int) -> Unit,
) {
    if (state.isLoadingStreams) {
        SheetLoading()
        return
    }
    val tracks = state.availableAudioTracks
    if (tracks.isEmpty()) {
        Text(
            text = stringResource(R.string.shorts_no_audio_tracks),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(EmptyPadding),
        )
        return
    }
    VoxTubeRowGroup {
        tracks.forEachIndexed { index, track ->
            MediaAudioTrackRow(
                label = track.label.ifBlank { audioTrackFallbackLabel(index) },
                supportingText = audioTrackBitrateLabel(track.bitrate),
                selected = index == state.selectedAudioIndex,
                shape = flowRowGroupShape(index, tracks.size),
                onClick = { onTrackSelected(index) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SheetLoading() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(LoadingPadding),
        contentAlignment = Alignment.Center,
    ) {
        VoxTubeSpinner()
    }
}

/** The MIME string the codec helpers expect, rebuilt from the halves Media3 keeps apart. */
private fun androidx.media3.common.Format.fullMimeType(): String =
    buildString {
        append(sampleMimeType.orEmpty())
        codecs?.takeIf { it.isNotBlank() }?.let { codecs ->
            append("; codecs=\"")
            append(codecs)
            append('"')
        }
    }
