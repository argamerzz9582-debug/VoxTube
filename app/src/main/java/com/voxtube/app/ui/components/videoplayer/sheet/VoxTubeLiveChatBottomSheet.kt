package com.voxtube.app.ui.components.videoplayer.sheet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voxtube.app.R
import com.voxtube.app.data.model.LiveChatMessage
import com.voxtube.app.ui.components.shared.VoxTubeBottomSheet
import com.voxtube.app.ui.components.shared.VoxTubeSheetHeader
import com.voxtube.app.ui.components.shared.defaultSheetExpandedHeight
import com.voxtube.app.ui.components.shared.rememberFlowBottomSheetState

// Draggable live-chat bottom sheet (portrait)
@Composable
fun VoxTubeLiveChatBottomSheet(
    messages: List<LiveChatMessage>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    expandedHeight: Dp? = null,
    collapsedHeight: Dp = 0.dp,
    onSheetProgressChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    isReplay: Boolean = false,
) {
    val sheetState = rememberFlowBottomSheetState()
    VoxTubeBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        state = sheetState,
        expandedHeight = expandedHeight ?: defaultSheetExpandedHeight(),
        collapsedHeight = collapsedHeight,
        dismissOnOutsideTap = false,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        onProgressChange = onSheetProgressChange,
        header = { dragModifier ->
            VoxTubeSheetHeader(
                title = stringResource(if (isReplay) R.string.chat_replay else R.string.live_chat),
                onClose = { sheetState.dismiss() },
                modifier = dragModifier,
            )
        },
    ) {
        LiveChatList(
            messages = messages,
            isLoading = isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
        )
    }
}
