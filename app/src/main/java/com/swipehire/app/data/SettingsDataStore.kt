package com.swipehire.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.swipehire.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsStore by preferencesDataStore(name = "swipehire_settings")

/** Combined UI state. Account preferences are supplied from Firestore by SettingsViewModel. */
data class SettingsState(
    val accountType: AccountType = AccountType.STUDENT,
    val pushNotifications: Boolean = true,
    val matchAlerts: Boolean = true,
    val messageAlerts: Boolean = true,
    val profileVisible: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val biometricLock: Boolean = false,
    val onboarded: Boolean = false
)

private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val BIOMETRIC = booleanPreferencesKey("biometric_lock")
    val ONBOARDED = booleanPreferencesKey("onboarded")
}

class SettingsRepository(private val context: Context) {

    val state: Flow<SettingsState> = context.settingsStore.data.map { prefs ->
        SettingsState(
            themeMode = prefs[Keys.THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            biometricLock = prefs[Keys.BIOMETRIC] ?: false,
            onboarded = prefs[Keys.ONBOARDED] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.BIOMETRIC] = enabled }
    }

    suspend fun setOnboarded(done: Boolean) {
        context.settingsStore.edit { it[Keys.ONBOARDED] = done }
    }
}
