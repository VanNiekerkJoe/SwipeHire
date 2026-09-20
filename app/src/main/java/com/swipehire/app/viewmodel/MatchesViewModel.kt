package com.swipehire.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipehire.app.data.MatchChat
import com.swipehire.app.data.currentFirebaseUserId
import com.swipehire.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class MatchesViewModel(private val repository: AppRepository = AppRepository()) : ViewModel() {
    private val _matches = MutableStateFlow<List<MatchChat>>(emptyList())
    val matches: StateFlow<List<MatchChat>> = _matches.asStateFlow()
    private var matchJob: Job? = null

    init { refresh() }

    fun refresh() {
        matchJob?.cancel()
        val userId = currentFirebaseUserId() ?: run { _matches.value = emptyList(); return }
        matchJob = viewModelScope.launch {
            repository.getMatches(userId).collect { _matches.value = it }
        }
    }
}
