package com.swipehire.app.data.repository

import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.UnknownHostException

class GeocodingRateLimitedException : Exception()

class AppRepository {

    // Lazy initialization ensures objects are constructed only when called
    private val firestoreRepository by lazy { FirestoreRepository() }
    private val api: SwipeHireApi by lazy { RetrofitClient.api }

    private fun logApiFallback(operation: String, error: Exception) {
        val reason = if (error is HttpException) "HTTP ${error.code()}" else error.javaClass.simpleName
        Log.w("SwipeHireApi", "$operation failed ($reason); trying Firestore fallback")
    }

    private fun isDefinitelyOffline(error: Exception): Boolean =
        error is ConnectException || error is UnknownHostException

    private suspend fun <T> writeWithOfflineFallback(
        operation: String,
        apiWrite: suspend () -> T,
        firestoreWrite: suspend () -> T?
    ): T? = try {
        apiWrite()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        if (isDefinitelyOffline(error)) {
            logApiFallback(operation, error)
            firestoreWrite()
        } else {
            val reason = if (error is HttpException) "HTTP ${error.code()}" else error.javaClass.simpleName
            Log.w("SwipeHireApi", "$operation failed ($reason); not bypassing the API response")
            null
        }
    }

    // Firestore getters
    suspend fun getJobPostings(): List<JobPosting> = firestoreRepository.getJobPostings()

    suspend fun getStudentProfiles(): List<StudentProfile> = firestoreRepository.getStudentProfiles()

    suspend fun getStudentProfile(userId: String): StudentProfile? = firestoreRepository.getStudentProfile(userId)

    suspend fun getCompanyProfile(userId: String): CompanyProfile? = firestoreRepository.getCompanyProfile(userId)

    suspend fun getCompanyJobs(companyId: String): List<JobPosting> {
        return try {
            api.getCompanyJobs(companyId).map { it.toDomain() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logApiFallback("getCompanyJobs", error)
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
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            logApiFallback("getJobs", e)
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
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            logApiFallback("getStudents", e)
            emptyList()
        }
    }

    // --- REST API: Student Profiles (POST & PUT) ---

    suspend fun updateStudent(id: String, dto: CreateStudentDto): ProfileResponseDto? =
        writeWithOfflineFallback("updateStudent", { api.updateStudent(id, dto) }) {
            firestoreRepository.updateStudent(id, dto)
        }

    // --- REST API: Company Profiles (POST & PUT) ---

    suspend fun updateCompany(id: String, dto: CreateCompanyDto): ProfileResponseDto? =
        writeWithOfflineFallback("updateCompany", { api.updateCompany(id, dto) }) {
            firestoreRepository.updateCompany(id, dto)
        }

    // --- REST API: Swiping & Geocoding ---

    suspend fun getSwipedTargetIds(userId: String): Set<String> {
        return try {
            api.getSwipedTargetIds(userId).toSet()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logApiFallback("getSwipedTargetIds", error)
            firestoreRepository.getSwipedTargetIds(userId)
        }
    }

    suspend fun recordSwipe(userId: String, targetId: String, targetUserId: String, isLike: Boolean): SwipeResponse? {
        return try {
            val response = api.recordSwipe(SwipeRequest(userId, targetId, targetUserId, isLike))
            if (response.isSuccessful) response.body()
            else {
                Log.w("SwipeHireApi", "recordSwipe failed (HTTP ${response.code()}); not retrying a potentially completed write")
                null
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (isDefinitelyOffline(error)) {
                logApiFallback("recordSwipe", error)
                firestoreRepository.recordSwipe(userId, targetId, targetUserId, isLike)
            } else {
                Log.w("SwipeHireApi", "recordSwipe failed (${error.javaClass.simpleName}); server outcome unknown")
                null
            }
        }
    }

    suspend fun deleteSwipe(userId: String, targetId: String, targetUserId: String): Boolean {
        return try {
            val response = api.undoSwipe(userId, targetId, targetUserId)
            if (response.isSuccessful) true
            else {
                Log.w("SwipeHireApi", "deleteSwipe failed (HTTP ${response.code()})")
                false
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (isDefinitelyOffline(error)) {
                logApiFallback("deleteSwipe", error)
                firestoreRepository.deleteSwipe(userId, targetId, targetUserId)
            } else {
                Log.w("SwipeHireApi", "deleteSwipe failed (${error.javaClass.simpleName}); server outcome unknown")
                false
            }
        }
    }

    suspend fun geocodeAddress(address: String): Pair<Double, Double>? {
        return try {
            val response = api.geocodeAddress(GeocodeRequest(address))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Pair(body.latitude, body.longitude)
            } else if (response.code() == 429) {
                throw GeocodingRateLimitedException()
            } else null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (limited: GeocodingRateLimitedException) {
            throw limited
        } catch (e: Exception) {
            null
        }
    }


    suspend fun deleteJob(id: String, companyId: String): ProfileResponseDto? =
        writeWithOfflineFallback("deleteJob", { api.deleteJob(id, companyId) }) {
            firestoreRepository.deleteJob(id, companyId)
        }

    suspend fun createJob(dto: CreateJobPostingDto): ProfileResponseDto? =
        writeWithOfflineFallback("createJob", { api.createJob(dto) }) {
            firestoreRepository.createJob(dto)
        }

    suspend fun getSavedItems(userId: String): SavedItemsDto? {
        return try {
            api.getSavedItems(userId)
        } catch (e: Exception) {
            firestoreRepository.getSavedItems(userId)
        }
    }

    suspend fun setSavedItem(userId: String, kind: String, itemId: String, saved: Boolean): SavedItemsDto? =
        writeWithOfflineFallback("setSavedItem", {
            api.setSavedItem(userId, kind, itemId, SetSavedItemRequest(saved))
        }) {
            firestoreRepository.setSavedItem(userId, kind, itemId, saved)
        }

    suspend fun getUserSettings(userId: String): UserSettingsDto? {
        return try {
            api.getUserSettings(userId)
        } catch (e: Exception) {
            firestoreRepository.getUserSettings(userId)
        }
    }

    suspend fun updateUserSettings(userId: String, settings: UserSettingsDto): UserSettingsDto? =
        writeWithOfflineFallback("updateUserSettings", { api.updateUserSettings(userId, settings) }) {
            firestoreRepository.updateUserSettings(userId, settings)
        }
}
