package com.craysafe.api.models

// Response Model
data class PairTankResponse(
    val success: Boolean,
    val message: String,
    val tank: Tank? = null
)

data class Tank(
    val TankID: Int,
    val ProductID: String,
    val Tankname: String
)