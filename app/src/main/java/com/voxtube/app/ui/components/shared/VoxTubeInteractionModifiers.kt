package com.voxtube.app.ui.components.shared

import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.voxtube.app.ui.theme.ArtworkScrim
import com.voxtube.app.ui.theme.glass.LocalGlassAccessibility

/**
 * Adds a subtle press-scale animation to any composable.
 * When the user presses down, the element shrinks to [pressedScale] on the theme's fast spatial spring,
 * giving tactile depth feedback without any layout shift.
 *
 * Usage: Modifier.pressScale(interactionSource)
 * The interactionSource should be the same one passed to clickable/combinedClickable.
 *
 * Composable rather than `composed {}`: the latter is opaque to Modifier equality, so every card
 * in a list re-materialised its whole chain on each recomposition. The spring is read inside the
 * [graphicsLayer] block instead of the composition, which keeps its per-frame updates off the
 * composition and layout phases entirely — only the layer block re-runs.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale =
        animateFloatAsState(
            targetValue = if (isPressed) pressedScale else 1f,
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "pressScale",
        )
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * The iOS press on a pill of glass: it sinks to [pressedScale] under the finger and springs back
 * with a little bounce, with a light tick as it goes down. [interactionSource] must be the one the
 * clickable uses, or the press is never seen. Reduce Motion keeps the tick and drops the bounce.
 */
@Composable
fun Modifier.pressBounce(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = PRESS_BOUNCE_SCALE,
    haptic: Boolean = true,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    if (haptic) {
        LaunchedEffect(isPressed) {
            if (isPressed) haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    }
    if (LocalGlassAccessibility.current.reduceMotion) return this
    val scale =
        animateFloatAsState(
            targetValue = if (isPressed) pressedScale else 1f,
            animationSpec = PressBounceSpring,
            label = "pressBounce",
        )
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** How far a pressed pill sinks. */
const val PRESS_BOUNCE_SCALE = 0.95f

/** Medium bounce, medium-low stiffness: the pill overshoots a touch on release, like iOS. */
private val PressBounceSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

/**
 * Scrim over the bottom of a thumbnail so the duration pill stays legible on light frames.
 *
 * Built in [drawWithCache] because the brush depends only on the layout size: created per draw it
 * allocated a Brush and its colour list for every visible card on every frame of a scroll.
 */
fun Modifier.thumbnailGradientOverlay(
    color: Color = ArtworkScrim,
    alpha: Float = 0.25f,
    startFraction: Float = 0.6f,
): Modifier =
    this.drawWithCache {
        val brush =
            Brush.verticalGradient(
                colors =
                    listOf(
                        Color.Transparent,
                        color.copy(alpha = alpha),
                    ),
                startY = size.height * startFraction,
                endY = size.height,
            )
        onDrawWithContent {
            drawContent()
            drawRect(brush = brush)
        }
    }

/**
 * Returns a [SheetState] configured for a smooth, polished bottom sheet experience:
 * - [skipPartiallyExpanded] defaults to true, so sheets always open fully without
 *   stopping at the awkward half-expanded state.
 *
 * Usage: `sheetState = rememberFlowSheetState()`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberFlowSheetState(skipPartiallyExpanded: Boolean = true): SheetState =
    rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
    )

/**
 * Drops the keyboard the moment a finger lands on a scrollable surface behind a text field.
 *
 * Runs on the initial pass so the list still receives the gesture — the field never steals it.
 */
fun Modifier.dismissKeyboardOnPress(onPress: () -> Unit): Modifier =
    pointerInput(onPress) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                if (event.changes.any { it.pressed }) onPress()
            }
        }
    }

fun LazyItemScope.animateMediaListItem(): Modifier =
    Modifier.animateItem(
        fadeInSpec = tween(ITEM_FADE_IN_MILLIS, easing = EaseOutCubic),
        fadeOutSpec = tween(ITEM_FADE_OUT_MILLIS, easing = EaseInCubic),
        placementSpec = spring(dampingRatio = ITEM_PLACEMENT_DAMPING, stiffness = Spring.StiffnessLow),
    )

/** [animateMediaListItem] for an item of a lazy grid. */
fun LazyGridItemScope.animateMediaGridItem(): Modifier =
    Modifier.animateItem(
        fadeInSpec = tween(ITEM_FADE_IN_MILLIS, easing = EaseOutCubic),
        fadeOutSpec = tween(ITEM_FADE_OUT_MILLIS, easing = EaseInCubic),
        placementSpec = spring(dampingRatio = ITEM_PLACEMENT_DAMPING, stiffness = Spring.StiffnessLow),
    )

private const val ITEM_FADE_IN_MILLIS = 300
private const val ITEM_FADE_OUT_MILLIS = 200
private const val ITEM_PLACEMENT_DAMPING = 0.8f
