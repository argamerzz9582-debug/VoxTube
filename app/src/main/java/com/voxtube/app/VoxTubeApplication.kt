package com.voxtube.app

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.material3.ComposeMaterial3Flags
import androidx.compose.material3.ExperimentalMaterial3Api
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.voxtube.app.applock.AppLock
import com.voxtube.app.data.contentfilter.aislist.AiListRepository
import com.voxtube.app.data.local.PlayerPreferences
import com.voxtube.app.data.local.SubscriptionRepository
import com.voxtube.app.data.music.MusicCache
import com.voxtube.app.data.profile.ActiveProfile
import com.voxtube.app.data.profile.PendingSlice
import com.voxtube.app.data.profile.PostSwitchWork
import com.voxtube.app.data.profile.ProfileFiles
import com.voxtube.app.data.profile.ProfileSlices
import com.voxtube.app.data.profile.ProfileWebView
import com.voxtube.app.data.profile.ProfileWorkReconciler
import com.voxtube.app.data.recommendation.VoxTubeNeuroEngine
import com.voxtube.app.data.repository.YouTubePlaylistRepository
import com.voxtube.app.data.repository.YouTubeRepository
import com.voxtube.app.discord.DiscordPresenceRuntime
import com.voxtube.app.innertube.YouTube
import com.voxtube.app.network.AppProxyManager
import com.voxtube.app.network.VpnStateMonitor
import com.voxtube.app.notification.AutoBackupWorker
import com.voxtube.app.notification.BackgroundCheckScheduler
import com.voxtube.app.notification.BreakReminderTimer
import com.voxtube.app.notification.DailyLimitGuard
import com.voxtube.app.notification.NotificationHelper
import com.voxtube.app.platform.memory.HeapSampler
import com.voxtube.app.player.GlobalPlayerState
import com.voxtube.app.player.PlaybackShutdown
import com.voxtube.app.player.SleepTimerManager
import com.voxtube.app.sync2.service.LinkedCatchUp
import com.voxtube.app.sync2.service.LinkedForeground
import com.voxtube.app.sync2.service.LinkedProfiles
import com.voxtube.app.sync2.service.LinkedSync
import com.voxtube.app.ui.screens.home.HomeFeedLanguageReset
import com.voxtube.app.utils.AppLanguageManager
import com.voxtube.app.utils.PerformanceDispatcher
import com.voxtube.app.utils.VoxTubeCrashHandler
import com.voxtube.app.utils.cipher.PipePipeNsigDecoder
import com.voxtube.app.utils.contentYouTubeLocale
import com.voxtube.app.utils.potoken.VisitorIdentityStore
import com.voxtube.app.utils.potoken.WebPoTokenSession
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class VoxTubeApplication :
    Application(),
    SingletonImageLoader.Factory {
    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var playlistRepository: YouTubePlaylistRepository

    @Inject
    lateinit var vpnStateMonitor: VpnStateMonitor

    @Inject
    lateinit var visitorIdentityStore: VisitorIdentityStore

    @Inject
    internal lateinit var homeFeedLanguageReset: HomeFeedLanguageReset

    @Inject
    lateinit var profileWorkReconciler: ProfileWorkReconciler

    @Inject
    lateinit var postSwitchWork: PostSwitchWork

    @Inject
    lateinit var profileSlices: dagger.Lazy<ProfileSlices>

    @Inject
    lateinit var linkedForeground: LinkedForeground

    @Inject
    lateinit var breakReminderTimer: BreakReminderTimer

    @Inject
    lateinit var dailyLimitGuard: DailyLimitGuard

    @Inject
    lateinit var appLock: AppLock

    @Inject
    lateinit var linkedProfiles: dagger.Lazy<LinkedProfiles>

    @Inject
    lateinit var linkedSync: dagger.Lazy<LinkedSync>

    @Inject
    lateinit var aiList: AiListRepository

    @Inject
    lateinit var heapSampler: HeapSampler

    @Inject
    lateinit var youTubeRepository: dagger.Lazy<YouTubeRepository>

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader

    companion object {
        private const val TAG = "VoxTubeApplication"
        lateinit var appContext: Context
            private set
    }

    override fun attachBaseContext(base: Context) {
        ActiveProfile.attach(base)
        super.attachBaseContext(AppLanguageManager.wrapContext(base, AppLanguageManager.resolveLanguageTag(base)))
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate() {
        ProfileWebView.applySuffix(this)
        super.onCreate()
        // A sheet re-anchored mid-animation (a menu that became shorter than half the window) throws
        // AnchoredDraggableUninitializedException with this on; off, M3 places it a frame later (#2065).
        ComposeMaterial3Flags.isAnchoredDraggableComponentsStrictOffsetCheckEnabled = false
        appContext = applicationContext
        linkedForeground.install()
        breakReminderTimer.install()
        dailyLimitGuard.install()
        appLock.install()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            // Left by the InnerTube HTTP cache of older builds; nothing writes to it any more.
            cacheDir.resolve("innertube_http_cache").deleteRecursively()
        }

        DiscordPresenceRuntime.initialize(this, okHttpClient)

        val playerPreferences = PlayerPreferences(this)

        // Install crash handler for real-time monitoring
        VoxTubeCrashHandler.install(this)
        heapSampler.install()

        try {
            com.voxtube.app.utils.cipher.CipherDeobfuscator
                .initialize(this)
            Log.d(TAG, "CipherDeobfuscator initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CipherDeobfuscator", e)
        }

        PipePipeNsigDecoder.initialize(this)

        // Initialize notification channels
        NotificationHelper.createNotificationChannels(this)
        Log.d(TAG, "Notification channels created")

        /*
        try {
            // Initialize YoutubeDL
            com.yausername.youtubedl_android.YoutubeDL.getInstance().init(this)
            Log.d(TAG, "YoutubeDL initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize YoutubeDL", e)
        }
         */

        PendingSlice.start(ProfileFiles.noBackupRoot(this), CoroutineScope(SupervisorJob() + Dispatchers.IO)) { file ->
            profileSlices.get().applyPending(file)
        }
        LinkedCatchUp.start(CoroutineScope(SupervisorJob() + Dispatchers.IO)) {
            PendingSlice.await()
            if (linkedProfiles.get().catchingUp()) linkedSync.get().syncNow()
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            VoxTubeNeuroEngine.getInstance(this@VoxTubeApplication).aiListMatcher = aiList::currentMatcher
            launch { profileWorkReconciler.reconcile() }
            launch { AutoBackupWorker.keepScheduled(this@VoxTubeApplication) }
            launch { postSwitchWork.runIfSwitched() }
            BackgroundCheckScheduler.follow(this@VoxTubeApplication, playerPreferences)
        }
        SleepTimerManager.attachExitCallback { PlaybackShutdown.closeApp(this) }

        val warmups = StartupWarmups(this)
        warmups.startWithFirstActivity()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            playerPreferences.proxyConfig
                .distinctUntilChanged()
                .flatMapLatest { config ->
                    if (config.watchesVpn()) {
                        vpnStateMonitor.vpnActive().map { vpnActive -> config to vpnActive }
                    } else {
                        flowOf(config to false)
                    }
                }.collectLatest { (proxyConfig, vpnActive) ->
                    applyProxyConfig(proxyConfig, vpnActive)
                    warmups.onProxyApplied()
                }
        }

        YouTube.onVisitorDataChanged = visitorIdentityStore::save
        WebPoTokenSession.bindIdentityStore(visitorIdentityStore)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                visitorIdentityStore.restore()?.let { restored ->
                    YouTube.visitorData = restored
                    Log.d(TAG, "visitorData restored")
                }
            } catch (e: Exception) {
                Log.w(TAG, "visitorData restore error: ${e.message}")
            }
            warmups.onIdentityRestored()
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            AppLanguageManager.appliedLanguage.filterNotNull().collect { tag ->
                if (playerPreferences.appLanguage.first() != tag) playerPreferences.setAppLanguage(tag)
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            combine(
                AppLanguageManager.appliedLanguage.filterNotNull(),
                playerPreferences.contentLanguage,
                playerPreferences.trendingRegion,
            ) { appLanguage, contentLanguage, region ->
                contentYouTubeLocale(appLanguage, contentLanguage, region, AppLanguageManager.deviceLocale(this@VoxTubeApplication))
            }.distinctUntilChanged().collectLatest { newLocale ->
                YouTube.locale = newLocale
                Log.d(TAG, "Dynamic YouTube Locale updated: gl=${newLocale.gl}, hl=${newLocale.hl}")
                homeFeedLanguageReset.onContentLanguage(newLocale.hl)
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = SubscriptionRepository.getInstance(this@VoxTubeApplication)
                val youtubeRepository = YouTubeRepository.getInstance(playerPreferences)
                val repaired =
                    repository.repairVideoThumbnailSubscriptions { channelId ->
                        // Startup's own fetches hold the InnerTube connections for a while; nothing waits on this.
                        withTimeoutOrNull(20_000L) {
                            youtubeRepository.fetchChannelAvatarById(channelId)
                        }.orEmpty()
                    }
                if (repaired > 0) {
                    Log.i(TAG, "Repaired $repaired subscription thumbnails")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Subscription thumbnail repair failed: ${e.message}")
            }
        }
    }

    private fun applyProxyConfig(
        config: com.voxtube.app.network.AppProxyConfig,
        vpnActive: Boolean,
    ) {
        AppProxyManager.update(config, vpnActive)
        YouTube.proxy = AppProxyManager.currentProxy()
        YouTube.proxyAuth = AppProxyManager.currentHttpProxyAuthorizationHeader()
    }

    override fun onTerminate() {
        DiscordPresenceRuntime.shutdown()
        super.onTerminate()
        // Clean up performance dispatcher resources
        PerformanceDispatcher.shutdown()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        VoxTubeCrashHandler.recordPhase("memory", "VoxTubeApplication.onLowMemory")
        releaseVolatileMemory()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        VoxTubeCrashHandler.recordPhase("memory", "VoxTubeApplication.onTrimMemory level=$level")
        if (AppMemoryTrim.releasesRefetchableCaches(level)) {
            releaseRefetchableCaches()
        }
        if (AppMemoryTrim.releasesVolatileMemory(level)) {
            releaseVolatileMemory()
        }
    }

    private fun releaseRefetchableCaches() {
        if (::youTubeRepository.isInitialized) {
            youTubeRepository.get().trimWatchCache(keepVideoId = GlobalPlayerState.currentVideo.value?.id)
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { MusicCache.clearAll() }
    }

    private fun releaseVolatileMemory() {
        if (::playlistRepository.isInitialized) playlistRepository.trimMemory()
        if (::youTubeRepository.isInitialized) {
            val repository = youTubeRepository.get()
            repository.trimWatchCache(keepVideoId = GlobalPlayerState.currentVideo.value?.id)
            repository.trimMetadataCaches()
        }
        YouTube.trimMemory()
    }
}
