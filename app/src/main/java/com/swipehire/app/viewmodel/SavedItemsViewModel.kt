package com.swipehire.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipehire.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Firestore-backed shortlist exposed through the custom REST API. */
class SavedItemsViewModel(
    private val repository: AppRepository = AppRepository()
) : ViewModel() {
    private val _savedJobIds = MutableStateFlow<Set<String>>(emptySet())
    val savedJobIds: StateFlow<Set<String>> = _savedJobIds.asStateFlow()

    private val _savedStudentIds = MutableStateFlow<Set<String>>(emptySet())
    val savedStudentIds: StateFlow<Set<String>> = _savedStudentIds.asStateFlow()

    private var userId: String = ""

    fun load(userId: String) {
        if (this.userId == userId && (_savedJobIds.value.isNotEmpty() || _savedStudentIds.value.isNotEmpty())) return
        this.userId = userId
        viewModelScope.launch {
            repository.getSavedItems(userId)?.let { saved ->
                _savedJobIds.value = saved.savedJobIds.toSet()
                _savedStudentIds.value = saved.savedStudentIds.toSet()
            }
        }
    }

    fun toggleJob(id: String) = setSaved("job", id, id !in _savedJobIds.value)
    fun toggleStudent(id: String) = setSaved("student", id, id !in _savedStudentIds.value)

    private fun setSaved(kind: String, id: String, saved: Boolean) {
        if (userId.isBlank()) return
        if (kind == "job") _savedJobIds.value = _savedJobIds.value.withItem(id, saved)
        else _savedStudentIds.value = _savedStudentIds.value.withItem(id, saved)

        viewModelScope.launch {
            val remote = repository.setSavedItem(userId, kind, id, saved)
            if (remote != null) {
                _savedJobIds.value = remote.savedJobIds.toSet()
                _savedStudentIds.value = remote.savedStudentIds.toSet()
            } else {
                // Roll back optimistic UI when the API cannot persist the change.
                if (kind == "job") _savedJobIds.value = _savedJobIds.value.withItem(id, !saved)
                else _savedStudentIds.value = _savedStudentIds.value.withItem(id, !saved)
            }
        }
    }

    private fun Set<String>.withItem(id: String, included: Boolean): Set<String> =
        if (included) this + id else this - id
}
