package com.swipehire.app.data

/** The account role is stored in users/{firebaseUid}.role and cannot be switched in-app. */
enum class AccountType { STUDENT, COMPANY }

enum class RemoteType(val label: String) {
    ON_SITE("On-site"),
    HYBRID("Hybrid"),
    REMOTE("Remote")
}

data class JobPosting(
    val id: String,
    val companyId: String,
    val company: String,
    val role: String,
    val location: String,
    val workAddress: String,
    val latitude: Double,
    val longitude: Double,
    val tags: List<String>,
    val blurb: String,
    val logoInitials: String,
    val remoteType: RemoteType,
    val salaryRange: String,
    val matchedSkills: List<String>
)

data class StudentProfile(
    /** Firebase UID of the student who owns this profile. */
    val id: String,
    val name: String,
    val course: String,
    val year: String,
    val skills: List<String>,
    val blurb: String,
    val avatarInitials: String,
    val matchedSkills: List<String>
)

data class CompanyProfile(
    val id: String,
    val name: String,
    val industry: String,
    val location: String,
    val description: String,
    val logoInitials: String,
    val hiringFor: List<String>
)

data class ChatMessage(
    val id: String,
    val text: String,
    val fromMe: Boolean
)

/** A real Firestore match. Only participantIds may read or write its conversation. */
data class MatchChat(
    val id: String,
    val participantIds: List<String>,
    val name: String,
    val subtitle: String,
    val avatarInitials: String,
    val lastMessage: String,
    val unread: Boolean
)
