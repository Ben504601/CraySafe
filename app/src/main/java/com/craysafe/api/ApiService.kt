package com.craysafe.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Query
import com.craysafe.api.models.LoginResponse
import com.craysafe.api.models.DashboardResponse
import com.craysafe.api.models.PairTankResponse
import com.craysafe.api.models.TankDetailResponse
import com.craysafe.api.models.SwitchModeResponse
import com.craysafe.api.models.AlertsResponse
import com.craysafe.api.models.MarkAlertResponse
import com.craysafe.api.models.BaseResponse
import com.craysafe.api.models.DiagnosticResponse
import com.craysafe.api.models.UnreadCountResponse
import com.craysafe.api.models.ReportData
import com.craysafe.api.models.ReportResponse
import okhttp3.ResponseBody
import retrofit2.http.Streaming


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

    @FormUrlEncoded
    @POST("tank/{id}/mode")
    suspend fun switchMode(
        @Header("Authorization") token: String,
        @Path("id") tankId: Int,
        @Field("mode") mode: String
    ): SwitchModeResponse

    @GET("tank/{id}/reports")
    suspend fun getTankReports(
        @Header("Authorization") token: String,
        @Path("id") tankId: Int,
        @Query("range") range: String
    ): ReportResponse

    @Streaming
    @GET("tank/{id}/reports/pdf")
    suspend fun downloadReportPdf(
        @Header("Authorization") token: String,
        @Path("id") tankId: Int,
        @Query("range") range: String
    ): ResponseBody

    @GET("alerts")
    suspend fun getAlerts(
        @Header("Authorization") token: String
    ): AlertsResponse

    @POST("alerts/{id}/read")
    suspend fun markAlertRead(
        @Header("Authorization") token: String,
        @Path("id") alertId: Int
    ): MarkAlertResponse

    @FormUrlEncoded
    @POST("fcm-token")
    suspend fun saveFcmToken(
        @Header("Authorization") token: String,
        @Field("fcm_token") fcmToken: String
    ): BaseResponse

    @GET("support/qa")
    suspend fun getDiagnosticQnA(
        @Header("Authorization") token: String,
        @Query("search") search: String? = null,
        @Query("category") category: String? = null
    ): DiagnosticResponse

    @GET("alerts/unread-count")
    suspend fun getUnreadAlertCount(
        @Header("Authorization") token: String
    ): UnreadCountResponse

    @GET("test")
    suspend fun wakeUp(): retrofit2.Response<Any>
}