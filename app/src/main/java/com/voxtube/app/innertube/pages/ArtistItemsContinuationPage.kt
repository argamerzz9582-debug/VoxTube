package com.voxtube.app.innertube.pages

import com.voxtube.app.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
