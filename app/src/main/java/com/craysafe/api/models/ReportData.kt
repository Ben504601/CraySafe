package com.craysafe.api.models

data class ReportReading(
    val timestamp: String,
    val temperature: Double,
    val ph: Double,
    val turbidity: Double
)

data class ReportStats(
    val min: Double,
    val avg: Double,
    val max: Double
)

data class ReportSummary(
    val temperature: ReportStats,
    val ph: ReportStats,
    val turbidity: ReportStats,
    val reading_count: Int
)

data class ReportTank(
    val TankID: Int,
    val Tankname: String?
)

data class ReportData(
    val tank: ReportTank,
    val range: String,
    val readings: List<ReportReading>,
    val summary: ReportSummary?
)

data class ReportResponse(
    val success: Boolean,
    val data: ReportData? = null,
    val message: String? = null
)