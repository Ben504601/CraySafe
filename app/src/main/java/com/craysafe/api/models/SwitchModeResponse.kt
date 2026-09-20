package com.craysafe.api.models

data class SwitchModeResponse(
    val success: Boolean,
    val message: String,
    val new_mode: String? = null
)
