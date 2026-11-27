package com.voxtube.app.ui.components.layout.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.voxtube.app.data.local.NavBarLook
import com.voxtube.app.data.local.NavBarScroll
import com.voxtube.app.data.local.NavBarStyle
import com.voxtube.app.ui.theme.glass.LocalGlassBackdrop
import com.voxtube.app.ui.theme.glass.LocalLiquidGlassEnabled
import com.voxtube.app.ui.theme.glass.ProvideGlassBackdrop
import com.voxtube.app.ui.theme.glass.glassBackdropSource
import com.voxtube.app.ui.utils.LocalWindowIsLandscape
import com.voxtube.app.ui.utils.LocalWindowSizeClass
import com.voxtube.app.ui.utils.isExpandedWidth
import com.voxtube.app.ui.utils.isMediumHeight
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

const val FLOW_NAV_BAR_TAG = "flow_nav_bar"
const val FLOW_NAV_RAIL_TAG = "flow_nav_rail"

object VoxTubeNavigationDefaults {
    /**
     * The bar's height above the system navigation inset: Material's short navigation bar
     * container. The bar reports its real height, which grows past this at large font scales.
     */
    val BarHeight: Dp = 64.dp

    /** One tab of the floating bar; shrunk, the bar is this plus the toolbar's side padding. */
    val FloatingItemSize: Dp = FloatingNavItemHeight
}

/**
 * Whether this window navigates from the start edge instead of the bottom. An expanded window tall
 * enough for the rail's items gets it, as on tablets. A landscape window too short for that, which
 * is a phone on its side, gets it too: a bottom bar would take a sixth of its height (#1845), and
 * the rail moves what does not fit behind "More". The decision belongs to the window, so a phone in
 * a narrow portrait split window keeps the bar even on a large display.
 */
@Composable
fun flowUsesNavigationRail(): Boolean = usesNavigationRail(LocalWindowSizeClass.current, LocalWindowIsLandscape.current)

internal fun usesNavigationRail(
    windowSizeClass: WindowSizeClass,
    isLandscape: Boolean,
): Boolean =
    (windowSizeClass.isExpandedWidth && windowSizeClass.isMediumHeight) ||
        (isLandscape && !windowSizeClass.isMediumHeight)

