package com.craysafe.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // For Android Emulator: use 10.0.2.2 (localhost on host machine)
    // For Real Phone: use your computer's IP (e.g., 192.168.1.100)
    //private const val BASE_URL = "http://192.168.1.9:8000/api/"

    // For USB Tethering IPv4 Address 10.176.30.170

    private const val BASE_URL = "http://10.176.30.170:8000/api/"

    // Logging interceptor to see network calls in Logcat
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // HTTP Client with logging
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // API Service instance
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}