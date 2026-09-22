package com.swipehire.app.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.swipehire.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val logging = HttpLoggingInterceptor().apply {
        redactHeader("Authorization")
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val firebaseAuthInterceptor = Interceptor { chain ->

        val originalRequest = chain.request()
        val segments = originalRequest.url.pathSegments
        val publicFeed = originalRequest.method == "GET" &&
            (segments == listOf("api", "jobs") ||
                segments == listOf("api", "students") ||
                (segments.size == 4 && segments[0] == "api" &&
                    segments[1] == "companies" && segments[3] == "jobs"))

        if (publicFeed) {
            return@Interceptor chain.proceed(originalRequest)
        }

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            return@Interceptor chain.proceed(originalRequest)
        }

        val token = try {
            Tasks.await(
                user.getIdToken(false),
                10,
                TimeUnit.SECONDS
            ).token
        } catch (_: Exception) {
            null
        }

        val request = originalRequest
            .newBuilder()
            .apply {
                if (!token.isNullOrBlank()) {
                    addHeader(
                        "Authorization",
                        "Bearer $token"
                    )
                }
            }
            .build()

        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(firebaseAuthInterceptor)
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        // Give the API time to resume when Visual Studio is paused at a breakpoint.
        .readTimeout(if (BuildConfig.DEBUG) 120 else 30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: SwipeHireApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SwipeHireApi::class.java)
    }
}
