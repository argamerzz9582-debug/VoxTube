package com.voxtube.app.ui.components.videoplayer.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.voxtube.app.ui.theme.PlayerScrim
import com.voxtube.app.ui.theme.PlayerScrimAffordance
import com.voxtube.app.ui.theme.PlayerScrimContent
import com.voxtube.app.ui.theme.glass.LocalGlassAccessibility

/** The smoked fill of glass on video: a touch lighter than the old flat scrim, since the rim now carries the edge. */
private val PlayerGlassFill = PlayerScrim.copy(alpha = 0.3f)
private val PlayerGlassContrastFill = PlayerScrim.copy(alpha = 0.62f)
private const val SHEEN_TOP_ALPHA = 0.16f
private const val SHEEN_MID_ALPHA = 0.04f
private const val RIM_TOP_ALPHA = 0.44f
private const val RIM_BOTTOM_ALPHA = 0.09f
private val RimWidth = 1.5.dp

/**
 * Liquid Glass for controls resting on video, after iOS 26's player: a smoked fill, a soft specular
 * sheen across the top and a rim that is bright where light falls on it and fades toward the bottom.
 *
 * The video is a SurfaceView the backdrop library cannot record, so this glass is drawn rather than
 * refracted; it costs one cached gradient per control. With the system contrast raised the fill
 * thickens so white glyphs hold their contrast over a bright frame.
 */
@Composable
internal fun playerGlass(shape: Shape = CircleShape): Modifier {
    val contrast = LocalGlassAccessibility.current.increaseContrast
    val fill = if (contrast) PlayerGlassContrastFill else PlayerGlassFill
    return Modifier
        .clip(shape)
        .background(fill, shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val sheen =
                Brush.verticalGradient(
                    0f to PlayerScrimContent.copy(alpha = SHEEN_TOP_ALPHA),
                    0.5f to PlayerScrimContent.copy(alpha = SHEEN_MID_ALPHA),
                    1f to Color.Transparent,
                )
            val rim =
                Brush.verticalGradient(
                    listOf(PlayerScrimContent.copy(alpha = RIM_TOP_ALPHA), PlayerScrimContent.copy(alpha = RIM_BOTTOM_ALPHA)),
                )
            val stroke = Stroke(width = RimWidth.toPx())
            onDrawWithContent {
                drawOutline(outline, sheen)
                drawContent()
                drawOutline(outline, rim, style = stroke)
            }
        }
}

/** Whether a control asked for the standard on-video container, which is now drawn as [playerGlass]. */
internal fun Color.isPlayerAffordance(): Boolean = this == PlayerScrimAffordance
