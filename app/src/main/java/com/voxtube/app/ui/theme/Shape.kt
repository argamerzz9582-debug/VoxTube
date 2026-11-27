package com.voxtube.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * VoxTube's shape scale: the larger, softer corners of the Liquid Glass design language. Every
 * component that reads `MaterialTheme.shapes` picks these up; capsules use `RoundedCornerShape(50)`.
 *
 * All steps are [RoundedCornerShape]s on purpose — the glass lens shader needs real corner radii.
 */
val VoxTubeShapes =
    Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(26.dp),
        extraLarge = RoundedCornerShape(34.dp),
    )

/** Fully rounded ends, for tab bars, pills and floating buttons. */
val CapsuleShape = RoundedCornerShape(percent = 50)
