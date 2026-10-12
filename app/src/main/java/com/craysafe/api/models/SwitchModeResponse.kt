package com.craysafe.api.models

data class SwitchModeResponse(
    val success: Boolean,
    val message: String,
    val data: SwitchModeData? = null,
    val code: String? = null
)

data class SwitchModeData(
    val mode: String? = null
)
