package com.voxtube.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.navigation.NavBackStackEntry
import com.voxtube.app.ui.components.layout.navigation.VoxTubeTab
import com.voxtube.app.ui.theme.NavigationMotion

/** How the screen changes between two back stack entries. */
internal enum class NavTransitionKind {
    /** A route that leaves on its first frame is involved: nothing moves. */
    NONE,

    /** Between two tabs' first screens: a fade-through, since neither is deeper than the other. */
    TAB,

    /** Into or out of a page: it slides in from the edge. */
    PAGE,
}

private val INSTANT_ROUTES =
    setOf(
        MUSIC_PLAYER_ROUTE_PATTERN,
        LINKED_PLAYLIST_ROUTE_PATTERN,
        PLAYER_ROUTE_PATTERN,
        ON_REPEAT_SHUFFLE_ROUTE,
        WIDGET_PLAYLIST_ROUTE,
    )

/** Search counts as a tab only when the bar has a Search tab; otherwise it is a page over the tab it was opened from. */
internal fun navTransitionKind(
    fromRoute: String?,
    fromTab: VoxTubeTab?,
    toRoute: String?,
    toTab: VoxTubeTab?,
    searchIsTab: Boolean,
): NavTransitionKind {
    if (fromRoute in INSTANT_ROUTES || toRoute in INSTANT_ROUTES) return NavTransitionKind.NONE
    val searchInvolved = fromTab == VoxTubeTab.Search || toTab == VoxTubeTab.Search
    val twoTabs = fromTab != null && toTab != null && fromTab != toTab
    return if (twoTabs && (!searchInvolved || searchIsTab)) NavTransitionKind.TAB else NavTransitionKind.PAGE
}

/**
 * The NavHost's transitions. A page slides in over the page it came from, which drifts a quarter
 * of the way out underneath. Only translation changes, so neither page needs an offscreen layer; a tab
 * fades through and settles in from a slightly smaller scale.
 * With [reducedMotion] (a low-RAM phone, battery saver) tabs swap at once and pages slide briskly.
 */
internal class VoxTubeNavTransitions(
    private val searchIsTab: () -> Boolean,
    private val reducedMotion: () -> Boolean = { false },
) {
    private fun pageSlide() = if (reducedMotion()) NavigationMotion.PageSlideBrisk else NavigationMotion.PageSlide

    private var tabTapTargetId: String? = null

    /**
     * The arriving tab fades in while it settles up from a hair smaller, the way iOS 26 lifts a tab's
     * content into place. Scale and alpha share the fade's timing, so they finish together.
     */
    private fun tabEnter(): EnterTransition =
        fadeIn(NavigationMotion.TabFadeIn) + scaleIn(NavigationMotion.TabFadeIn, initialScale = TAB_ENTER_SCALE)

    /**
     * Called right after a tab tap navigates, with the entry it landed on. A tab can reopen on a
     * page deeper in its saved stack, and that switch still fades through like any other.
     */
    fun markTabTap(entryId: String?) {
        tabTapTargetId = entryId
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.kind(): NavTransitionKind {
        val tapTarget = tabTapTargetId
        if (tapTarget != targetState.id) tabTapTargetId = null
        val kind =
            navTransitionKind(
                fromRoute = initialState.destination.route,
                fromTab = initialState.flowTab(),
                toRoute = targetState.destination.route,
                toTab = targetState.flowTab(),
                searchIsTab = searchIsTab(),
            )
        val tabSwitch = kind == NavTransitionKind.TAB || (kind == NavTransitionKind.PAGE && tapTarget == targetState.id)
        return when {
            tabSwitch && reducedMotion() -> NavTransitionKind.NONE
            tabSwitch -> NavTransitionKind.TAB
            else -> kind
        }
    }

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when (kind()) {
            NavTransitionKind.NONE -> EnterTransition.None
            NavTransitionKind.TAB -> tabEnter()
            NavTransitionKind.PAGE -> slideIntoContainer(SlideDirection.Start, pageSlide())
        }
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when (kind()) {
            NavTransitionKind.NONE -> ExitTransition.None
            NavTransitionKind.TAB -> fadeOut(NavigationMotion.TabFadeOut)
            NavTransitionKind.PAGE -> slideOutOfContainer(SlideDirection.Start, pageSlide()) { it / UNDERNEATH_SHARE }
        }
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when (kind()) {
            NavTransitionKind.NONE -> EnterTransition.None
            NavTransitionKind.TAB -> tabEnter()
            NavTransitionKind.PAGE -> slideIntoContainer(SlideDirection.End, pageSlide()) { it / UNDERNEATH_SHARE }
        }
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when (kind()) {
            NavTransitionKind.NONE -> ExitTransition.None
            NavTransitionKind.TAB -> fadeOut(NavigationMotion.TabFadeOut)
            NavTransitionKind.PAGE -> slideOutOfContainer(SlideDirection.End, pageSlide())
        }
    }

    // A back gesture seeks these by its progress, so every part shares one linear duration and
    // follows the finger; a spring's open-ended duration let the fades finish a quarter of the way
    // in and left a cancelled gesture's state behind for the next one (#1069).
    val predictivePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = {
        when (kind()) {
            NavTransitionKind.NONE -> EnterTransition.None
            NavTransitionKind.TAB -> fadeIn(tween(PREDICTIVE_BACK_DURATION_MS, easing = LinearEasing))
            NavTransitionKind.PAGE -> slideIntoContainer(SlideDirection.End, predictiveBackSpec()) { it / UNDERNEATH_SHARE }
        }
    }

    val predictivePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = {
        when (kind()) {
            NavTransitionKind.NONE -> ExitTransition.None
            NavTransitionKind.TAB -> fadeOut(tween(PREDICTIVE_BACK_DURATION_MS, easing = LinearEasing))
            NavTransitionKind.PAGE -> slideOutOfContainer(SlideDirection.End, predictiveBackSpec())
        }
    }

    private fun <T> predictiveBackSpec() = tween<T>(PREDICTIVE_BACK_DURATION_MS, easing = LinearEasing)
}

private const val PREDICTIVE_BACK_DURATION_MS = 300
private const val UNDERNEATH_SHARE = 4
private const val TAB_ENTER_SCALE = 0.97f
