package com.swipehire.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipehire.app.data.ChatMessage
import com.swipehire.app.data.MatchChat
import com.swipehire.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: AppRepository = AppRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    private val _match = MutableStateFlow<MatchChat?>(null)
    val match: StateFlow<MatchChat?> = _match.asStateFlow()
    private val _matchResolved = MutableStateFlow(false)
    val matchResolved: StateFlow<Boolean> = _matchResolved.asStateFlow()

    fun observeMessages(matchId: String, currentUserId: String) {
        viewModelScope.launch {
            _matchResolved.value = false
            _match.value = repository.getMatch(matchId, currentUserId)
            _matchResolved.value = true
            if (_match.value == null) return@launch
            repository.getLiveMessages(matchId, currentUserId).collect { liveMessages ->
                _messages.value = liveMessages
            }
        }
    }

    fun sendMessage(matchId: String, senderId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(matchId, senderId, text)
        }
    }
}
