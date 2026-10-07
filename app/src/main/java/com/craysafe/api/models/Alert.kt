package com.craysafe.api.models

data class Alert(
    val alert_id: Int,
    val tank_id: Int,
    val alert_type: String,
    val severity: String? = null,
    val parameter: String? = null,
    val headline: String? = null,
    val advice: String? = null,
    val message: String,
    val status: String,
    val alert_date: String,
    val tank_name: String? = null
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