/**
 * The app's primary navigation around [content]: the bottom bar over it on compact and medium
 * windows, the wide navigation rail beside it once the window is expanded.
 *
 * The bar overlays the content, so callers reserve [onBarHeightChanged] themselves. [barScrollState]
 * moves it with the content's scroll. [glassBackdrop] records the pages for the Liquid Glass bar. The rail takes
 * its own column; [onRailWidthChanged] is only for overlays drawn outside this layout.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VoxTubeNavigationChrome(
    tabs: List<VoxTubeTab>,
    selectedTab: VoxTubeTab?,
    onTabSelected: (VoxTubeTab) -> Unit,
    barVisible: Boolean,
    railVisible: Boolean,
    modifier: Modifier = Modifier,
    barCovered: Boolean = false,
    barCoveredFraction: () -> Float = { 0f },
    barScrollState: VoxTubeNavigationScrollState? = null,
    barLook: NavBarLook = NavBarLook(),
    barRestsFolded: Boolean = false,
    onBarHeightChanged: (Dp) -> Unit = {},
    onRailWidthChanged: (Dp) -> Unit = {},
    glassBackdrop: LayerBackdrop? = null,
    onSearchClick: (() -> Unit)? = null,
    searchActive: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val usesRail = flowUsesNavigationRail()
    val showRail = usesRail && railVisible
    val shrinks = !usesRail && barLook.style == NavBarStyle.FLOATING && barLook.scroll == NavBarScroll.SHRINK
    SideEffect { barScrollState?.shrinks = shrinks }
    val shrinkOnScroll =
        if (barScrollState != null && barScrollState.shrinks) {
            Modifier.floatingToolbarVerticalNestedScroll(
                expanded = !barScrollState.isCollapsed,
                onExpand = barScrollState::expand,
                onCollapse = barScrollState::collapse,
            )
        } else {
            Modifier
        }
    val density = LocalDensity.current
    val railInsets = WideNavigationRailDefaults.windowInsets
    // [barRestsFolded]: the floating bar sits folded to its open tab. A tap unfolds it; swiping the page,
    // choosing a tab or a few idle seconds fold it again.
    var restUnfolded by remember { mutableStateOf(false) }
    LaunchedEffect(barRestsFolded) { restUnfolded = false }
    LaunchedEffect(restUnfolded) {
        if (restUnfolded) {
            delay(REST_UNFOLD_MILLIS)
            restUnfolded = false
        }
    }
    val refoldOnScroll =
        remember {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y != 0f) restUnfolded = false
                    return Offset.Zero
                }
            }
        }

    Row(modifier = modifier.fillMaxSize()) {
        if (showRail) {
            VoxTubeNavigationRail(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                modifier =
                    Modifier
                        .testTag(FLOW_NAV_RAIL_TAG)
                        .onSizeChanged { onRailWidthChanged(with(density) { it.width.toDp() }) },
            )
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(shrinkOnScroll)
                    .then(if (barRestsFolded) Modifier.nestedScroll(refoldOnScroll) else Modifier)
                    .then(
                        if (showRail) {
                            Modifier.consumeWindowInsets(railInsets.only(WindowInsetsSides.Start))
                        } else {
                            Modifier
                        },
                    ),
        ) {
            // The pages are recorded so the glass bar floating over them can blur and bend them. Glass
            // inside the pages must not sample that recording (it would sample itself), so it gets none;
            // pages record their own (see VoxTubeScrollAwayLayout), and only while glass is on at all.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(if (glassBackdrop != null) Modifier.glassBackdropSource(glassBackdrop) else Modifier),
            ) {
                val pages = this
                CompositionLocalProvider(
                    LocalGlassBackdrop provides null,
                    LocalLiquidGlassEnabled provides (glassBackdrop != null),
                ) { pages.content() }
            }
            if (!usesRail) {
                ProvideGlassBackdrop(glassBackdrop) {
                    NavigationBarSlot(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            restUnfolded = false
                            onTabSelected(tab)
                        },
                        restFolded = barRestsFolded && !restUnfolded,
                        onRestUnfold = { restUnfolded = true },
                        visible = barVisible,
                        covered = barCovered,
                        coveredFraction = barCoveredFraction,
                        scrollState = barScrollState,
                        look = barLook,
                        onHeightChanged = onBarHeightChanged,
                        onSearchClick =
                            onSearchClick?.let { open ->
                                {
                                    restUnfolded = false
                                    open()
                                }
                            },
                        searchActive = searchActive,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.NavigationBarSlot(
    tabs: List<VoxTubeTab>,
    selectedTab: VoxTubeTab?,
    onTabSelected: (VoxTubeTab) -> Unit,
    restFolded: Boolean,
    onRestUnfold: () -> Unit,
    visible: Boolean,
    covered: Boolean,
    coveredFraction: () -> Float,
    scrollState: VoxTubeNavigationScrollState?,
    look: NavBarLook,
    onHeightChanged: (Dp) -> Unit,
    onSearchClick: (() -> Unit)?,
    searchActive: Boolean,
) {
    val density = LocalDensity.current
    val bottomInsetPx = ShortNavigationBarDefaults.windowInsets.getBottom(density)
    val motion = MaterialTheme.motionScheme
    val travelToBottom = remember { mutableFloatStateOf(0f) }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(motion.defaultSpatialSpec()) { it } + fadeIn(motion.defaultEffectsSpec()),
        exit = slideOutVertically(motion.fastSpatialSpec()) { it } + fadeOut(motion.fastEffectsSpec()),
    ) {
        // While a video player is up the bar stays under it and rides its expand fraction down
        // off screen, measured from where the bar rests so the scroll-away offset is not counted.
        val barModifier =
            Modifier
                .testTag(FLOW_NAV_BAR_TAG)
                .onPlaced { coords ->
                    travelToBottom.floatValue = coords.findRootCoordinates().size.height - coords.positionInRoot().y
                }.onSizeChanged {
                    onHeightChanged(with(density) { (it.height - bottomInsetPx).toDp() })
                    scrollState?.setBarHeight(it.height.toFloat())
                }.offset { IntOffset(0, scrollState?.offsetPx?.roundToInt() ?: 0) }
                .graphicsLayer { translationY = coveredFraction().coerceIn(0f, 1f) * travelToBottom.floatValue }
                .then(if (covered) Modifier.clearAndSetSemantics { } else Modifier)
        when (look.style) {
            NavBarStyle.STANDARD -> {
                VoxTubeNavigationBar(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    labels = look.labels,
                    modifier = barModifier,
                )
            }

            NavBarStyle.FLOATING -> {
                VoxTubeFloatingNavigationBar(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    look = look,
                    collapsed = restFolded || scrollState?.isCollapsed == true,
                    onExpand = {
                        onRestUnfold()
                        scrollState?.expand()
                    },
                    // No scroll edge over a surface that rests folded (Shorts): it would wash out the video.
                    modifier = if (restFolded) barModifier else barModifier.bottomScrollEdge(MaterialTheme.colorScheme.background),
                    onSearchClick = onSearchClick,
                    searchActive = searchActive,
                )
            }
        }
    }
}

/** How long a bar that rests folded stays open after a tap before folding back on its own. */
private const val REST_UNFOLD_MILLIS = 4_000L

/** How far above the floating bar the bottom scroll edge starts, and how much of the page it hides at the bottom. */
private val BottomScrollEdgeReach = 24.dp
private const val BOTTOM_SCROLL_EDGE_ALPHA = 0.6f

/**
 * Scroll edge effect under the floating bar: the page fades softly into [color] toward the screen's bottom
 * edge instead of ending in a hard line behind the glass. Drawn behind the bar, and outside the backdrop
 * the glass samples, so the glass itself still shows the page at full strength.
 */
private fun Modifier.bottomScrollEdge(color: Color): Modifier =
    drawBehind {
        val reach = BottomScrollEdgeReach.toPx()
        drawRect(
            brush =
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to color.copy(alpha = BOTTOM_SCROLL_EDGE_ALPHA),
                    startY = -reach,
                    endY = size.height,
                ),
            topLeft = Offset(0f, -reach),
            size = Size(size.width, size.height + reach),
        )
    }
