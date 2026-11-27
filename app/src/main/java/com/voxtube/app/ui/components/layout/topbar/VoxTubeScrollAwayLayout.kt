package com.voxtube.app.ui.components.layout.topbar

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomAppBarState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.voxtube.app.ui.LocalStatusBarHero
import com.voxtube.app.ui.components.layout.navigation.flowUsesNavigationRail
import com.voxtube.app.ui.theme.glass.GlassShapes
import com.voxtube.app.ui.theme.glass.LiquidGlassStyle
import com.voxtube.app.ui.theme.glass.LocalLiquidGlassEnabled
import com.voxtube.app.ui.theme.glass.glassBackdropSource
import com.voxtube.app.ui.theme.glass.rememberGlassBackdrop
import com.voxtube.app.ui.theme.glass.themedGlass
import com.voxtube.app.ui.theme.statusBarScrim
import com.voxtube.app.ui.utils.rememberTouchExplorationEnabled
import kotlin.math.roundToInt

/** How tall the fade under a header is, and how strongly it hides the content at its top. */
private val ScrollEdgeHeight = 20.dp
private const val SCROLL_EDGE_ALPHA = 0.85f

/** How far a [VoxTubeScrollAwayLayout]'s header has scrolled away. Saved with the screen, so Back restores it. */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
class VoxTubeScrollAwayState internal constructor(
    internal val barState: BottomAppBarState,
) {
    /** How far up the header has moved, in px, 0 when fully shown. Read in layout or draw only. */
    val headerOffsetPx: Float
        get() = barState.heightOffset

    /** Brings the header back, as a tab reselect or a chip change does. */
    suspend fun show(animationSpec: AnimationSpec<Float>) {
        if (barState.heightOffset == 0f) return
        animate(barState.heightOffset, 0f, animationSpec = animationSpec) { value, _ -> barState.heightOffset = value }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberFlowScrollAwayState(): VoxTubeScrollAwayState {
    val barState = rememberBottomAppBarState()
    return remember(barState) { VoxTubeScrollAwayState(barState) }
}

/**
 * A feed whose header (the top bar and anything under it, such as chips) slides away as the feed
 * scrolls down and comes back on the way up, snapping shown or hidden on release.
 *
 * It must start at the window's top edge: give the screen's outermost container (its Scaffold, which
 * clips its content) [reachUnderStatusBar]. The header's background runs up under the status bar,
 * so the status bar is solid while the header is shown; as the header slides away under the status
 * bar's translucent scrim, the feed shows through it. The header is laid over the feed and moved in
 * the layout phase; the feed keeps its full size and is never measured again while the header
 * moves. [content] receives the header's height, status bar included, as its top padding.
 *
 * The header is Liquid Glass: the feed is recorded, and as it scrolls under the header it shows
 * through blurred and refracted. Top bars inside the header drop their own fill ([LocalTopBarOnGlass]).
 *
 * The header stays put when [hideOnScroll] is off, with a screen reader exploring by touch, and on
 * windows with the rail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoxTubeScrollAwayLayout(
    header: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    state: VoxTubeScrollAwayState = rememberFlowScrollAwayState(),
    hideOnScroll: Boolean = true,
    content: @Composable (topPadding: Dp) -> Unit,
) {
    val density = LocalDensity.current
    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val statusBarTop = with(density) { statusBarTopPx.toDp() }
    val pinned = !hideOnScroll || rememberTouchExplorationEnabled() || flowUsesNavigationRail()
    val canScroll = rememberUpdatedState(!pinned)
    val motion = MaterialTheme.motionScheme
    val behavior =
        BottomAppBarDefaults.exitAlwaysScrollBehavior(
            state = state.barState,
            canScroll = { canScroll.value },
            snapAnimationSpec = motion.fastSpatialSpec(),
            flingAnimationSpec = rememberSplineBasedDecay(),
        )
    LaunchedEffect(pinned) { if (pinned) state.barState.heightOffset = 0f }

    val statusBarHero = LocalStatusBarHero.current
    val claim = remember { Any() }
    DisposableEffect(statusBarHero) {
        statusBarHero?.claim(claim, themedIcons = true)
        onDispose { statusBarHero?.release(claim) }
    }

    var headerHeightPx by rememberSaveable { mutableIntStateOf(0) }
    val headerHeight =
        if (headerHeightPx > 0) with(density) { headerHeightPx.toDp() } else statusBarTop + TopAppBarDefaults.TopAppBarExpandedHeight
    val scrim = MaterialTheme.colorScheme.statusBarScrim
    // Liquid Glass header: the feed is recorded and the header blurs and bends it as it scrolls under.
    val feedBackdrop = rememberGlassBackdrop()
    val glassOn = LocalLiquidGlassEnabled.current
    // Scroll edge effect: content fades out just before it slides under the header instead of meeting a
    // hard line. At the top of a page the fade lies over the page's own colour, so it is invisible.
    // It shows only once something has scrolled under the header, so the first card is never faded at rest.
    val scrollEdge = MaterialTheme.colorScheme.background.copy(alpha = SCROLL_EDGE_ALPHA)
    var scrolledUnder by rememberSaveable { mutableStateOf(false) }
    val edgeConnection = remember { ScrolledUnderConnection { scrolledUnder = it } }
    val edgeAlpha = animateFloatAsState(if (scrolledUnder) 1f else 0f, motion.fastEffectsSpec(), label = "scrollEdge")

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(edgeConnection)
                .nestedScroll(behavior.nestedScrollConnection)
                .drawWithContent {
                    drawContent()
                    drawRect(scrim, size = Size(size.width, statusBarTopPx.toFloat()))
                },
    ) {
        Box(
            modifier = Modifier.fillMaxSize().then(if (glassOn) Modifier.glassBackdropSource(feedBackdrop) else Modifier),
        ) {
            content(headerHeight)
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        headerHeightPx = size.height
                        state.barState.heightOffsetLimit = -size.height.toFloat()
                    }.offset { IntOffset(0, state.barState.heightOffset.roundToInt()) }
                    .drawWithContent {
                        drawContent()
                        val edge = ScrollEdgeHeight.toPx()
                        if (edgeAlpha.value > 0f) {
                            drawRect(
                                brush =
                                    Brush.verticalGradient(
                                        listOf(scrollEdge, Color.Transparent),
                                        startY = size.height,
                                        endY =
                                            size.height + edge,
                                    ),
                                topLeft = Offset(0f, size.height),
                                size = Size(size.width, edge),
                                alpha = edgeAlpha.value,
                            )
                        }
                    }.then(
                        themedGlass(
                            shape = GlassShapes.TopBar,
                            style = LiquidGlassStyle.Bar,
                            backdrop = feedBackdrop.takeIf { glassOn },
                            tint = MaterialTheme.colorScheme.background,
                        ),
                    ),
        ) {
            Spacer(Modifier.height(statusBarTop))
            CompositionLocalProvider(LocalTopBarOnGlass provides true) { header() }
        }
    }
}

/**
 * Tells whether content has scrolled under the header: on once the content moves up, off again when a
 * scroll or fling runs into the top of the content.
 */
private class ScrolledUnderConnection(
    private val onChange: (Boolean) -> Unit,
) : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        when {
            available.y > 0f -> onChange(false)
            consumed.y < 0f -> onChange(true)
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        if (available.y > 0f) onChange(false)
        return Velocity.Zero
    }
}
