package com.swipehire.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.SettingsRepository
import com.swipehire.app.data.SettingsState
import com.swipehire.app.data.currentFirebaseUserId
import com.swipehire.app.data.remote.UserSettingsDto
import com.swipehire.app.data.repository.AppRepository
import com.swipehire.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val localRepository = SettingsRepository(application)
    private val appRepository = AppRepository()
    private val userId = currentFirebaseUserId("student_user")
    private val remoteSettings = MutableStateFlow(UserSettingsDto())

    val state: StateFlow<SettingsState> = combine(localRepository.state, remoteSettings) { local, remote ->
        local.copy(
            pushNotifications = remote.pushNotifications,
            matchAlerts = remote.matchAlerts,
            messageAlerts = remote.messageAlerts,
            profileVisible = remote.profileVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    init {
        viewModelScope.launch {
            appRepository.getUserSettings(userId)?.let { remoteSettings.value = it }
        }
    }

    fun setAccountType(type: AccountType) = viewModelScope.launch { localRepository.setAccountType(type) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { localRepository.setThemeMode(mode) }
    fun setBiometricLock(enabled: Boolean) = viewModelScope.launch { localRepository.setBiometricLock(enabled) }
    fun setOnboarded(done: Boolean) = viewModelScope.launch { localRepository.setOnboarded(done) }

    fun setPushNotifications(enabled: Boolean) = updateRemote { it.copy(pushNotifications = enabled) }
    fun setMatchAlerts(enabled: Boolean) = updateRemote { it.copy(matchAlerts = enabled) }
    fun setMessageAlerts(enabled: Boolean) = updateRemote { it.copy(messageAlerts = enabled) }
    fun setProfileVisible(visible: Boolean) = updateRemote { it.copy(profileVisible = visible) }

    private fun updateRemote(transform: (UserSettingsDto) -> UserSettingsDto) {
        val previous = remoteSettings.value
        val updated = transform(previous)
        remoteSettings.value = updated
        viewModelScope.launch {
            val persisted = appRepository.updateUserSettings(userId, updated)
            if (persisted != null) remoteSettings.value = persisted else remoteSettings.value = previous
        }
    }
}
