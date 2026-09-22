package com.swipehire.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.JobPosting
import com.swipehire.app.data.StudentProfile
import com.swipehire.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val repository: AppRepository = AppRepository()
) : ViewModel() {

    private val _jobStack = MutableStateFlow<List<JobPosting>>(emptyList())
    val jobStack: StateFlow<List<JobPosting>> = _jobStack.asStateFlow()

    private val _studentStack = MutableStateFlow<List<StudentProfile>>(emptyList())
    val studentStack: StateFlow<List<StudentProfile>> = _studentStack.asStateFlow()

    private val _swipedTargetIds = MutableStateFlow<Set<String>>(emptySet())
    val swipedTargetIds: StateFlow<Set<String>> = _swipedTargetIds.asStateFlow()
    private val swipeJobs = mutableMapOf<String, Job>()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadCards(accountType: AccountType, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _swipedTargetIds.value = repository.getSwipedTargetIds(userId)
            if (accountType == AccountType.STUDENT) {
                // Fetch job postings from the C# REST API
                val apiJobs = repository.getJobsFromApi()
                _jobStack.value = if (apiJobs.isNotEmpty()) {
                    apiJobs
                } else {
                    // Fallback to Firestore if API returns empty
                    repository.getJobPostings()
                }
            } else {
                // Fetch student profiles from the C# REST API
                val apiStudents = repository.getStudentsFromApi()
                _studentStack.value = if (apiStudents.isNotEmpty()) {
                    apiStudents
                } else {
                    // Fallback to Firestore if API returns empty
                    repository.getStudentProfiles()
                }
            }
            _isLoading.value = false
        }
    }

    fun onSwipe(
        userId: String,
        targetId: String,
        targetUserId: String,
        isLike: Boolean,
        onMatchFound: (String?) -> Unit
    ) {
        if (targetId in _swipedTargetIds.value) return
        _swipedTargetIds.value = _swipedTargetIds.value + targetId

        val job = viewModelScope.launch {
            val result = repository.recordSwipe(userId, targetId, targetUserId, isLike)
            if (result == null) {
                // Persistence failed, so allow the user to make the decision again.
                _swipedTargetIds.value = _swipedTargetIds.value - targetId
            }
            onMatchFound(result?.matchId?.takeIf { result.isMatch })
        }
        swipeJobs[targetId] = job
        job.invokeOnCompletion {
            if (swipeJobs[targetId] === job) swipeJobs.remove(targetId)
        }
    }

    fun undoSwipe(userId: String, targetId: String, targetUserId: String) {
        val pendingWrite = swipeJobs[targetId]
        viewModelScope.launch {
            pendingWrite?.join()
            _swipedTargetIds.value = _swipedTargetIds.value - targetId
            if (!repository.deleteSwipe(userId, targetId, targetUserId)) {
                _swipedTargetIds.value = _swipedTargetIds.value + targetId
            }
        }
    }
}
