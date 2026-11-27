package com.voxtube.app.innertube.models.body

import com.voxtube.app.innertube.models.Context
import kotlinx.serialization.Serializable

/** The first `updated_metadata` request names the video; every later one sends only the continuation. */
@Serializable
data class UpdatedMetadataBody(
    val context: Context,
    val videoId: String? = null,
    val continuation: String? = null,
)
