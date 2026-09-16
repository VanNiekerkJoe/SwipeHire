package com.swipehire.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.SettingsRepository
import com.swipehire.app.data.SettingsState
import com.swipehire.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application)

    val state: StateFlow<SettingsState> = repo.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    fun setAccountType(type: AccountType) = viewModelScope.launch { repo.setAccountType(type) }
    fun setPushNotifications(enabled: Boolean) = viewModelScope.launch { repo.setPushNotifications(enabled) }
    fun setMatchAlerts(enabled: Boolean) = viewModelScope.launch { repo.setMatchAlerts(enabled) }
    fun setMessageAlerts(enabled: Boolean) = viewModelScope.launch { repo.setMessageAlerts(enabled) }
    fun setProfileVisible(visible: Boolean) = viewModelScope.launch { repo.setProfileVisible(visible) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setBiometricLock(enabled: Boolean) = viewModelScope.launch { repo.setBiometricLock(enabled) }
    fun setOnboarded(done: Boolean) = viewModelScope.launch { repo.setOnboarded(done) }
}
