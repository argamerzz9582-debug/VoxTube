package com.voxtube.app.ui.theme.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

private val RimWidth = 1.dp
private const val DARK_SURFACE_LUMINANCE = 0.5f

/**
 * Glass for pills and cards that sit in a page's content, like the watch page's actions and comments:
 * a faint fill, a sheen across the top and a rim lit from above, iOS 26-style. Over the watch page's
 * ambient light the fill takes on the video's colours, the way the reference glass does.
 *
 * Content glass is drawn, not refracted: pages never sample a backdrop (glass must not sample glass,
 * and the bar's recording is of the pages). With glass off (Reduce transparency, battery saver, low
 * RAM) it is the solid tonal container the page used before; with the system contrast raised it gets
 * a firm outline.
 */
@Composable
fun contentGlass(shape: Shape = GlassShapes.Capsule): Modifier {
    val scheme = MaterialTheme.colorScheme
    val accessibility = LocalGlassAccessibility.current
    if (!LocalLiquidGlassEnabled.current || accessibility.increaseContrast) {
        val solid = Modifier.clip(shape).background(scheme.surfaceContainerHigh, shape)
        return if (accessibility.increaseContrast) solid.border(1.dp, scheme.outline, shape) else solid
    }
    val dark = scheme.surface.luminance() < DARK_SURFACE_LUMINANCE
    val fill = if (dark) Color.White.copy(alpha = 0.075f) else Color.Black.copy(alpha = 0.045f)
    val sheenTop = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.55f)
    val rimTop = if (dark) Color.White.copy(alpha = 0.24f) else Color.White
    val rimBottom = if (dark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.08f)
    return Modifier
        .clip(shape)
        .background(fill, shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val sheen = Brush.verticalGradient(0f to sheenTop, 0.55f to Color.Transparent)
            val rim = Brush.verticalGradient(listOf(rimTop, rimBottom))
            val stroke = Stroke(width = RimWidth.toPx() * 2f)
            onDrawWithContent {
                drawOutline(outline, sheen)
                drawContent()
                // Twice the width, half of it clipped away: the visible rim sits inside the shape.
                drawOutline(outline, rim, style = stroke)
            }
        }
}
