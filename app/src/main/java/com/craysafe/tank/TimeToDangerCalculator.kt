package com.craysafe.tank

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Predicts when a water parameter will cross a safe threshold.
 * Uses multiple linear regression with:
 *   ŷ = b + m1*t + m2*sin(2πh/24) + m3*cos(2πh/24)
 */
object TimeToDangerCalculator {

    data class Reading(val value: Double, val timeMillis: Long)

    data class DangerResult(
        val minutes: Long,
        val predictedValue: Double,
        val breachType: String // "high" or "Low"
    )

    /**
     *  Returns minutes until threshold breach, or null if no danger
     *  @param readings Historical readings (oldest first)
     *  @param lowerBound Safe lower limit
     *  @param upperBound Safe upper limit
     */
    fun compute(readings: List<Reading>, lowerBound: Double, upperBound: Double): DangerResult? {
        if (readings.size < 21) return null

        val coefficients = fitMLR(readings) ?: return null
        val (b, m1, m2, m3) = coefficients

        val startTime = readings.first().timeMillis
        val now = System.currentTimeMillis()
        val minutesSinceStart = (now - startTime) / 60_000.0

        val maxMinutes = 30L * 24 * 60
        var futureMinutes = 8L
        while (futureMinutes <= maxMinutes) {
            val totalMinutes = minutesSinceStart + futureMinutes
            val futureTime = now + futureMinutes * 60_000L
            val hour = getHourOfDay(futureTime)

            val predicted = b +
                    m1 * totalMinutes +
                    m2 * sin(2 * Math.PI * hour / 24) +
                    m3 * cos(2 * Math.PI * hour / 24)

            if (predicted >= upperBound || predicted <= lowerBound) {
                return DangerResult(
                    minutes = futureMinutes,
                    predictedValue = predicted,
                    breachType = if (predicted >= upperBound) "high" else "low"
                )
            }
            futureMinutes += 8
        }
        return null
    }

    private fun fitMLR(readings: List<Reading>): DoubleArray? {
        val n = readings.size
        if (n < 4) return null

        val startTime = readings.first().timeMillis

        // Build design matrix X (n x 4) and target vector y (n)
        val X = Array(n) {DoubleArray(4)}
        val y = DoubleArray(n)

        for (i in 0 until n) {
            val t = (readings[i].timeMillis - startTime) / 60_000.0
            val h = getHourOfDay(readings[i].timeMillis)
            X[i][0] = 1.0
            X[i][1] = t
            X[i][2] = sin(2 * Math.PI * h / 24)
            X[i][3] = cos(2 * Math.PI * h / 24)
            y[i] = readings[i].value
        }

        // XtX = X^T . X (4x4)
        val XtX = Array(4) { DoubleArray(4) }
        val Xty = DoubleArray(4)
        for ( i in 0 until n) {
            for (j in 0 until 4) {
                for (k in 0 until 4) {
                    XtX[j][k] += X[i][j] * X[i][k]
                }
                Xty[j] += X[i][j] * y[i]
            }
        }
        return solveLinearSystem(XtX, Xty)
    }

    private fun solveLinearSystem(A: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val n = b.size
        val aug = Array(n) { DoubleArray(n + 1) }
        for (i in 0 until n) {
            for (j in 0 until n) aug[i][j] = A[i][j]
            aug[i][n] = b[i]
        }

        for (i in 0 until n) {
            var maxRow = i
            for (k in i + 1 until n) {
                if (abs(aug[k][i]) > abs(aug[maxRow][i])) maxRow = k
            }
            if (abs(aug[maxRow][i]) < 1e-10) return null

            val temp = aug[i]; aug[i] = aug[maxRow]; aug[maxRow] = temp

            for (k in i + 1 until n) {
                val factor = aug[k][i] / aug[i][i]
                for (j in i..n) {
                    aug[k][j] -= factor * aug[i][j]
                }
            }
        }

        // Back substitution
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sum = aug[i][n]
            for (j in i + 1 until n) {
                sum -= aug[i][j] * x[j]
            }
            x[i] = sum / aug[i][i]
        }
        return x
    }

    private fun getHourOfDay(millis: Long): Int {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = millis
        return calendar.get(java.util.Calendar.HOUR_OF_DAY)
    }
}