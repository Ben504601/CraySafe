// In: api/models/LoginResponse.kt
package com.craysafe.api.models

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: User? = null
)