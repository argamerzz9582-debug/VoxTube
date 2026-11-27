package com.voxtube.app.ui.components.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.voxtube.app.data.video.playsSmoothlyHere
import com.voxtube.app.innertube.models.response.PlayerResponse.StreamingData.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Which of the download sheet's video formats play smoothly here, by itag; null until the decoders answer. */
@Composable
internal fun rememberSmoothItags(formats: List<Format>): State<Set<Int>?> =
    produceState<Set<Int>?>(null, formats) {
        value = withContext(Dispatchers.Default) { formats.filter { it.playsSmoothlyHere() }.mapTo(HashSet()) { it.itag } }
    }

/** True until the answer is in, so nothing is marked before the decoders were asked. */
internal fun Set<Int>?.playsSmoothly(format: Format?): Boolean = this == null || format == null || format.itag in this
