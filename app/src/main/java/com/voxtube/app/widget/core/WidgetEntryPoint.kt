package com.voxtube.app.widget.core

import android.content.Context
import com.voxtube.app.widget.core.refresh.WidgetContentSync
import com.voxtube.app.widget.downloads.DownloadsSource
import com.voxtube.app.widget.onrepeat.OnRepeatSource
import com.voxtube.app.widget.playlist.PlaylistWidgetSource
import com.voxtube.app.widget.recent.RecentlyPlayedSource
import com.voxtube.app.widget.week.WeekSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Glance widgets can't use constructor injection (the framework instantiates them),
 * so Hilt singletons are reached through this entry point.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun recentlyPlayedSource(): RecentlyPlayedSource

    fun downloadsSource(): DownloadsSource

    fun onRepeatSource(): OnRepeatSource

    fun playlistWidgetSource(): PlaylistWidgetSource

    fun weekSource(): WeekSource

    fun widgetContentSync(): WidgetContentSync
}

fun widgetEntryPoint(context: Context): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
