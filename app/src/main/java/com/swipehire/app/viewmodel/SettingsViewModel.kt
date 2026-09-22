package com.swipehire.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.AppLanguage
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
import com.google.firebase.auth.FirebaseAuth

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val localRepository = SettingsRepository(application)
    private val appRepository = AppRepository()
    private val remoteSettings = MutableStateFlow(UserSettingsDto())
    private val remoteAccountType = MutableStateFlow<AccountType?>(null)

    val state: StateFlow<SettingsState> = combine(localRepository.state, remoteSettings, remoteAccountType) { local, remote, role ->
        local.copy(
            accountType = role ?: local.accountType,
            pushNotifications = remote.pushNotifications,
            matchAlerts = remote.matchAlerts,
            messageAlerts = remote.messageAlerts,
            profileVisible = remote.profileVisible,
            language = local.language
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    init {
        refreshSession()
    }

    fun refreshSession() = viewModelScope.launch {
        val userId = currentFirebaseUserId() ?: return@launch
        remoteAccountType.value = appRepository.getAccountType(userId)
        appRepository.getUserSettings(userId)?.let {
            remoteSettings.value = it
            localRepository.setLanguage(AppLanguage.fromCode(it.language))
        }
    }

    fun chooseAccountType(type: AccountType, onSaved: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val userId = currentFirebaseUserId()
        val saved = userId != null && appRepository.setAccountType(userId, type)
        if (saved) {
            remoteAccountType.value = type
            localRepository.setOnboarded(true)
        }
        onSaved(saved)
    }

    fun resolveSignedInAccount(onResolved: (AccountType?) -> Unit) = viewModelScope.launch {
        val userId = currentFirebaseUserId()
        val role = userId?.let { appRepository.getAccountType(it) }
        if (role != null) {
            remoteAccountType.value = role
            localRepository.setOnboarded(true)
        }
        onResolved(role)
    }

    fun logOut() = viewModelScope.launch {
        FirebaseAuth.getInstance().signOut()
        remoteAccountType.value = null
        remoteSettings.value = UserSettingsDto()
        localRepository.setOnboarded(false)
    }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { localRepository.setThemeMode(mode) }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        localRepository.setLanguage(language)
        val userId = currentFirebaseUserId() ?: return@launch
        val previous = remoteSettings.value
        val updated = previous.copy(language = language.code)
        remoteSettings.value = updated
        if (appRepository.updateUserSettings(userId, updated) == null) remoteSettings.value = previous
    }
    fun setBiometricLock(enabled: Boolean) = viewModelScope.launch { localRepository.setBiometricLock(enabled) }
    fun setOnboarded(done: Boolean) = viewModelScope.launch { localRepository.setOnboarded(done) }

    fun setPushNotifications(enabled: Boolean) = updateRemote { it.copy(pushNotifications = enabled) }
    fun setMatchAlerts(enabled: Boolean) = updateRemote { it.copy(matchAlerts = enabled) }
    fun setMessageAlerts(enabled: Boolean) = updateRemote { it.copy(messageAlerts = enabled) }
    fun setProfileVisible(visible: Boolean) = updateRemote { it.copy(profileVisible = visible) }

    private fun updateRemote(transform: (UserSettingsDto) -> UserSettingsDto) {
        val userId = currentFirebaseUserId() ?: return
        val previous = remoteSettings.value
        val updated = transform(previous)
        remoteSettings.value = updated
        viewModelScope.launch {
            val persisted = appRepository.updateUserSettings(userId, updated)
            if (persisted != null) remoteSettings.value = persisted else remoteSettings.value = previous
        }
    }
}
