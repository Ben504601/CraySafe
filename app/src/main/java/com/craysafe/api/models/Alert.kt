package com.craysafe.api.models

data class Alert(
    val alert_id: Int,
    val tank_id: Int,
    val alert_type: String,
    val message: String,
    val status: String,
    val alert_date: String
)

data class AlertsResponse(
    val success: Boolean,
    val data: List<Alert>? = null,
    val unread_count: Int? = null,
    val message: String? = null
)

data class MarkAlertResponse(
    val success: Boolean,
    val message: String
)