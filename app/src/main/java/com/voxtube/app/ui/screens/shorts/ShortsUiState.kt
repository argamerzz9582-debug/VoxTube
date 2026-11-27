package com.voxtube.app.ui.screens.shorts

import com.voxtube.app.data.model.ShortVideo

data class ShortsUiState(
    val shorts: List<ShortVideo> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)
