package com.voxtube.app.ui.screens.settings.security

import com.voxtube.app.applock.AppLock
import com.voxtube.app.applock.AppLockSettings
import com.voxtube.app.data.local.PlayerPreferences
import com.voxtube.app.ui.screens.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel
    @Inject
    constructor(
        val appLock: AppLock,
        private val preferences: PlayerPreferences,
    ) : SettingsViewModel() {
        val settings: StateFlow<AppLockSettings?> = appLock.settings

        fun isDeviceSecure(): Boolean = appLock.isDeviceSecure()

        fun setEnabled(enabled: Boolean) = write { preferences.setAppLockEnabled(enabled) }

        fun setDelayMinutes(minutes: Int) = write { preferences.setAppLockDelayMinutes(minutes) }

        fun setHideInRecents(hide: Boolean) = write { preferences.setAppLockHideInRecents(hide) }
    }
