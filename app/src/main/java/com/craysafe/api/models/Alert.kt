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
    val message: String? = null,
    val data: AlertsData? = null,
    val code: String? = null
)

data class AlertsData(
    val alerts: List<Alert>? = null,
    val unread_count: Int = 0,
    val pagination: PaginationData? = null
)

data class PaginationData(
    val current_page: Int,
    val last_page: Int,
    val per_page: Int,
    val total: Int,
    val has_more: Boolean
)

data class MarkAlertResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null,
    val code: String? = null
)