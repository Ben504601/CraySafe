package com.craysafe.api.models

data class UnreadCountResponse(
    val success: Boolean,
    val count: Int = 0,
    val message: String? = null
)