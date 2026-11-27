package com.voxtube.app.innertube.pages

import com.voxtube.app.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
