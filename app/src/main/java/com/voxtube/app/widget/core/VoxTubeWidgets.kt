package com.voxtube.app.widget.core

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.voxtube.app.BuildConfig
import com.voxtube.app.R
import com.voxtube.app.widget.core.theme.lastWidgetPreviewStamp
import com.voxtube.app.widget.core.theme.lastWidgetThemeSignature
import com.voxtube.app.widget.core.theme.widgetThemeSignatureFlow
import com.voxtube.app.widget.core.theme.writeWidgetPreviewStamp
import com.voxtube.app.widget.core.theme.writeWidgetThemeSignature
import com.voxtube.app.widget.downloads.DownloadsWidget
import com.voxtube.app.widget.downloads.DownloadsWidgetReceiver
import com.voxtube.app.widget.nowplaying.NowPlayingWidget
import com.voxtube.app.widget.nowplaying.NowPlayingWidgetReceiver
import com.voxtube.app.widget.onrepeat.OnRepeatWidget
import com.voxtube.app.widget.onrepeat.OnRepeatWidgetReceiver
import com.voxtube.app.widget.playlist.PlaylistWidget
import com.voxtube.app.widget.playlist.PlaylistWidgetReceiver
import com.voxtube.app.widget.quickactions.QuickActionsWidget
import com.voxtube.app.widget.quickactions.QuickActionsWidgetReceiver
import com.voxtube.app.widget.recent.RecentlyPlayedWidget
import com.voxtube.app.widget.recent.RecentlyPlayedWidgetReceiver
import com.voxtube.app.widget.recognize.RecognizeWidget
import com.voxtube.app.widget.recognize.RecognizeWidgetReceiver
import com.voxtube.app.widget.turntable.TurntableWidget
import com.voxtube.app.widget.turntable.TurntableWidgetReceiver
import com.voxtube.app.widget.week.WeekWidget
import com.voxtube.app.widget.week.WeekWidgetReceiver
import kotlin.reflect.KClass

/** Registry of every Flow widget: re-renders them on theme changes, publishes previews, lists them in Settings. */
object VoxTubeWidgets {
    /**
     * Re-renders every placed widget whenever the app's palette actually changes.
     *
     * The baseline is persisted rather than positional. Glance turns each `updateAll` into one
     * `SessionWorker` per placed instance, and a session lives about fifty seconds registering a
     * global snapshot write observer that taxes every frame the app draws in that window — so a
     * launch that re-renders widgets for nothing is expensive, and ten to twenty of them was the
     * measured cost of a `drop(1)` that could only ever mean "first value I have seen this
     * process", never "same as last time".
     *
     * Persisting it also closes the opposite hole: a theme change made while this Activity is not
     * alive (the TV appearance settings) used to be swallowed by that same `drop(1)`.
     */
    suspend fun observeThemeChanges(context: Context) {
        val appContext = context.applicationContext
        var lastSignature = appContext.lastWidgetThemeSignature()
        widgetThemeSignatureFlow(appContext).collect { signature ->
            val persisted = signature.persistedForm()
            publishPreviewsIfStale(appContext, persisted)
            if (persisted == lastSignature) return@collect
            lastSignature = persisted
            appContext.writeWidgetThemeSignature(persisted)
            updateAll(appContext)
        }
    }

    suspend fun updateAll(
        context: Context,
        except: Set<KClass<out GlanceAppWidget>> = emptySet(),
    ) {
        catalog.map { it.widget() }.filterNot { it::class in except }.forEach { it.updateAll(context) }
    }

    /**
     * Re-renders the picker previews (Android 15+) after an update or a palette change. The platform
     * rate-limits the call, so it runs once per version and theme, never on every launch.
     */
    private suspend fun publishPreviewsIfStale(
        context: Context,
        themeSignature: String,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val stamp = "${BuildConfig.VERSION_CODE}|$themeSignature"
        if (context.lastWidgetPreviewStamp() == stamp) return
        val manager = GlanceAppWidgetManager(context)
        // Some profiles, such as a Private Space, reject our own providers as invalid (#1357), and
        // Glance passes that binder exception straight through.
        val published =
            try {
                catalog.all { entry ->
                    manager.setWidgetPreviews(entry.receiver) == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS
                }
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Widget previews rejected: ${e.message}")
                false
            }
        Log.i(TAG, "Widget previews published: $published")
        if (published) context.writeWidgetPreviewStamp(stamp)
    }

    private const val TAG = "VoxTubeWidgets"

    val catalog: List<VoxTubeWidgetEntry> =
        listOf(
            VoxTubeWidgetEntry(
                id = "now_playing",
                receiver = NowPlayingWidgetReceiver::class,
                widget = ::NowPlayingWidget,
                label = R.string.widget_now_playing_label,
                description = R.string.widget_now_playing_description,
                icon = R.drawable.ic_widget_music,
            ),
            VoxTubeWidgetEntry(
                id = "turntable",
                receiver = TurntableWidgetReceiver::class,
                widget = ::TurntableWidget,
                label = R.string.widget_turntable_label,
                description = R.string.widget_turntable_description,
                icon = R.drawable.ic_music_note,
            ),
            VoxTubeWidgetEntry(
                id = "on_repeat",
                receiver = OnRepeatWidgetReceiver::class,
                widget = ::OnRepeatWidget,
                label = R.string.widget_on_repeat_label,
                description = R.string.widget_on_repeat_description,
                icon = R.drawable.ic_repeat,
            ),
            VoxTubeWidgetEntry(
                id = "playlist",
                receiver = PlaylistWidgetReceiver::class,
                widget = ::PlaylistWidget,
                label = R.string.widget_playlist_label,
                description = R.string.widget_playlist_description,
                icon = R.drawable.ic_widget_library,
            ),
            VoxTubeWidgetEntry(
                id = "week",
                receiver = WeekWidgetReceiver::class,
                widget = ::WeekWidget,
                label = R.string.widget_week_label,
                description = R.string.widget_week_description,
                icon = R.drawable.ic_widget_history,
            ),
            VoxTubeWidgetEntry(
                id = "continue_watching",
                receiver = RecentlyPlayedWidgetReceiver::class,
                widget = ::RecentlyPlayedWidget,
                label = R.string.widget_recently_played_label,
                description = R.string.widget_recently_played_description,
                icon = R.drawable.ic_widget_history,
            ),
            VoxTubeWidgetEntry(
                id = "downloads",
                receiver = DownloadsWidgetReceiver::class,
                widget = ::DownloadsWidget,
                label = R.string.widget_downloads_label,
                description = R.string.widget_downloads_description,
                icon = R.drawable.ic_widget_download,
            ),
            VoxTubeWidgetEntry(
                id = "quick_actions",
                receiver = QuickActionsWidgetReceiver::class,
                widget = ::QuickActionsWidget,
                label = R.string.widget_quick_actions_label,
                description = R.string.widget_quick_actions_description,
                icon = R.drawable.ic_widget_search,
            ),
            VoxTubeWidgetEntry(
                id = "recognize",
                receiver = RecognizeWidgetReceiver::class,
                widget = ::RecognizeWidget,
                label = R.string.widget_recognize_label,
                description = R.string.widget_recognize_description,
                icon = R.drawable.ic_widget_mic,
            ),
        )
}

/** One widget as Settings lists it: what to pin, and how to describe it. */
class VoxTubeWidgetEntry(
    val id: String,
    val receiver: KClass<out GlanceAppWidgetReceiver>,
    val widget: () -> GlanceAppWidget,
    @StringRes val label: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
)
