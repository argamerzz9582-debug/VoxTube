package com.voxtube.app.ui.theme.glass

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * VoxTube's Liquid Glass material: translucent surfaces that blur, saturate and bend (refract) the
 * content scrolling underneath them, with a specular rim — the look of Apple's Liquid Glass, drawn
 * with the Kyant backdrop library (`io.github.kyant0:backdrop`).
 *
 * How it fits together:
 * 1. A screen records the content that glass sits over with [glassBackdropSource] (one
 *    [LayerBackdrop] per window, from [rememberGlassBackdrop]) and provides it via [ProvideGlassBackdrop].
 * 2. Floating chrome (tab bar, top bar, mini player, sheets) draws itself with [liquidGlass] or
 *    [GlassSurface]. Glass must never be inside the node it samples, or it would sample itself.
 *
 * Capability ladder (see [LiquidGlassSupport]):
 * - Android 13+: blur + vibrancy + lens refraction + highlight — the full effect.
 * - Android 12: blur + vibrancy + highlight, no refraction.
 * - Android 8–11: no backdrop effects exist, so glass falls back to a near-opaque tinted surface
 *   with a hairline rim. Text contrast never depends on the blur being there.
 */
object LiquidGlassSupport {
    /** `RenderEffect` blur of what is behind the glass. */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun hasBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** `RuntimeShader` lens refraction and the shader-based specular highlight. */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun hasRefraction(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

/**
 * The physical recipe of one kind of glass. Values are in dp so they scale with density.
 *
 * @param blurRadius frost applied to the backdrop; small for clear glass, large for sheets that hold text.
 * @param refractionHeight how far in from the edge the lens bends light.
 * @param refractionAmount how strongly the edge bends light.
 * @param tintAlpha opacity of the surface tint laid over the processed backdrop.
 * @param fallbackAlpha opacity of the plain surface used when the device cannot draw backdrop effects.
 */
@Immutable
data class LiquidGlassStyle(
    val blurRadius: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val tintAlpha: Float,
    val fallbackAlpha: Float,
    val vibrancy: Boolean = true,
    val chromaticAberration: Boolean = false,
    val highlight: Boolean = true,
    val shadow: Boolean = true,
    val rim: Boolean = true,
) {
    companion object {
        /** Tab bar, top bar and floating toolbars: clear glass that visibly bends what scrolls behind it. */
        val Regular =
            LiquidGlassStyle(
                blurRadius = 3.dp,
                refractionHeight = 16.dp,
                refractionAmount = 32.dp,
                tintAlpha = 0.38f,
                fallbackAlpha = 0.94f,
            )

        /** Small floating buttons, chips and the selection lens: the clearest glass. */
        val Clear =
            LiquidGlassStyle(
                blurRadius = 2.dp,
                refractionHeight = 12.dp,
                refractionAmount = 24.dp,
                tintAlpha = 0.22f,
                fallbackAlpha = 0.9f,
                shadow = false,
            )

        /**
         * The floating tab bar: [Regular] glass with a little more tint, so the icons hold their
         * contrast over bright thumbnails.
         */
        val TabBar =
            LiquidGlassStyle(
                blurRadius = 4.dp,
                refractionHeight = 16.dp,
                refractionAmount = 32.dp,
                tintAlpha = 0.55f,
                fallbackAlpha = 0.96f,
            )

        /**
         * Edge-to-edge headers: at rest they are the page's own colour, and content scrolling under
         * them frosts, iOS-style. No lens, rim, highlight or shadow, so the header has no visible edge.
         */
        val Bar =
            LiquidGlassStyle(
                blurRadius = 20.dp,
                refractionHeight = 0.dp,
                refractionAmount = 0.dp,
                tintAlpha = 0.8f,
                fallbackAlpha = 1f,
                highlight = false,
                shadow = false,
                rim = false,
            )

        /** Sheets, menus and dialogs: frosted enough that long text on it stays readable. */
        val Thick =
            LiquidGlassStyle(
                blurRadius = 24.dp,
                refractionHeight = 20.dp,
                refractionAmount = 28.dp,
                tintAlpha = 0.62f,
                fallbackAlpha = 0.97f,
            )
    }
}

/** Colours of the glass, taken from the active theme so every palette gets matching glass. */
@Immutable
data class LiquidGlassColors(
    /** Laid over the blurred backdrop; carries the theme's surface hue into the glass. */
    val tint: Color,
    /** Hairline rim drawn on the fallback surface, where no shader highlight exists. */
    val rim: Color,
    /** Contact shadow under floating glass. */
    val shadow: Color,
)

object LiquidGlassDefaults {
    @Composable
    @ReadOnlyComposable
    fun colors(): LiquidGlassColors {
        val scheme = MaterialTheme.colorScheme
        val isDark = scheme.surface.luminance() < DARK_SURFACE_LUMINANCE
        return LiquidGlassColors(
            tint = scheme.surfaceContainer,
            rim = scheme.outlineVariant.copy(alpha = if (isDark) DARK_RIM_ALPHA else LIGHT_RIM_ALPHA),
            shadow = scheme.scrim.copy(alpha = if (isDark) DARK_SHADOW_ALPHA else LIGHT_SHADOW_ALPHA),
        )
    }

    private const val DARK_SURFACE_LUMINANCE = 0.5f
    private const val DARK_RIM_ALPHA = 0.55f
    private const val LIGHT_RIM_ALPHA = 0.8f
    private const val DARK_SHADOW_ALPHA = 0.32f
    private const val LIGHT_SHADOW_ALPHA = 0.12f
}

/** The backdrop glass in this subtree samples, or null when nothing records one (glass then falls back). */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Whether glass may draw backdrop effects at all. Off for "reduce transparency"-style preferences,
 * battery saver or low-end devices; glass then uses the opaque fallback everywhere.
 */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }

/** One recorded layer of window content that glass can sample. */
@Composable
fun rememberGlassBackdrop(): LayerBackdrop = rememberLayerBackdrop()

/**
 * Records this node's drawing into [backdrop] so glass drawn above it can blur and refract it.
 * Put it on the content container (e.g. the NavHost), never on an ancestor of the glass itself.
 */
fun Modifier.glassBackdropSource(backdrop: LayerBackdrop): Modifier = layerBackdrop(backdrop)

/** Makes [backdrop] the one [liquidGlass] and [GlassSurface] sample in [content]. */
@Composable
fun ProvideGlassBackdrop(
    backdrop: Backdrop?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalGlassBackdrop provides backdrop, content = content)
}

