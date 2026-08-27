// In: api/models/DashboardResponse.kt
package com.craysafe.api.models

data class DashboardResponse(
    val success: Boolean,
    val data: List<DashboardData>? = null,
    val message: String? = null
)

data class DashboardData(
    val DashboardID: Int,
    val UserID: Int? = null,
    val TankID: Int,
    val Mode: String? = null,
    val Temperature: Double? = null,
    val Ph_Level: Double? = null,
    val Turbidity: Double? = null,
    val Status: String? = null,
    val Tankname: String? = null
)