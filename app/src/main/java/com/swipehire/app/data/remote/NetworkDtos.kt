package com.swipehire.app.data.remote

data class SwipeRequestDto(
    val userId: String,
    val targetId: String,
    val isLike: Boolean
)

data class SwipeResponseDto(
    val isMatch: Boolean
)

data class GeocodeRequestDto(
    val address: String
)

data class GeocodeResponseDto(
    val latitude: Double,
    val longitude: Double
)

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