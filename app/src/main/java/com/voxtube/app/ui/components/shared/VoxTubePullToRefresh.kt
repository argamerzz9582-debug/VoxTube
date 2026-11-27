package com.voxtube.app.ui.components.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

/**
 * Every pull-to-refresh surface in the app, on the M3 Expressive loading indicator, or on the circular
 * one while [LocalClassicLoadingIndicator] is on. A remote can't pull, so on a D-pad a refresh shows as
 * a thin bar along the top edge instead. [indicatorTopPadding] keeps the indicator below a header laid
 * over the content.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VoxTubePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopStart,
    indicatorTopPadding: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    if (LocalDpadNavigation.current) {
        Box(modifier = modifier, contentAlignment = contentAlignment) {
            content()
            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.padding(top = indicatorTopPadding).fillMaxWidth().align(Alignment.TopCenter))
            }
        }
        return
    }
    // A tick under the finger the moment a pull goes far enough to refresh, so letting go is a sure thing.
    // Only while idle: a refresh already running holds the indicator at the threshold.
    if (!isRefreshing) {
        val haptics = LocalHapticFeedback.current
        LaunchedEffect(state) {
            snapshotFlow { state.distanceFraction >= 1f }
                .drop(1)
                .distinctUntilChanged()
                .filter { it }
                .collect { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) }
        }
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        contentAlignment = contentAlignment,
        indicator = {
            if (LocalClassicLoadingIndicator.current) {
                PullToRefreshDefaults.Indicator(
                    state = state,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = indicatorTopPadding),
                )
            } else {
                PullToRefreshDefaults.LoadingIndicator(
                    state = state,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = indicatorTopPadding),
                )
            }
        },
        content = content,
    )
}
