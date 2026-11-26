package com.voxtube.app.ui.screens.settings.appearance

import android.net.Uri
import com.voxtube.app.data.local.AppFontPreferences
import com.voxtube.app.data.local.AppFontSelection
import com.voxtube.app.data.local.FontImportResult
import com.voxtube.app.ui.screens.settings.SettingsViewModel
import com.voxtube.app.ui.theme.AppFont
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class FontViewModel
    @Inject
    constructor(
        private val preferences: AppFontPreferences,
    ) : SettingsViewModel() {
        val selection = preferences.selection.asState(AppFontSelection())

        private val _importResult = MutableStateFlow<FontImportResult?>(null)

        /** What the last file pick did, until the page has said so. */
        val importResult: StateFlow<FontImportResult?> = _importResult.asStateFlow()

        fun select(font: AppFont) = write { preferences.setFont(font) }

        fun import(uri: Uri) =
            write {
                val result = preferences.customFonts.import(uri)
                if (result is FontImportResult.Imported) preferences.setCustomFont(result.displayName)
                _importResult.value = result
            }

        fun consumeImportResult() {
            _importResult.value = null
        }
    }
