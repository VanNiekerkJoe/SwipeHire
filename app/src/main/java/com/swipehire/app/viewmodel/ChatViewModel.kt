package com.swipehire.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipehire.app.data.ChatMessage
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

    fun observeMessages(matchId: String, currentUserId: String) {
        viewModelScope.launch {
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