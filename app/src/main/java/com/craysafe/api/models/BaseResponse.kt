package com.craysafe.api.models

data class BaseResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null,
    val code: String? = null
)
