package com.swipehire.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipehire.app.data.AppAlert
import com.swipehire.app.data.currentFirebaseUserId
import com.swipehire.app.data.repository.AppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertsViewModel(private val repository: AppRepository = AppRepository()) : ViewModel() {
    private val _alerts = MutableStateFlow<List<AppAlert>>(emptyList())
    val alerts: StateFlow<List<AppAlert>> = _alerts.asStateFlow()
    private var listenerJob: Job? = null

    fun refresh() {
        listenerJob?.cancel()
        val userId = currentFirebaseUserId() ?: run {
            _alerts.value = emptyList()
            return
        }
        listenerJob = viewModelScope.launch {
            repository.getAlerts(userId).collect { _alerts.value = it }
        }
    }

    fun markRead(alertId: String) {
        val userId = currentFirebaseUserId() ?: return
        viewModelScope.launch { repository.markAlertRead(userId, alertId) }
    }
}
