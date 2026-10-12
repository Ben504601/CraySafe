package com.craysafe.api.models

// Response Model
data class PairTankResponse(
    val success: Boolean,
    val message: String,
    val data: Tank? = null,
    val code: String? = null
)

data class Tank(
    val TankID: Int,
    val ProductID: String,
    val Tankname: String
)