package com.craysafe.api.models

data class TankDetailData(
    val TankID: Int,
    val Tankname: String?,
    val Mode: String?,
    val Temperature: Double?,
    val Ph_Level: Double?,
    val Turbidity: Double?,
    val Status: String?,
    val TimeToDanger: String?,
    val LastUpdated: String?
)

data class TankDetailResponse(
    val success: Boolean,
    val data: TankDetailData? = null,
    val message: String? = null
)