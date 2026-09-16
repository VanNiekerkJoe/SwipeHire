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

/** Everything the Settings screen (20-mark deliverable) lets a user change and persist. */
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
    val ACCOUNT_TYPE = stringPreferencesKey("account_type")
    val PUSH = booleanPreferencesKey("push_notifications")
    val MATCH_ALERTS = booleanPreferencesKey("match_alerts")
    val MESSAGE_ALERTS = booleanPreferencesKey("message_alerts")
    val PROFILE_VISIBLE = booleanPreferencesKey("profile_visible")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val BIOMETRIC = booleanPreferencesKey("biometric_lock")
    val ONBOARDED = booleanPreferencesKey("onboarded")
}

class SettingsRepository(private val context: Context) {

    val state: Flow<SettingsState> = context.settingsStore.data.map { prefs ->
        SettingsState(
            accountType = prefs[Keys.ACCOUNT_TYPE]?.let { AccountType.valueOf(it) } ?: AccountType.STUDENT,
            pushNotifications = prefs[Keys.PUSH] ?: true,
            matchAlerts = prefs[Keys.MATCH_ALERTS] ?: true,
            messageAlerts = prefs[Keys.MESSAGE_ALERTS] ?: true,
            profileVisible = prefs[Keys.PROFILE_VISIBLE] ?: true,
            themeMode = prefs[Keys.THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            biometricLock = prefs[Keys.BIOMETRIC] ?: false,
            onboarded = prefs[Keys.ONBOARDED] ?: false
        )
    }

    suspend fun setAccountType(type: AccountType) {
        context.settingsStore.edit { it[Keys.ACCOUNT_TYPE] = type.name }
    }

    suspend fun setPushNotifications(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.PUSH] = enabled }
    }

    suspend fun setMatchAlerts(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.MATCH_ALERTS] = enabled }
    }

    suspend fun setMessageAlerts(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.MESSAGE_ALERTS] = enabled }
    }

    suspend fun setProfileVisible(visible: Boolean) {
        context.settingsStore.edit { it[Keys.PROFILE_VISIBLE] = visible }
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
