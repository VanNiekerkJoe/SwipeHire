package com.swipehire.app.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.swipehire.app.data.JobPosting
import com.swipehire.app.data.RemoteType
import com.swipehire.app.data.StudentProfile

// Firestore document for Job Posting
data class JobPostingDto(
    @DocumentId val id: String = "",
    val companyId: String = "",
    val company: String = "",
    val role: String = "",
    val location: String = "",
    val workAddress: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val tags: List<String> = emptyList(),
    val blurb: String = "",
    val logoInitials: String = "",
    val remoteType: String = "ON_SITE",
    val salaryRange: String = ""
) {
    fun toDomain(matchedSkills: List<String> = emptyList(), willMatch: Boolean = false): JobPosting {
        return JobPosting(
            id = id,
            company = company,
            role = role,
            location = location,
            workAddress = workAddress,
            latitude = latitude,
            longitude = longitude,
            tags = tags,
            blurb = blurb,
            logoInitials = logoInitials,
            remoteType = try { RemoteType.valueOf(remoteType) } catch (e: Exception) { RemoteType.ON_SITE },
            salaryRange = salaryRange,
            matchedSkills = matchedSkills,
            willMatch = willMatch
        )
    }
}

// Firestore document for Student Profile
data class StudentProfileDto(
    @DocumentId val id: String = "",
    val name: String = "",
    val course: String = "",
    val year: String = "",
    val skills: List<String> = emptyList(),
    val blurb: String = "",
    val avatarInitials: String = ""
) {
    fun toDomain(matchedSkills: List<String> = emptyList(), willMatch: Boolean = false): StudentProfile {
        return StudentProfile(
            id = id,
            name = name,
            course = course,
            year = year,
            skills = skills,
            blurb = blurb,
            avatarInitials = avatarInitials,
            matchedSkills = matchedSkills,
            willMatch = willMatch
        )
    }
}

// Firestore message document for subcollection
data class ChatMessageDto(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)