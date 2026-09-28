package com.craysafe.api.models

data class DiagnosticItem(
    val qa_id: Int,
    val category: String,
    val issue_title: String,
    val solution: String,
    val created_at: String
)

data class DiagnosticResponse(
    val success: Boolean,
    val data: List<DiagnosticItem>? = null,
    val message: String? = null
)