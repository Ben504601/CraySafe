// In: api/models/LoginResponse.kt
package com.craysafe.api.models

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginData? = null,
    val code: String? = null
)

data class LoginData(
    val token: String? = null,
    val user: User? = null
)

data class ValidationErrorResponse(
    val success: Boolean,
    val message: String,
    val code: String? = null,
    val errors: Map<String, List<String>>? = null
)