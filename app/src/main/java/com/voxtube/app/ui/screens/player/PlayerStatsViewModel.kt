package com.voxtube.app.ui.screens.player

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.media3.common.util.UnstableApi
import com.voxtube.app.data.localmedia.LocalMediaIds
import com.voxtube.app.player.EnhancedPlayerManager
import com.voxtube.app.player.stats.PlayerStatsReader
import com.voxtube.app.player.stats.PlayerStatsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Reads the player for the stats panel, which only exists while the panel is open. */
@OptIn(UnstableApi::class)
@HiltViewModel
class PlayerStatsViewModel
    @Inject
    constructor(
        private val playerManager: EnhancedPlayerManager,
    ) : ViewModel() {
        internal fun read(
            videoId: String,
            client: String?,
        ): PlayerStatsSnapshot? {
            val player = playerManager.getPlayer() ?: return null
            val kind =
                when {
                    LocalMediaIds.isLocal(videoId) -> "local"
                    player.isCurrentMediaItemLive -> "live"
                    else -> "vod"
                }
            return PlayerStatsReader.read(player, playerManager.statsReadings(), videoId, kind, client)
        }
    }
