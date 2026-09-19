package com.swipehire.app.data.remote

data class CreateStudentDto(
    val name: String,
    val course: String,
    val year: String,
    val skills: List<String>,
    val blurb: String,
    val avatarInitials: String
)

data class CreateCompanyDto(
    val name: String,
    val industry: String,
    val location: String,
    val description: String,
    val logoInitials: String
)

data class ProfileResponseDto(
    val id: String,
    val message: String,
    val success: Boolean
)

data class CreateJobPostingDto(
    val company: String,
    val role: String,
    val location: String,
    val workAddress: String,
    val latitude: Double,
    val longitude: Double,
    val tags: List<String>,
    val blurb: String,
    val logoInitials: String,
    val remoteType: String,
    val salaryRange: String
)

data class SavedItemsDto(
    val savedJobIds: List<String> = emptyList(),
    val savedStudentIds: List<String> = emptyList()
)

data class SetSavedItemRequest(val saved: Boolean)

data class UserSettingsDto(
    val pushNotifications: Boolean = true,
    val matchAlerts: Boolean = true,
    val messageAlerts: Boolean = true,
    val profileVisible: Boolean = true
)
