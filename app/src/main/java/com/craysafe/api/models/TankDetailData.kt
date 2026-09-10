package com.craysafe.api.models

data class TankDetailData(
    val tankName: String,
    val mode: String,
    val temperature: Double?,
    val phLevel: Double?,
    val turbidity: Double?,
    val status: String,
    val lastUpdated: String,
    val timeToDanger: String?,
    val tankId: Int
)
