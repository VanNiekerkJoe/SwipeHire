package com.swipehire.app.data.repository

import android.net.Uri
import com.swipehire.app.data.AppAlert
import com.swipehire.app.data.ChatMessage
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.CompanyProfile
import com.swipehire.app.data.JobPosting
import com.swipehire.app.data.RemoteType
import com.swipehire.app.data.StudentProfile
import com.swipehire.app.data.MatchChat
import com.swipehire.app.data.remote.CreateCompanyDto
import com.swipehire.app.data.remote.CreateStudentDto
import com.swipehire.app.data.remote.GeocodeRequest
import com.swipehire.app.data.remote.ProfileResponseDto
import com.swipehire.app.data.remote.CreateJobPostingDto
import com.swipehire.app.data.remote.RetrofitClient
import com.swipehire.app.data.remote.SwipeHireApi
import com.swipehire.app.data.remote.SwipeRequest
import com.swipehire.app.data.remote.SwipeResponse
import com.swipehire.app.data.remote.SavedItemsDto
import com.swipehire.app.data.remote.SetSavedItemRequest
import com.swipehire.app.data.remote.UserSettingsDto
import kotlinx.coroutines.flow.Flow

class AppRepository {

    // Lazy initialization ensures objects are constructed only when called
    private val firestoreRepository by lazy { FirestoreRepository() }
    private val api: SwipeHireApi by lazy { RetrofitClient.api }

    // Firestore getters
    suspend fun getJobPostings(): List<JobPosting> = firestoreRepository.getJobPostings()

    suspend fun getStudentProfiles(): List<StudentProfile> = firestoreRepository.getStudentProfiles()

    suspend fun getStudentProfile(userId: String): StudentProfile? = firestoreRepository.getStudentProfile(userId)

    suspend fun getCompanyProfile(userId: String): CompanyProfile? = firestoreRepository.getCompanyProfile(userId)

    suspend fun getCompanyJobs(companyId: String): List<JobPosting> {
        return try {
            api.getCompanyJobs(companyId).map { it.toDomain() }
        } catch (_: Exception) {
            firestoreRepository.getCompanyJobs(companyId)
        }
    }

    suspend fun getAccountType(userId: String): AccountType? = firestoreRepository.getAccountType(userId)

    suspend fun setAccountType(userId: String, type: AccountType): Boolean =
        firestoreRepository.setAccountType(userId, type)

    fun getMatches(userId: String): Flow<List<MatchChat>> = firestoreRepository.getMatches(userId)

    fun getAlerts(userId: String): Flow<List<AppAlert>> = firestoreRepository.getAlerts(userId)

    suspend fun markAlertRead(userId: String, alertId: String): Boolean =
        firestoreRepository.markAlertRead(userId, alertId)

    suspend fun getMatch(matchId: String, userId: String): MatchChat? =
        firestoreRepository.getMatch(matchId, userId)

    fun getLiveMessages(matchId: String, currentUserId: String): Flow<List<ChatMessage>> =
        firestoreRepository.getLiveMessages(matchId, currentUserId)

    suspend fun sendMessage(matchId: String, senderId: String, text: String): Boolean =
        firestoreRepository.sendMessage(matchId, senderId, text)

    suspend fun uploadStudentCv(userId: String, uri: Uri, fileName: String): Pair<String, String>? =
        firestoreRepository.uploadStudentCv(userId, uri, fileName)

    suspend fun getCvDownloadUrl(path: String): Uri? = firestoreRepository.getCvDownloadUrl(path)

    suspend fun getIncomingInterestCount(userId: String): Int =
        firestoreRepository.getIncomingInterestCount(userId)

    suspend fun getAverageReplyTimeMillis(userId: String): Long? =
        firestoreRepository.getAverageReplyTimeMillis(userId)

    // --- REST API: GET Feed Endpoints ---

    suspend fun getJobsFromApi(): List<JobPosting> {
        return try {
            val dtos = api.getJobs()
            dtos.map { dto ->
                JobPosting(
                    id = dto.id,
                    companyId = dto.companyId,
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
                    matchedSkills = emptyList()
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
            dtos.map { dto ->
                StudentProfile(
                    id = dto.id,
                    name = dto.name,
                    course = dto.course,
                    year = dto.year,
                    skills = dto.skills,
                    blurb = dto.blurb,
                    avatarInitials = dto.avatarInitials,
                    matchedSkills = emptyList()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- REST API: Student Profiles (POST & PUT) ---

    suspend fun updateStudent(id: String, dto: CreateStudentDto): ProfileResponseDto? {
        return try {
            api.updateStudent(id, dto)
        } catch (e: Exception) {
            e.printStackTrace()
            firestoreRepository.updateStudent(id, dto)
        }
    }

    // --- REST API: Company Profiles (POST & PUT) ---

    suspend fun updateCompany(id: String, dto: CreateCompanyDto): ProfileResponseDto? {
        return try {
            api.updateCompany(id, dto)
        } catch (e: Exception) {
            e.printStackTrace()
            firestoreRepository.updateCompany(id, dto)
        }
    }

    // --- REST API: Swiping & Geocoding ---

    suspend fun getSwipedTargetIds(userId: String): Set<String> {
        return try {
            api.getSwipedTargetIds(userId).toSet()
        } catch (_: Exception) {
            firestoreRepository.getSwipedTargetIds(userId)
        }
    }

    suspend fun recordSwipe(userId: String, targetId: String, targetUserId: String, isLike: Boolean): SwipeResponse? {
        return try {
            val response = api.recordSwipe(SwipeRequest(userId, targetId, targetUserId, isLike))
            if (response.isSuccessful) response.body()
            else firestoreRepository.recordSwipe(userId, targetId, targetUserId, isLike)
        } catch (_: Exception) {
            firestoreRepository.recordSwipe(userId, targetId, targetUserId, isLike)
        }
    }

    suspend fun deleteSwipe(userId: String, targetId: String, targetUserId: String): Boolean {
        return try {
            val response = api.undoSwipe(userId, targetId, targetUserId)
            if (response.isSuccessful) true
            else firestoreRepository.deleteSwipe(userId, targetId, targetUserId)
        } catch (_: Exception) {
            firestoreRepository.deleteSwipe(userId, targetId, targetUserId)
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


    suspend fun deleteJob(id: String, companyId: String): ProfileResponseDto? {
        return try {
            api.deleteJob(id, companyId)
        } catch (e: Exception) {
            e.printStackTrace()
            firestoreRepository.deleteJob(id, companyId)
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
