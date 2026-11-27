package com.voxtube.app.innertube.pages

import com.voxtube.app.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
