package com.swipehire.app.data.repository

import com.swipehire.app.data.ChatMessage
import com.swipehire.app.data.JobPosting
import com.swipehire.app.data.RemoteType
import com.swipehire.app.data.StudentProfile
import com.swipehire.app.data.remote.CreateCompanyDto
import com.swipehire.app.data.remote.CreateStudentDto
import com.swipehire.app.data.remote.GeocodeRequest
import com.swipehire.app.data.remote.ProfileResponseDto
import com.swipehire.app.data.remote.CreateJobPostingDto
import com.swipehire.app.data.remote.RetrofitClient
import com.swipehire.app.data.remote.SwipeHireApi
import com.swipehire.app.data.remote.SwipeRequest
import com.swipehire.app.data.remote.SavedItemsDto
import com.swipehire.app.data.remote.SetSavedItemRequest
import com.swipehire.app.data.remote.UserSettingsDto
import kotlinx.coroutines.flow.Flow
import com.swipehire.app.util.matchingSkills

class AppRepository {

    // Lazy initialization ensures objects are constructed only when called
    private val firestoreRepository by lazy { FirestoreRepository() }
    private val api: SwipeHireApi by lazy { RetrofitClient.api }

    // Firestore getters
    suspend fun getJobPostings(): List<JobPosting> = firestoreRepository.getJobPostings()

    suspend fun getStudentProfiles(): List<StudentProfile> = firestoreRepository.getStudentProfiles()

    fun getLiveMessages(matchId: String, currentUserId: String): Flow<List<ChatMessage>> =
        firestoreRepository.getLiveMessages(matchId, currentUserId)

    suspend fun sendMessage(matchId: String, senderId: String, text: String): Boolean =
        firestoreRepository.sendMessage(matchId, senderId, text)

    // --- REST API: GET Feed Endpoints ---

    suspend fun getJobsFromApi(): List<JobPosting> {
        return try {
            val dtos = api.getJobs()
            val viewerSkills = setOf("kotlin", "android", "rest apis", "sql", "c#")
            dtos.map { dto ->
                JobPosting(
                    id = dto.id,
                    company = dto.company,
                    role = dto.role,
                    location = dto.location,
                    workAddress = dto.workAddress,
                    latitude = dto.latitude,
                    longitude = dto.longitude,
                    tags = dto.tags,
                    blurb = dto.blurb,
                    logoInitials = dto.logoInitials,
                    remoteType = try { RemoteType.valueOf(dto.remoteType) } catch (e: Exception) { RemoteType.HYBRID },
                    salaryRange = dto.salaryRange,
                    matchedSkills = matchingSkills(dto.tags, viewerSkills),
                    willMatch = true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getStudentsFromApi(): List<StudentProfile> {
        return try {
            val dtos = api.getStudents()
            val hiringSkills = setOf("c#", ".net", "kotlin", "security", "azure")
            dtos.map { dto ->
                StudentProfile(
                    id = dto.id,
                    name = dto.name,
                    course = dto.course,
                    year = dto.year,
                    skills = dto.skills,
                    blurb = dto.blurb,
                    avatarInitials = dto.avatarInitials,
                    matchedSkills = matchingSkills(dto.skills, hiringSkills),
                    willMatch = true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- REST API: Student Profiles (POST & PUT) ---

    suspend fun createStudent(dto: CreateStudentDto): ProfileResponseDto? {
        return try {
            api.createStudent(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            firestoreRepository.createStudent(dto)
        }
    }

    suspend fun updateStudent(id: String, dto: CreateStudentDto): ProfileResponseDto? {
        return try {
            api.updateStudent(id, dto)
        } catch (e: Exception) {
            e.printStackTrace()
            firestoreRepository.updateStudent(id, dto)
        }
    }

    // --- REST API: Company Profiles (POST & PUT) ---

    suspend fun createCompany(dto: CreateCompanyDto): ProfileResponseDto? {
        return try {
            api.createCompany(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            firestoreRepository.createCompany(dto)
        }
    }

    suspend fun updateCompany(id: String, dto: CreateCompanyDto): ProfileResponseDto? {
        return try {
            api.updateCompany(id, dto)
        } catch (e: Exception) {
            e.printStackTrace()
            firestoreRepository.updateCompany(id, dto)
        }
    }

    // --- REST API: Swiping & Geocoding ---

    suspend fun recordSwipe(userId: String, targetId: String, isLike: Boolean): Boolean {
        return try {
            val response = api.recordSwipe(SwipeRequest(userId, targetId, isLike))
            response.isSuccessful && (response.body()?.isMatch == true)
        } catch (e: Exception) {
            firestoreRepository.recordSwipe(userId, targetId, isLike)
        }
    }

    suspend fun geocodeAddress(address: String): Pair<Double, Double>? {
        return try {
            val response = api.geocodeAddress(GeocodeRequest(address))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Pair(body.latitude, body.longitude)
            } else null
        } catch (e: Exception) {
            null
        }
    }


    suspend fun deleteJob(id: String): ProfileResponseDto? {
        return try {
            api.deleteJob(id)
        } catch (e: Exception) {
            e.printStackTrace()
            firestoreRepository.deleteJob(id)
        }
    }

    suspend fun createJob(dto: CreateJobPostingDto): ProfileResponseDto? {
        return try {
            api.createJob(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            firestoreRepository.createJob(dto)
        }
    }

    suspend fun getSavedItems(userId: String): SavedItemsDto? {
        return try {
            api.getSavedItems(userId)
        } catch (e: Exception) {
            firestoreRepository.getSavedItems(userId)
        }
    }

    suspend fun setSavedItem(userId: String, kind: String, itemId: String, saved: Boolean): SavedItemsDto? {
        return try {
            api.setSavedItem(userId, kind, itemId, SetSavedItemRequest(saved))
        } catch (e: Exception) {
            firestoreRepository.setSavedItem(userId, kind, itemId, saved)
        }
    }

    suspend fun getUserSettings(userId: String): UserSettingsDto? {
        return try {
            api.getUserSettings(userId)
        } catch (e: Exception) {
            firestoreRepository.getUserSettings(userId)
        }
    }

    suspend fun updateUserSettings(userId: String, settings: UserSettingsDto): UserSettingsDto? {
        return try {
            api.updateUserSettings(userId, settings)
        } catch (e: Exception) {
            firestoreRepository.updateUserSettings(userId, settings)
        }
    }
}