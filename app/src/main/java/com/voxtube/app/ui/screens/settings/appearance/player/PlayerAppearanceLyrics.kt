package com.voxtube.app.ui.screens.settings.appearance.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.voxtube.app.R
import com.voxtube.app.data.local.LyricsAnimationStyle
import com.voxtube.app.data.local.LyricsBackgroundStyle
import com.voxtube.app.data.local.LyricsGlow
import com.voxtube.app.ui.components.musicplayer.lyrics.LyricsAlignments
import com.voxtube.app.ui.components.musicplayer.lyrics.lyricsAlignmentLabel
import com.voxtube.app.ui.components.musicplayer.lyrics.lyricsAnimationLabel
import com.voxtube.app.ui.components.musicplayer.lyrics.lyricsBackgroundLabel
import com.voxtube.app.ui.components.musicplayer.lyrics.lyricsGlowLabel
import com.voxtube.app.ui.components.settings.SettingsListScope
import com.voxtube.app.ui.components.settings.switch
import com.voxtube.app.ui.components.settings.toggleGroup
import com.voxtube.app.ui.components.shared.VoxTubeToggleOption
import com.voxtube.app.ui.screens.settings.index.PlayerAppearanceIndex

/** The choices the lyrics group offers, resolved once per screen. */
@Immutable
internal class LyricsLookOptions(
    val animation: List<VoxTubeToggleOption<LyricsAnimationStyle>>,
    val glow: List<VoxTubeToggleOption<LyricsGlow>>,
    val background: List<VoxTubeToggleOption<LyricsBackgroundStyle>>,
    val alignment: List<VoxTubeToggleOption<String>>,
)

@Composable
internal fun rememberLyricsLookOptions(): LyricsLookOptions =
    LyricsLookOptions(
        animation = LyricsAnimationStyle.entries.map { VoxTubeToggleOption(it, stringResource(lyricsAnimationLabel(it))) },
        glow = LyricsGlow.entries.map { VoxTubeToggleOption(it, stringResource(lyricsGlowLabel(it))) },
        background = LyricsBackgroundStyle.entries.map { VoxTubeToggleOption(it, stringResource(lyricsBackgroundLabel(it))) },
        alignment = LyricsAlignments.map { VoxTubeToggleOption(it, stringResource(lyricsAlignmentLabel(it))) },
    )

/** How synced lyrics look and move, beside the music player's other looks. */
internal fun SettingsListScope.lyricsLookGroup(
    viewModel: PlayerAppearanceViewModel,
    options: LyricsLookOptions,
) = group(key = "player_appearance.lyrics", header = R.string.settings_section_lyrics) {
    toggleGroup(
        PlayerAppearanceIndex.lyricsAnimation,
        options.animation,
        viewModel.lyricsAnimationStyle,
        viewModel::setLyricsAnimationStyle,
    )
    toggleGroup(PlayerAppearanceIndex.lyricsGlow, options.glow, viewModel.lyricsGlow, viewModel::setLyricsGlow)
    switch(PlayerAppearanceIndex.lyricsBlurOtherLines, viewModel.lyricsBlurOtherLines, viewModel::setLyricsBlurOtherLines)
    toggleGroup(PlayerAppearanceIndex.lyricsBackground, options.background, viewModel.lyricsBackground, viewModel::setLyricsBackground)
    toggleGroup(PlayerAppearanceIndex.lyricsAlignment, options.alignment, viewModel.lyricsTextAlign, viewModel::setLyricsTextAlign)
}
