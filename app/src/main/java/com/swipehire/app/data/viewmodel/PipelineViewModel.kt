package com.swipehire.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.Interview
import com.swipehire.app.data.InterviewMode
import com.swipehire.app.data.PipelineStage
import com.swipehire.app.data.repository.PipelineRepository
import com.swipehire.app.util.InterviewRules
import com.swipehire.app.util.PipelineRules
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PipelineUiState(
    val role: AccountType? = null,
    val stage: PipelineStage = PipelineStage.MATCHED,
    val interviews: List<Interview> = emptyList(),
    val busy: Boolean = false,
    /** English message (a `tr()` key) for the last failed action, or null. */
    val error: String? = null
)

/** Drives the pipeline panel inside the chat screen. */
class PipelineViewModel(
    private val repository: PipelineRepository = PipelineRepository()
) : ViewModel() {

    private companion object { const val TAG = "PipelineVM" }

    private val _state = MutableStateFlow(PipelineUiState())
    val state: StateFlow<PipelineUiState> = _state.asStateFlow()

    private var observing: Job? = null
    private var observedMatchId: String? = null

    /** Starts (or restarts) live observation of [matchId]. Safe to call on every recomposition key change. */
    fun start(matchId: String, userId: String) {
        if (observedMatchId == matchId && observing?.isActive == true) return
        observing?.cancel()
        observedMatchId = matchId
        _state.value = PipelineUiState()
        observing = viewModelScope.launch {
            _state.update { it.copy(role = repository.getRole(userId)) }
            launch { repository.observeStage(matchId).collect { stage -> _state.update { it.copy(stage = stage) } } }
            launch {
                repository.observeInterviews(matchId).collect { list -> _state.update { it.copy(interviews = list) } }
            }
        }
        Log.d(TAG, "Observing pipeline for match $matchId")
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /** [onResult] receives an error key to show, or null on success. */
    fun propose(
        matchId: String, userId: String, startAtMillis: Long,
        durationMinutes: Int, mode: InterviewMode, note: String, onResult: (String?) -> Unit
    ) {
        val current = _state.value
        val validation = when {
            current.role != AccountType.COMPANY -> "Only the company can propose interview times"
            !PipelineRules.canSchedule(current.stage) -> "Interviews can't be scheduled at this stage"
            else -> InterviewRules.validate(startAtMillis, System.currentTimeMillis(), durationMinutes, current.interviews)
        }
        if (validation != null) { onResult(validation); return }
        launchAction("propose", onResult) {
            repository.proposeInterview(matchId, userId, startAtMillis, durationMinutes, mode, note)
        }
    }

    fun respond(matchId: String, interviewId: String, userId: String, accept: Boolean) =
        launchAction("respond") { repository.respondToInterview(matchId, interviewId, userId, accept) }

    fun cancel(matchId: String, interviewId: String, userId: String) =
        launchAction("cancel") { repository.cancelInterview(matchId, interviewId, userId) }

    fun moveTo(matchId: String, userId: String, newStage: PipelineStage) {
        val role = _state.value.role ?: return
        launchAction("moveTo") { repository.updateStage(matchId, userId, role, newStage) }
    }

    private fun launchAction(action: String, onResult: ((String?) -> Unit)? = null, block: suspend () -> Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            val ok = block()
            val message = if (ok) null else "Something went wrong. Please try again."
            if (!ok) Log.w(TAG, "$action failed")
            _state.update { it.copy(busy = false, error = if (onResult == null) message else null) }
            onResult?.invoke(message)
        }
    }
}