/**
 * Draws this node as Liquid Glass of [style] in [shape].
 *
 * [shape] is a [CornerBasedShape] (rounded rect or capsule — `RoundedCornerShape(50)`) because the
 * lens shader needs real corner radii. Content goes on top; this only draws the material.
 */
fun Modifier.liquidGlass(
    backdrop: Backdrop?,
    shape: CornerBasedShape,
    colors: LiquidGlassColors,
    style: LiquidGlassStyle = LiquidGlassStyle.Regular,
    enabled: Boolean = true,
): Modifier {
    if (backdrop == null || !enabled || !LiquidGlassSupport.hasBlur()) {
        val solid =
            this
                .clip(shape)
                .background(colors.tint.copy(alpha = style.fallbackAlpha), shape)
        return if (style.rim) solid.border(Dp.Hairline, colors.rim, shape) else solid
    }
    val tint = colors.tint.copy(alpha = style.tintAlpha)
    val shadow = if (style.shadow) Shadow(color = colors.shadow) else null
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (style.vibrancy) vibrancy()
            blur(style.blurRadius.toPx())
            if (LiquidGlassSupport.hasRefraction() && style.refractionAmount > 0.dp) {
                lens(
                    refractionHeight = style.refractionHeight.toPx(),
                    refractionAmount = style.refractionAmount.toPx(),
                    chromaticAberration = style.chromaticAberration,
                )
            }
        },
        highlight = if (style.highlight) ({ Highlight.Default }) else null,
        shadow = shadow?.let { s -> { s } },
        onDrawSurface = { drawRect(tint) },
    )
}

/**
 * A Liquid Glass container: [liquidGlass] with the theme's glass colours and the provided backdrop.
 */
@Composable
fun GlassSurface(
    shape: CornerBasedShape,
    modifier: Modifier = Modifier,
    style: LiquidGlassStyle = LiquidGlassStyle.Regular,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier.liquidGlass(
                backdrop = backdrop,
                shape = shape,
                colors = LiquidGlassDefaults.colors(),
                style = style,
                enabled = LocalLiquidGlassEnabled.current,
            ),
        content = content,
    )
}

/**
 * The [liquidGlass] modifier for the current theme: the backdrop from [LocalGlassBackdrop], the theme's
 * glass colours and the [LocalLiquidGlassEnabled] switch. Chrome without a backdrop gets the fallback.
 */
@Composable
fun themedGlass(
    shape: CornerBasedShape,
    style: LiquidGlassStyle = LiquidGlassStyle.Regular,
    backdrop: Backdrop? = LocalGlassBackdrop.current,
    tint: Color? = null,
): Modifier =
    Modifier.liquidGlass(
        backdrop = backdrop,
        shape = shape,
        colors = LiquidGlassDefaults.colors().let { if (tint != null) it.copy(tint = tint) else it },
        style = style,
        enabled = LocalLiquidGlassEnabled.current,
    )

/** Shapes the glass chrome uses, so every capsule and sheet bends light the same way. */
object GlassShapes {
    /** Tab bar, mini player, floating toolbars and pill buttons. */
    val Capsule: CornerBasedShape = RoundedCornerShape(percent = 50)

    /** Glass cards and grouped rows. */
    val Card: CornerBasedShape = RoundedCornerShape(24.dp)

    /** The edge-to-edge bottom bar: rounded where it meets the content. */
    val BottomBar: CornerBasedShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** Edge-to-edge headers: square, so they meet the status bar and the screen edges cleanly. */
    val TopBar: CornerBasedShape = RoundedCornerShape(0.dp)
}
