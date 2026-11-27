package com.voxtube.app.ui

import android.app.Activity
import android.app.ActivityManager
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.voxtube.app.MainActivity
import com.voxtube.app.data.local.NavBarLook
import com.voxtube.app.data.local.NavBarScroll
import com.voxtube.app.data.local.NavBarStyle
import com.voxtube.app.data.local.PlayerPreferences
import com.voxtube.app.data.profile.PendingSlice
import com.voxtube.app.data.recommendation.VoxTubeNeuroEngine
import com.voxtube.app.player.DeepFlowManager
import com.voxtube.app.player.EnhancedMusicPlayerManager
import com.voxtube.app.player.EnhancedPlayerManager
import com.voxtube.app.player.GlobalPlayerState
import com.voxtube.app.player.SleepTimerManager
import com.voxtube.app.sync2.service.LinkedCatchUp
import com.voxtube.app.ui.components.equalizer.LocalEqualizerState
import com.voxtube.app.ui.components.layout.LocalFlowBottomInsets
import com.voxtube.app.ui.components.layout.LocalFlowSnackbarHostState
import com.voxtube.app.ui.components.layout.navigation.NavigationVisibility
import com.voxtube.app.ui.components.layout.navigation.VoxTubeNavigationChrome
import com.voxtube.app.ui.components.layout.navigation.VoxTubeNavigationDefaults
import com.voxtube.app.ui.components.layout.navigation.VoxTubeTab
import com.voxtube.app.ui.components.layout.navigation.flowUsesNavigationRail
import com.voxtube.app.ui.components.layout.navigation.rememberFlowNavigationScrollState
import com.voxtube.app.ui.components.layout.navigation.resolveDefaultFlowTab
import com.voxtube.app.ui.components.layout.navigation.visibleFlowTabs
import com.voxtube.app.ui.components.layout.topbar.ProvideFlowGlobalActions
import com.voxtube.app.ui.components.music.common.ProvideMusicPlaybackState
import com.voxtube.app.ui.components.music.sheet.LocalMusicMenus
import com.voxtube.app.ui.components.music.sheet.rememberMusicMenus
import com.voxtube.app.ui.components.musicplayer.sheet.rememberMusicPlayerSheetState
import com.voxtube.app.ui.components.profiles.ProfileSwitchViewModel
import com.voxtube.app.ui.components.profiles.ProfileTopBarAction
import com.voxtube.app.ui.components.profiles.SwitchRefusals
import com.voxtube.app.ui.components.settings.SettingsDestination
import com.voxtube.app.ui.components.settings.SettingsTarget
import com.voxtube.app.ui.components.shared.LocalMediaOpenOrigins
import com.voxtube.app.ui.components.shared.LocalNetworkOnline
import com.voxtube.app.ui.components.shared.MediaOpenOrigins
import com.voxtube.app.ui.components.shared.SubscriptionBellAlerts
import com.voxtube.app.ui.components.shared.VoxTubeLoadingIndicator
import com.voxtube.app.ui.components.shared.VoxTubeSharedTransitions
import com.voxtube.app.ui.components.videoplayer.PlayerSheetValue
import com.voxtube.app.ui.components.videoplayer.rememberPlayerDraggableState
import com.voxtube.app.ui.screens.equalizer.EqualizerViewModel
import com.voxtube.app.ui.screens.home.HomeViewModel
import com.voxtube.app.ui.screens.notifications.NotificationViewModel
import com.voxtube.app.ui.screens.player.VideoPlayerViewModel
import com.voxtube.app.ui.screens.playon.LocalPlayOn
import com.voxtube.app.ui.screens.playon.PlayOnHost
import com.voxtube.app.ui.screens.playon.remoteSlot
import com.voxtube.app.ui.screens.settings.wellbeing.TIMES_UP_ROUTE
import com.voxtube.app.ui.screens.settings.wellbeing.TimesUpLaunchEffect
import com.voxtube.app.ui.screens.sync.linked.LinkedSyncAlerts
import com.voxtube.app.ui.screens.update.UPDATE_ROUTE
import com.voxtube.app.ui.screens.update.UpdateLaunchEffect
import com.voxtube.app.ui.theme.ThemeMode
import com.voxtube.app.ui.theme.ThemeVariant
import com.voxtube.app.ui.theme.glass.LocalGlassBackdrop
import com.voxtube.app.ui.theme.glass.LocalLiquidGlassEnabled
import com.voxtube.app.ui.theme.glass.rememberGlassBackdrop
import com.voxtube.app.ui.utils.rememberPowerSaveMode
import com.voxtube.app.ui.utils.rememberTouchExplorationEnabled
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

