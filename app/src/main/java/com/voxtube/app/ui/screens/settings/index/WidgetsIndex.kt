package com.voxtube.app.ui.screens.settings.index

import com.voxtube.app.R
import com.voxtube.app.ui.components.settings.SettingEntry
import com.voxtube.app.ui.components.settings.SettingsDestination
import com.voxtube.app.widget.core.VoxTubeWidgetEntry
import com.voxtube.app.widget.core.VoxTubeWidgets

internal object WidgetsIndex {
    fun entryFor(widget: VoxTubeWidgetEntry) =
        SettingEntry(
            key = "widgets.${widget.id}",
            title = widget.label,
            summary = widget.description,
            section = R.string.settings_widgets_add_header,
            keywords = R.string.settings_keywords_widgets,
            destination = SettingsDestination.WIDGETS,
        )

    val all = VoxTubeWidgets.catalog.map(::entryFor)
}
