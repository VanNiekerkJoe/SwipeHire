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
import kotlinx.coroutines.flow.Flow

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
                    matchedSkills = emptyList(),
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
            dtos.map { dto ->
                StudentProfile(
                    id = dto.id,
                    name = dto.name,
                    course = dto.course,
                    year = dto.year,
                    skills = dto.skills,
                    blurb = dto.blurb,
                    avatarInitials = dto.avatarInitials,
                    matchedSkills = emptyList(),
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
            null
        }
    }

    suspend fun updateStudent(id: String, dto: CreateStudentDto): ProfileResponseDto? {
        return try {
            api.updateStudent(id, dto)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- REST API: Company Profiles (POST & PUT) ---

    suspend fun createCompany(dto: CreateCompanyDto): ProfileResponseDto? {
        return try {
            api.createCompany(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateCompany(id: String, dto: CreateCompanyDto): ProfileResponseDto? {
        return try {
            api.updateCompany(id, dto)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- REST API: Swiping & Geocoding ---

    suspend fun recordSwipe(userId: String, targetId: String, isLike: Boolean): Boolean {
        return try {
            val response = api.recordSwipe(SwipeRequest(userId, targetId, isLike))
            response.isSuccessful && (response.body()?.isMatch == true)
        } catch (e: Exception) {
            false
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
            null
        }
    }

    suspend fun createJob(dto: CreateJobPostingDto): ProfileResponseDto? {
        return try {
            api.createJob(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}