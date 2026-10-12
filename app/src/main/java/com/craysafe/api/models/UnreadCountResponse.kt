package com.craysafe.api.models

data class UnreadCountResponse(
    val success: Boolean,
    val message: String? = null,
    val data: UnreadCountData? = null,
    val code: String? = null
)

data class UnreadCountData(
    val count: Int = 0
)