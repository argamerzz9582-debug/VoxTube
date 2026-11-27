/*
 * The lens recipe (a combined page and tab-row backdrop, a capsule lens with chromatic aberration,
 * an accent-tinted copy of the row, and a press that swells the lens) follows the
 * `LiquidBottomTabs` sample of Kyant0/AndroidLiquidGlass, Copyright 2025 Kyant, Apache License 2.0.
 */
package com.voxtube.app.ui.components.layout.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.voxtube.app.ui.theme.glass.GlassShapes
import com.voxtube.app.ui.theme.glass.LiquidGlassSupport
import com.voxtube.app.ui.theme.glass.LocalGlassAccessibility
import com.voxtube.app.ui.theme.glass.LocalGlassBackdrop
import com.voxtube.app.ui.theme.glass.LocalLiquidGlassEnabled
import kotlin.math.roundToInt

/** How much the lens magnifies the icons under it at rest, and how much more while pressed. */
private const val LENS_MAGNIFY_REST = 1.06f
private const val LENS_MAGNIFY_PRESS = 0.1f

/** How far the lens swells past the row while a finger holds it, like iOS 26's tab bar. */
private const val LENS_PRESS_SCALE = 0.22f

/** Stretching between tabs flattens the lens a little, so it reads as liquid rather than a sliding pill. */
private const val LENS_SQUASH = 0.22f
private const val LENS_SQUASH_MAX = 0.14f

private val LensRefractionHeightRest = 8.dp
private val LensRefractionAmountRest = 12.dp
private val LensRefractionHeightPress = 14.dp
private val LensRefractionAmountPress = 22.dp

/**
 * What the floating bar needs to draw its selection as a Liquid Glass lens: the recording of the tab
 * row, tinted with the accent so whatever slides under the lens lights up, and the page backdrop it is
 * combined with. Null when the lens cannot or should not be drawn (no backdrop, glass off, below
 * Android 13, Reduce Motion or Increase Contrast); the bar then keeps its plain soft pill.
 */
internal class FloatingNavLensState(
    val rowBackdrop: LayerBackdrop,
    val lensBackdrop: Backdrop,
)

@Composable
internal fun rememberFloatingNavLens(accent: Color): FloatingNavLensState? {
    val page = LocalGlassBackdrop.current
    val accessibility = LocalGlassAccessibility.current
    val available =
        page != null &&
            LocalLiquidGlassEnabled.current &&
            LiquidGlassSupport.hasRefraction() &&
            !accessibility.reduceMotion &&
            !accessibility.increaseContrast
    if (!available || page == null) return null
    val layer = rememberGraphicsLayer()
    // The row draws as usual; only its recording is tinted, so the icons light up inside the lens alone.
    SideEffect { layer.colorFilter = ColorFilter.tint(accent, BlendMode.SrcIn) }
    val row = rememberLayerBackdrop(layer)
    return FloatingNavLensState(row, rememberCombinedBackdrop(page, row))
}

/**
 * Places and draws the lens over the tab row. [start] and [end] are the indicator's animated edges in
 * px from the row's start; [press] is 0 at rest and 1 while a finger is on the bar; [restWidth] is the
 * width of one tab's indicator, used to measure the stretch. Reads all of them in layout and draw, so
 * motion never recomposes the bar.
 */
internal fun Modifier.floatingNavLens(
    state: FloatingNavLensState,
    start: Animatable<Float, AnimationVector1D>,
    end: Animatable<Float, AnimationVector1D>,
    press: () -> Float,
    restWidth: () -> Float,
    top: Dp,
    height: Dp?,
    surface: Color,
): Modifier =
    this
        .layout { measurable, constraints ->
            val lensHeight = height?.roundToPx() ?: constraints.maxHeight
            var from = start.value
            var to = maxOf(end.value, from + lensHeight)
            if (layoutDirection == LayoutDirection.Rtl) {
                val mirrored = constraints.maxWidth - to
                to = constraints.maxWidth - from
                from = mirrored
            }
            val width = (to - from).roundToInt().coerceAtLeast(0)
            val placeable = measurable.measure(Constraints.fixed(width, lensHeight))
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(from.roundToInt(), top.roundToPx())
            }
        }.drawBackdrop(
            backdrop = state.lensBackdrop,
            shape = { GlassShapes.Capsule },
            effects = {
                val p = press()
                vibrancy()
                lens(
                    refractionHeight = lerpDp(LensRefractionHeightRest, LensRefractionHeightPress, p).toPx(),
                    refractionAmount = lerpDp(LensRefractionAmountRest, LensRefractionAmountPress, p).toPx(),
                    chromaticAberration = true,
                )
            },
            highlight = { Highlight.Default.copy(alpha = 0.6f + 0.4f * press()) },
            shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.1f + 0.08f * press())) },
            innerShadow = { press().takeIf { it > 0.01f }?.let { InnerShadow(radius = 8.dp * it, alpha = it) } },
            layerBlock = {
                val p = press()
                val rest = restWidth()
                val stretch = if (rest > 0f) (size.width / rest - 1f).coerceAtLeast(0f) else 0f
                val squash = (stretch * LENS_SQUASH).coerceAtMost(LENS_SQUASH_MAX)
                val swell = 1f + LENS_PRESS_SCALE * p
                scaleX = swell
                scaleY = swell * (1f - squash)
                transformOrigin = TransformOrigin.Center
            },
            onDrawBackdrop = { drawBackdrop ->
                val magnify = LENS_MAGNIFY_REST + LENS_MAGNIFY_PRESS * press()
                scale(magnify) { drawBackdrop() }
            },
            onDrawSurface = { drawRect(surface.copy(alpha = surface.alpha * (1f - press()))) },
        )

private fun lerpDp(
    from: Dp,
    to: Dp,
    fraction: Float,
): Dp = from + (to - from) * fraction
