package com.voxtube.app.innertube.models.body

import com.voxtube.app.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class VisitorIdBody(
    val context: Context,
)
