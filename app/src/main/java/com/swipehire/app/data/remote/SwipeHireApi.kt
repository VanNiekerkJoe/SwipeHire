package com.swipehire.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.DELETE


data class SwipeRequest(
    val userId: String,
    val targetId: String,
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

    @POST("api/swipes")
    suspend fun recordSwipe(@Body request: SwipeRequest): Response<SwipeResponse>

    @POST("api/location/geocode")
    suspend fun geocodeAddress(@Body request: GeocodeRequest): Response<GeocodeResponse>

    @GET("api/jobs")
    suspend fun getJobs(): List<JobPostingDto>

    @GET("api/students")
    suspend fun getStudents(): List<StudentProfileDto>

    @POST("api/swipes")
    suspend fun recordSwipe(@Body request: SwipeRequestDto): SwipeResponseDto

    @POST("api/location/geocode")
    suspend fun geocodeAddress(@Body request: GeocodeRequestDto): GeocodeResponseDto

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
    suspend fun deleteJob(@Path("id") id: String): ProfileResponseDto
}