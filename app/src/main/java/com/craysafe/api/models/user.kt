package com.craysafe.api.models

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val role: String
)