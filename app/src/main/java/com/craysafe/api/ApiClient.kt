package com.craysafe.api

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.io.File

object ApiClient {

    private const val BASE_URL = "https://craysafe-api.onrender.com/api/"
    private var retrofit: Retrofit? = null

    // Call this once when the app starts
    fun init(context: Context) {
        if (retrofit != null) return // Prevent re-initialization

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 1. Create a 10MB cache on the device
        val cache = Cache(
            File(context.cacheDir, "http_cache"),
            10L * 1024 * 1024 // 10 MB
        )

        // 2. Build the single OkHttpClient with cache and timeouts
        val client = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                val newRequest = request.newBuilder()
                    .header("Cache-Control", "public, max-age=60") // Cache valid for 60s
                    .build()
                chain.proceed(newRequest)
            }
            .connectTimeout(15, TimeUnit.SECONDS) // Fail faster on slow networks
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        // 3. Build Retrofit
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Access the API service
    val apiService: ApiService by lazy {
        retrofit!!.create(ApiService::class.java)
    }
}