@UnstableApi
@Composable
fun VoxTubeApp(
    currentTheme: ThemeMode,
    themeVariant: ThemeVariant,
    systemLightThemeMode: ThemeMode,
    systemDarkThemeMode: ThemeMode,
    deeplinkVideoId: String? = null,
    isShort: Boolean = false,
    isSharedLink: Boolean = false,
    openMusicPlayerRequest: Int = 0,
    onDeeplinkConsumed: () -> Unit = {},
    pendingRoute: String? = null,
    onPendingRouteConsumed: () -> Unit = {},
    downloadVideoId: String? = null,
    onDownloadConsumed: () -> Unit = {},
    onStartDestinationKnown: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val navController = rememberNavController()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    val playerViewModel: VideoPlayerViewModel = hiltViewModel(activity!!)
    // Activity-scoped so the unread badge has exactly one collector for the whole shell.
    val notificationViewModel: NotificationViewModel = hiltViewModel(activity)
    val equalizerViewModel: EqualizerViewModel = hiltViewModel(activity)
    // One for every root top bar's profile avatar, rather than one per back stack entry.
    val profileSwitchViewModel: ProfileSwitchViewModel = hiltViewModel(activity)
    val playerUiStateResult = playerViewModel.uiState.collectAsStateWithLifecycle()
    val playerUiState by playerUiStateResult

    val preferences = remember { PlayerPreferences(context) }
    val isHomeNavigationEnabled by preferences.homeNavigationEnabled.collectAsState(initial = true)
    val isShortsNavigationEnabled by preferences.effectiveShortsNavigationEnabled.collectAsState(initial = true)
    val isMusicNavigationEnabled by preferences.musicNavigationEnabled.collectAsState(initial = true)
    val isSearchNavigationEnabled by preferences.searchNavigationEnabled.collectAsState(initial = false)
    val isCategoriesNavigationEnabled by preferences.categoriesNavigationEnabled.collectAsState(initial = false)
    val isDownloadsNavigationEnabled by preferences.downloadsNavigationEnabled.collectAsState(initial = false)
    val disableShortsPlayer by preferences.effectiveDisableShortsPlayer.collectAsState(initial = false)
    val navTabOrder by preferences.navTabOrder.collectAsState(initial = com.voxtube.app.data.local.DEFAULT_NAV_TAB_ORDER)
    val defaultNavTabIndex by preferences.defaultNavTabIndex.collectAsState(initial = 0)
    val subscriptionRefreshOnStartup by preferences.subscriptionRefreshOnStartup.collectAsState(initial = false)
    val bottomNavHideOnScroll by preferences.bottomNavHideOnScroll.collectAsState(initial = true)
    val bottomNavLook by preferences.bottomNavLook.collectAsState(initial = NavBarLook())
    val searchShowsNavBar by preferences.searchShowsNavBar.collectAsState(initial = false)
    val innerPagesShowNavBar by preferences.innerPagesShowNavBar.collectAsState(initial = false)
    val sleepTimerCloseAppOnExpiry by preferences.sleepTimerCloseAppOnExpiry.collectAsState(
        initial = SleepTimerManager.preferredCloseAppOnExpiry,
    )
    val navigationVisibility =
        NavigationVisibility(
            home = isHomeNavigationEnabled,
            shorts = isShortsNavigationEnabled,
            music = isMusicNavigationEnabled,
            search = isSearchNavigationEnabled,
            categories = isCategoriesNavigationEnabled,
            downloads = isDownloadsNavigationEnabled,
        )
    val navigationTabs = remember(navTabOrder, navigationVisibility) { visibleFlowTabs(navTabOrder, navigationVisibility) }
    val defaultTab = resolveDefaultFlowTab(defaultNavTabIndex, navTabOrder, navigationVisibility)
    val defaultStartRoute = defaultTab.route
    val currentNavigationTabs by rememberUpdatedState(navigationTabs)
    val lowRamDevice = remember(context) { context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true }
    val powerSave = rememberPowerSaveMode()
    val reducedNavMotion by rememberUpdatedState(lowRamDevice || powerSave)
    // Liquid Glass: the pages are recorded once and the bar, mini players and their sheets bend them.
    // Low-RAM devices and battery saver get the solid fallback glass instead.
    val glassBackdrop = rememberGlassBackdrop()
    val glassEnabled = !lowRamDevice && !powerSave
    val navTransitions =
        remember {
            VoxTubeNavTransitions(searchIsTab = { VoxTubeTab.Search in currentNavigationTabs }, reducedMotion = { reducedNavMotion })
        }
    val currentStartRoute by rememberUpdatedState(defaultStartRoute)
    val openDownloads = remember(navController) { { navController.openDownloads(currentNavigationTabs, currentStartRoute) } }

    // Mini Player Customizations
    val miniPlayerScale by preferences.miniPlayerScale.collectAsState(initial = 0.45f)
    val miniPlayerShowSkipControls by preferences.miniPlayerShowSkipControls.collectAsState(initial = false)
    val miniPlayerShowNextPrevControls by preferences.miniPlayerShowNextPrevControls.collectAsState(initial = false)
    val showRestoredMusicMiniPlayer by produceState<Boolean?>(initialValue = null, preferences) {
        preferences.showRestoredMusicMiniPlayer.collect { value = it }
    }

    // Set from the back stack, never by a page while it composes: a page leaving during a
    // transition would write its own key back over the one arriving.
    val currentRoute = remember { mutableStateOf(defaultStartRoute) }
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            routeKeyFor(entry.destination.route)?.let { currentRoute.value = it }
        }
    }

    // Onboarding check
    var needsOnboarding by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        PendingSlice.await()
        LinkedCatchUp.await()
        DeepFlowManager.initialize(context)
        val bypass = activity?.intent?.getBooleanExtra(MainActivity.EXTRA_BENCHMARK_BYPASS_ONBOARDING, false) == true
        // A viewer who is past onboarding never waits for the engine here; everyone else asks it (#1301).
        needsOnboarding =
            when {
                bypass || preferences.onboardingDone.first() -> {
                    false
                }

                else -> {
                    VoxTubeNeuroEngine.initialize(context)
                    VoxTubeNeuroEngine.needsOnboarding().also { preferences.setOnboardingDone(!it) }
                }
            }
        onStartDestinationKnown()
    }

    VoxTubeAppSideEffects(
        snackbarHostState = snackbarHostState,
        sleepTimerCloseAppOnExpiry = sleepTimerCloseAppOnExpiry,
        subscriptionRefreshOnStartup = subscriptionRefreshOnStartup,
    )

    HandleIncomingVideo(
        videoId = deeplinkVideoId,
        isShort = isShort,
        isShared = isSharedLink,
        navController = navController,
        preferences = preferences,
        shortsExitRoute = { exitTab(currentNavigationTabs, VoxTubeTab.Shorts).route },
        onConsumed = onDeeplinkConsumed,
    )
    HandleIncomingDownload(downloadVideoId, onDownloadConsumed)
    val networkOnline = rememberNetworkOnline(context)
    OfflineMonitor(context, networkOnline, snackbarHostState, currentRoute, openDownloads)

    val currentEntry by navController.currentBackStackEntryAsState()
    val statusBarHero = remember { StatusBarHeroState() }
    val currentTab = currentEntry?.flowTab()
    val selectedTab by rememberSelectedFlowTab(navController, defaultTab) { currentNavigationTabs }
    val usesNavigationRail = flowUsesNavigationRail()
    var navigationRailWidth by remember { mutableStateOf(0.dp) }
    var navigationBarHeight by remember { mutableStateOf(VoxTubeNavigationDefaults.BarHeight) }

    VoxTubeStartTabEffects(
        navController = navController,
        currentRoute = currentRoute,
        defaultTab = defaultTab,
        isHomeNavigationEnabled = isHomeNavigationEnabled,
        needsOnboarding = needsOnboarding,
    )

    val navScrollState =
        rememberFlowNavigationScrollState(
            // A screen reader exploring by touch would otherwise land on a bar that has moved off screen.
            hideOnScroll = bottomNavHideOnScroll && !usesNavigationRail && !rememberTouchExplorationEnabled(),
            routeKey = currentEntry?.id ?: currentRoute.value,
            locked = currentRoute.value == SHORTS_ROUTE_KEY,
        )

    val isInPipMode by GlobalPlayerState.isInPipMode.collectAsState()
    val currentVideo by GlobalPlayerState.currentVideo.collectAsState()
    val isShortsPlayerRoute = currentRoute.value == SHORTS_ROUTE_KEY

    LaunchedEffect(isShortsPlayerRoute) {
        if (isShortsPlayerRoute) {
            EnhancedPlayerManager.getInstance().pause()
            GlobalPlayerState.hideMiniPlayer()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenHeightPx = constraints.maxHeight.toFloat()

        val navBarBottomInset = WindowInsets.navigationBars.getBottom(density)

        val playerSheetState = rememberPlayerDraggableState()
        val mediaOpenOrigins = remember { MediaOpenOrigins() }
        val playerVisibleState = remember { mutableStateOf(false) }
        var playerVisible by playerVisibleState

        val musicPlayerSheetState = rememberMusicPlayerSheetState()
        val musicMenus = rememberMusicMenus()
        val collapseExpandedPlayers: () -> Unit =
            remember(playerSheetState, musicPlayerSheetState) {
                {
                    if (playerSheetState.currentValue == PlayerSheetValue.Expanded) playerSheetState.collapse()
                    if (musicPlayerSheetState.isExpanded) musicPlayerSheetState.collapse()
                }
            }
        val currentShortsExitRoute by rememberUpdatedState(exitTab(navigationTabs, VoxTubeTab.Shorts).route)
        val mediaNavigator =
            remember(navController, collapseExpandedPlayers) {
                VoxTubeMediaNavigator(navController, collapseExpandedPlayers) { currentShortsExitRoute }
            }
        // A page opened from another app would otherwise land under an expanded player.
        HandlePendingRoute(
            pendingRoute,
            navController,
            defaultStartRoute,
            // Read from the setting itself: a widget opening a cold start runs before the bar's state has loaded.
            downloadsTab = { preferences.downloadsNavigationEnabled.first() },
            onPendingRouteConsumed,
            onBeforeNavigate = collapseExpandedPlayers,
        )

        val activeVideo = playerUiState.cachedVideo

        VoxTubePlayerSessionEffects(
            playerSheetState = playerSheetState,
            playerViewModel = playerViewModel,
            playerUiStateResult = playerUiStateResult,
            playerVisibleState = playerVisibleState,
            isInPipMode = isInPipMode,
            openOrigins = mediaOpenOrigins,
        )

        val currentMusicTrack by EnhancedMusicPlayerManager.currentTrack.collectAsStateWithLifecycle()
        var suppressMusicMiniAfterVideo by remember { mutableStateOf(false) }
        val openMusicPlayerOnPlay = preferences.openMusicPlayerOnPlay.collectAsState(initial = false)
        // Shows the player for a song that just started, without a route: a navigation here used to
        // swap the page out and back for a frame, which the mini player now leaves in view.
        val onMusicStarted: () -> Unit =
            remember(musicPlayerSheetState, playerViewModel) {
                {
                    // One player at a time: a video still cached, even one playing in the
                    // background, would keep the music from showing its own mini player.
                    if (playerViewModel.uiState.value.cachedVideo != null) {
                        playerVisible = false
                        playerViewModel.clearVideo()
                    }
                    suppressMusicMiniAfterVideo = false
                    if (openMusicPlayerOnPlay.value) {
                        musicPlayerSheetState.expand()
                    } else if (musicPlayerSheetState.isDismissed) {
                        musicPlayerSheetState.collapse()
                    }
                }
            }
        val playOn = LocalPlayOn.current
        LaunchedEffect(playOn, onMusicStarted) { playOn?.musicStarted?.collect { onMusicStarted() } }
        VoxTubeMusicSheetEffects(
            musicPlayerSheetState = musicPlayerSheetState,
            currentMusicTrack = currentMusicTrack,
            activeVideoId = activeVideo?.id,
            currentRoute = currentRoute.value,
            showRestoredMusicMiniPlayer = showRestoredMusicMiniPlayer,
            openMusicPlayerRequest = openMusicPlayerRequest,
            suppressMusicMiniAfterVideo = { suppressMusicMiniAfterVideo = it },
            playerVisible = { playerVisible },
            collapseVideoPlayer = { playerSheetState.collapse() },
        )

        ApplyStatusBarStyle(
            themeMode = currentTheme,
            themeVariant = themeVariant,
            systemLightThemeMode = systemLightThemeMode,
            systemDarkThemeMode = systemDarkThemeMode,
            isFullscreen = false,
            preferences = preferences,
            musicPlayer = musicPlayerSheetState.takeIf { currentMusicTrack != null },
            isShortsPlayer = isShortsPlayerRoute,
            videoPlayerExpanded = playerVisible && playerSheetState.currentValue == PlayerSheetValue.Expanded,
            heroUnderStatusBar =
                statusBarHero.heroUnderStatusBar &&
                    !musicPlayerSheetState.isExpanded &&
                    playerSheetState.currentValue != PlayerSheetValue.Expanded,
            pageUnderStatusBar = statusBarHero.pageUnderStatusBar,
        )

        LaunchedEffect(isInPipMode) {
            if (!isInPipMode) return@LaunchedEffect
            navController.awaitGraph()
            val video = currentVideo
            val route = currentRoute.value
            if (route != SHORTS_ROUTE_KEY && !route.startsWith("player") && video != null) {
                navController.navigate("player/${video.id}")
            }
        }

        val remote = remoteSlot()
        val activeMiniPlayer =
            resolveActiveMiniPlayer(
                hasVideo = playerUiState.cachedVideo != null,
                videoVisible = playerVisible,
                videoInBackground = playerUiState.isBackgroundPlaybackMode,
                onShortsPlayer = isShortsPlayerRoute,
                hasMusic = currentMusicTrack != null,
                musicSuppressed = suppressMusicMiniAfterVideo,
                remoteSession = remote.session,
            )
        val isMusicSheetShown = activeMiniPlayer == ActiveMiniPlayer.Music
        val isPlayerCoveringContent =
            (playerVisible && playerSheetState.currentValue == PlayerSheetValue.Expanded) ||
                (isMusicSheetShown && musicPlayerSheetState.isExpanded)
        val barOnScreen = navigationBarShown(currentTab, currentEntry?.destination?.route, searchShowsNavBar, innerPagesShowNavBar)
        val showBottomNav = !isInPipMode && barOnScreen && !isPlayerCoveringContent
        // The video player covers the bar rather than removing it, so the bar can ride the collapse.
        val barComposed = !isInPipMode && barOnScreen && !(isMusicSheetShown && musicPlayerSheetState.isExpanded)
        val isBottomNavPresent = !usesNavigationRail && showBottomNav
        val isBottomNavShown = isBottomNavPresent && navScrollState.isBarVisible
        // The rail is hidden only where content goes truly full screen; the expanded players cover
        // it instead, so opening them never re-lays out the page beneath.
        val currentDestinationRoute = currentEntry?.destination?.route
        val isNavigationRailVisible =
            !isInPipMode &&
                needsOnboarding != null &&
                currentDestinationRoute != "onboarding" &&
                !(currentDestinationRoute == SHORTS_ROUTE_PATTERN && currentTab == null)
        val showVideoBar = activeMiniPlayer == ActiveMiniPlayer.VideoBar && !isInPipMode
        val showRemoteBar =
            !isInPipMode && remote.shows(activeMiniPlayer, activeVideo != null, musicPlayerSheetState.isDismissed, isShortsPlayerRoute)
        val isMiniBarObscuringContent =
            (isMusicSheetShown && !musicPlayerSheetState.isDismissed && !musicPlayerSheetState.isExpanded) || showVideoBar || showRemoteBar
        val (bottomInsets, miniPlayerBounds, miniPlayerDock) =
            rememberFlowBottomChrome(
                barPresent = isBottomNavPresent,
                barShown = isBottomNavShown,
                miniPlayerShown = isMiniBarObscuringContent,
                systemBottomPx = navBarBottomInset,
                barHeight = if (usesNavigationRail) 0.dp else navigationBarHeight,
                containerWidthPx = constraints.maxWidth.toFloat(),
                railWidth = if (usesNavigationRail && isNavigationRailVisible) navigationRailWidth else 0.dp,
                navScrollState = navScrollState,
                miniPlayerBesideShrunkBar =
                    bottomNavLook.style == NavBarStyle.FLOATING && bottomNavLook.scroll == NavBarScroll.SHRINK && bottomNavLook.miniBeside,
            )
        VoxTubeNavigationChrome(
            tabs = navigationTabs,
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                val action =
                    tabTapAction(
                        tab,
                        currentTab,
                        selectedTab,
                        searchIsTab = VoxTubeTab.Search in currentNavigationTabs,
                        startTab = defaultTab,
                    )
                if (action == TabTap.SCROLL_TO_TOP) {
                    TabScrollEventBus.emitScrollToTop(tab.route)
                } else {
                    currentRoute.value = tab.route
                    navController.tapTab(action, tab, selectedTab, defaultStartRoute)
                    if (action != TabTap.POP_TO_ROOT) navTransitions.markTabTap(navController.currentBackStackEntry?.id)
                }
            },
            barVisible = barComposed,
            barCovered = isPlayerCoveringContent,
            barCoveredFraction = { if (playerVisible) 1f - playerSheetState.fraction else 0f },
            barScrollState = navScrollState,
            barLook = bottomNavLook,
            railVisible = isNavigationRailVisible,
            onBarHeightChanged = { navigationBarHeight = it },
            onRailWidthChanged = { navigationRailWidth = it },
            glassBackdrop = glassBackdrop.takeIf { glassEnabled },
        ) {
            ProvideMusicPlaybackState(
                surfacesVisible = !musicPlayerSheetState.isExpanded && playerSheetState.currentValue != PlayerSheetValue.Expanded,
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor =
                        if (isInPipMode) {
                            androidx.compose.ui.graphics.Color.Black
                        } else {
                            androidx.compose.material3.MaterialTheme.colorScheme.background
                        },
                    // Pages run under the bottom chrome and clear it themselves through LocalFlowBottomInsets.
                    contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ) { paddingValues ->
                    val layoutDirection = LocalLayoutDirection.current
                    val contentPadding =
                        if (
                            isShortsPlayerRoute
                        ) {
                            PaddingValues(
                                start = paddingValues.calculateStartPadding(layoutDirection),
                                top = 0.dp,
                                end = paddingValues.calculateEndPadding(layoutDirection),
                                bottom = bottomInsets.systemBottom,
                            )
                        } else {
                            paddingValues
                        }
                    Box(
                        modifier =
                            Modifier
                                .padding(if (isInPipMode) PaddingValues(0.dp) else contentPadding)
                                .nestedScroll(navScrollState.nestedScrollConnection),
                    ) {
                        if (needsOnboarding != null) {
                            val homeViewModel: HomeViewModel = hiltViewModel(activity!!)
                            LaunchedEffect(homeViewModel) {
                                homeViewModel.initialize(context.applicationContext)
                            }

                            ProvideFlowGlobalActions(
                                unreadNotifications = notificationViewModel.unreadCount,
                                account = profileSwitchViewModel.topBarAccount,
                                onOpenNotifications = { navController.navigate("notifications") },
                                onOpenSettings = { navController.navigate("settings") },
                                profileAction = { withSettings ->
                                    ProfileTopBarAction(
                                        withSettings = withSettings,
                                        onOpenSettings = { navController.navigate("settings") },
                                        onOpenSettingsPage = mediaNavigator::openSettings,
                                        viewModel = profileSwitchViewModel,
                                    )
                                },
                            ) {
                                CompositionLocalProvider(
                                    *mediaNavigationLocals(mediaNavigator),
                                    LocalMediaOpenOrigins provides mediaOpenOrigins,
                                    LocalMusicMenus provides musicMenus,
                                    LocalEqualizerState provides equalizerViewModel.state,
                                    LocalFlowBottomInsets provides bottomInsets,
                                    LocalFlowSnackbarHostState provides snackbarHostState,
                                    LocalStatusBarHero provides statusBarHero,
                                    LocalNetworkOnline provides networkOnline,
                                ) {
                                    if (needsOnboarding == false) SubscriptionBellAlerts(snackbarHostState)
                                    LinkedSyncAlerts(snackbarHostState)
                                    SwitchRefusals(profileSwitchViewModel)
                                    PlayOnHost(onManageDevices = { mediaNavigator.openSettings(SettingsTarget(SettingsDestination.SYNC)) })
                                    VoxTubeSharedTransitions(enabled = !lowRamDevice) {
                                        NavHost(
                                            navController = navController,
                                            startDestination = if (needsOnboarding == true) "onboarding" else defaultStartRoute,
                                            enterTransition = navTransitions.enter,
                                            exitTransition = navTransitions.exit,
                                            popEnterTransition = navTransitions.popEnter,
                                            popExitTransition = navTransitions.popExit,
                                            predictivePopEnterTransition = navTransitions.predictivePopEnter,
                                            predictivePopExitTransition = navTransitions.predictivePopExit,
                                        ) {
                                            flowAppGraph(
                                                navController = navController,
                                                mediaNavigator = mediaNavigator,
                                                currentRoute = currentRoute,
                                                playerSheetState = playerSheetState,
                                                musicPlayerSheetState = musicPlayerSheetState,
                                                homeViewModel = homeViewModel,
                                                playerViewModel = playerViewModel,
                                                playerUiStateResult = playerUiStateResult,
                                                playerVisibleState = playerVisibleState,
                                                disableShortsPlayer = disableShortsPlayer,
                                                defaultStartRoute = defaultStartRoute,
                                                onMusicStarted = onMusicStarted,
                                                openDownloads = openDownloads,
                                                searchExitTab = { exitTab(currentNavigationTabs, VoxTubeTab.Search) },
                                                searchIsTab = { VoxTubeTab.Search in currentNavigationTabs },
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            VoxTubeLoadingIndicator()
                        }
                    }
                }
            }
        }

        val bottomPaddingTarget =
            if (isBottomNavShown) {
                navigationBarHeight + with(density) { navBarBottomInset.toDp() }
            } else {
                with(density) { navBarBottomInset.toDp() }
            }
        CompositionLocalProvider(
            LocalGlassBackdrop provides glassBackdrop.takeIf { glassEnabled },
            LocalLiquidGlassEnabled provides glassEnabled,
        ) {
            VoxTubePlayerOverlays(
                navController = navController,
                mediaNavigator = mediaNavigator,
                playerViewModel = playerViewModel,
                playerUiStateResult = playerUiStateResult,
                playerVisibleState = playerVisibleState,
                playerSheetState = playerSheetState,
                activeVideo = activeVideo,
                isShortsPlayerRoute = isShortsPlayerRoute,
                bottomPadding = bottomPaddingTarget,
                startInset = if (usesNavigationRail && isNavigationRailVisible) navigationRailWidth else 0.dp,
                miniDock = miniPlayerDock,
                miniPlayerScale = miniPlayerScale,
                miniPlayerShowSkipControls = miniPlayerShowSkipControls,
                miniPlayerShowNextPrevControls = miniPlayerShowNextPrevControls,
                showMusicSheet = isMusicSheetShown,
                showVideoBar = showVideoBar,
                showRemoteBar = showRemoteBar,
                musicPlayerSheetState = musicPlayerSheetState,
                containerWidth = maxWidth,
                containerHeight = with(density) { screenHeightPx.toDp() },
                miniBarBounds = miniPlayerBounds,
                musicMenus = musicMenus,
                equalizerState = equalizerViewModel.state,
                bottomInsets = bottomInsets,
                openOrigins = mediaOpenOrigins,
                snackbarHostState = snackbarHostState,
            )
        }

        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .offset {
                        val lift = bottomInsets.floatingBottomPx(this) + 12.dp.toPx()
                        IntOffset(0, -lift.roundToInt())
                    },
        )

        UpdateLaunchEffect(needsOnboarding = needsOnboarding) {
            navController.navigate(UPDATE_ROUTE) { launchSingleTop = true }
        }
        TimesUpLaunchEffect(enabled = needsOnboarding == false && !isInPipMode) {
            collapseExpandedPlayers()
            navController.navigate(TIMES_UP_ROUTE) { launchSingleTop = true }
        }
    }
}
