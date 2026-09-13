package com.craysafe.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import com.craysafe.api.models.LoginResponse
import com.craysafe.api.models.DashboardResponse
import com.craysafe.api.models.PairTankResponse
import com.craysafe.api.models.TankDetailResponse

interface ApiService {

    // ─── LOGIN ───
    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): LoginResponse

    @GET("dashboard")
    suspend fun getDashboard(
        @Header("Authorization") token: String
    ): DashboardResponse

    @FormUrlEncoded
    @POST("register")
    suspend fun register(
        @Field("username") username: String,
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("confirm_password") confirm_password: String,
        @Field("product_id") productId: String
    ): LoginResponse

    @FormUrlEncoded
    @POST("pair-tank")
    suspend fun pairTank(
        @Header("Authorization") token: String,
        @Field("product_id") productId: String
    ): PairTankResponse

    @GET("tank/{id}")
    suspend fun getTankDetail(
        @Header("Authorization") token: String,
        @Path("id") tankId: Int
    ): TankDetailResponse
}