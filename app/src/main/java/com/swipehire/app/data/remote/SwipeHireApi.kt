package com.swipehire.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.DELETE
import retrofit2.http.Query


data class SwipeRequest(
    val userId: String,
    val targetId: String,
    val targetUserId: String,
    val isLike: Boolean
)

data class SwipeResponse(
    val isMatch: Boolean,
    val matchId: String?
)

data class GeocodeRequest(
    val address: String
)

data class GeocodeResponse(
    val latitude: Double,
    val longitude: Double
)

interface SwipeHireApi {

    @GET("api/swipes/{userId}")
    suspend fun getSwipedTargetIds(@Path("userId") userId: String): List<String>

    @POST("api/swipes")
    suspend fun recordSwipe(@Body request: SwipeRequest): Response<SwipeResponse>

    @DELETE("api/swipes")
    suspend fun undoSwipe(
        @Query("userId") userId: String,
        @Query("targetId") targetId: String,
        @Query("targetUserId") targetUserId: String
    ): Response<Unit>

    @POST("api/location/geocode")
    suspend fun geocodeAddress(@Body request: GeocodeRequest): Response<GeocodeResponse>

    @GET("api/jobs")
    suspend fun getJobs(): List<JobPostingDto>

    @GET("api/companies/{companyId}/jobs")
    suspend fun getCompanyJobs(@Path("companyId") companyId: String): List<JobPostingDto>

    @GET("api/students")
    suspend fun getStudents(): List<StudentProfileDto>

    // Student Profiles
    @POST("api/students")
    suspend fun createStudent(@Body request: CreateStudentDto): ProfileResponseDto

    @PUT("api/students/{id}")
    suspend fun updateStudent(
        @Path("id") id: String,
        @Body request: CreateStudentDto
    ): ProfileResponseDto

    // Company Profiles
    @POST("api/companies")
    suspend fun createCompany(@Body request: CreateCompanyDto): ProfileResponseDto

    @PUT("api/companies/{id}")
    suspend fun updateCompany(
        @Path("id") id: String,
        @Body request: CreateCompanyDto
    ): ProfileResponseDto

    @POST("api/jobs")
    suspend fun createJob(@Body request: CreateJobPostingDto): ProfileResponseDto

    @DELETE("api/jobs/{id}")
    suspend fun deleteJob(@Path("id") id: String, @Query("companyId") companyId: String): ProfileResponseDto

    @GET("api/users/{userId}/saved")
    suspend fun getSavedItems(@Path("userId") userId: String): SavedItemsDto

    @PUT("api/users/{userId}/saved/{kind}/{itemId}")
    suspend fun setSavedItem(
        @Path("userId") userId: String,
        @Path("kind") kind: String,
        @Path("itemId") itemId: String,
        @Body request: SetSavedItemRequest
    ): SavedItemsDto

    @GET("api/users/{userId}/settings")
    suspend fun getUserSettings(@Path("userId") userId: String): UserSettingsDto

    @PUT("api/users/{userId}/settings")
    suspend fun updateUserSettings(
        @Path("userId") userId: String,
        @Body settings: UserSettingsDto
    ): UserSettingsDto
}
