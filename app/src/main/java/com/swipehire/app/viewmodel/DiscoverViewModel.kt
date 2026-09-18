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
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val repository: AppRepository = AppRepository()
) : ViewModel() {

    private val _jobStack = MutableStateFlow<List<JobPosting>>(emptyList())
    val jobStack: StateFlow<List<JobPosting>> = _jobStack.asStateFlow()

    private val _studentStack = MutableStateFlow<List<StudentProfile>>(emptyList())
    val studentStack: StateFlow<List<StudentProfile>> = _studentStack.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadCards(accountType: AccountType) {
        viewModelScope.launch {
            _isLoading.value = true
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

    fun onSwipe(userId: String, targetId: String, isLike: Boolean, onMatchFound: (Boolean) -> Unit) {
        viewModelScope.launch {
            // Remove top card locally from active stack
            if (_jobStack.value.isNotEmpty()) {
                _jobStack.value = _jobStack.value.drop(1)
            } else if (_studentStack.value.isNotEmpty()) {
                _studentStack.value = _studentStack.value.drop(1)
            }

            // Record swipe via C# REST API (triggers SwipesController breakpoint)
            val isMatch = repository.recordSwipe(userId, targetId, isLike)
            onMatchFound(isMatch)
        }
    }
}