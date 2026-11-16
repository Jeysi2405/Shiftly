package com.example.shiftly.utils

import java.time.Instant
import java.time.ZoneId

enum class DayType(val label: String) {
    ORDINARY("Ordinary Working Day"),
    REST_DAY("Rest Day"),
    SPECIAL_NON_WORKING("Special Non-Working Day"),
    SPECIAL_NON_WORKING_REST("Special Non-Working + Rest Day"),
    REGULAR_HOLIDAY("Regular Holiday"),
    REGULAR_HOLIDAY_REST("Regular Holiday + Rest Day"),
    SPECIAL_WORKING("Special Working Day"),
    SPECIAL_WORKING_REST("Special Working + Rest Day")
}

data class WageDetails(
    val totalEarned: Double,
    val regularMinutes: Long,
    val nightDiffMinutes: Long,
    val overtimeMinutes: Long
)

object WageCalculator {

    fun calculateTotalEarned(
        startMillis: Long,
        endMillis: Long,
        hourlyRate: Double,
        dayTypeStr: String,
        nightDiffStartHour: Int = 22,
        nightDiffMultiplier: Double = 1.10,
        ordinaryOvertimeMultiplier: Double = 1.25
    ): WageDetails {
        val type = try {
            DayType.valueOf(dayTypeStr)
        } catch (e: Exception) {
            DayType.ORDINARY
        }

        // Multipliers for the first 8 hours (regular shift)
        val regularMultiplier = when (type) {
            DayType.ORDINARY -> 1.00
            DayType.REST_DAY -> 1.30
            DayType.SPECIAL_NON_WORKING -> 1.30
            DayType.SPECIAL_NON_WORKING_REST -> 1.50
            DayType.REGULAR_HOLIDAY -> 2.00
            DayType.REGULAR_HOLIDAY_REST -> 2.60
            DayType.SPECIAL_WORKING -> 1.00
            DayType.SPECIAL_WORKING_REST -> 1.30
        }

        // Multipliers for beyond 8 hours (overtime)
        val overtimeMultiplier = when (type) {
            DayType.ORDINARY -> ordinaryOvertimeMultiplier
            DayType.REST_DAY -> 1.30 * 1.30          // 1.69
            DayType.SPECIAL_NON_WORKING -> 1.30 * 1.30 // 1.69
            DayType.SPECIAL_NON_WORKING_REST -> 1.50 * 1.30 // 1.95
            DayType.REGULAR_HOLIDAY -> 2.00 * 1.30 // 2.60
            DayType.REGULAR_HOLIDAY_REST -> 2.60 * 1.30 // 3.38
            DayType.SPECIAL_WORKING -> ordinaryOvertimeMultiplier
            DayType.SPECIAL_WORKING_REST -> 1.30 * 1.30 // Rest day overtime
        }

        val totalMinutes = ((endMillis - startMillis) / 60_000L).coerceAtLeast(0L)

        var totalPay = 0.0
        var regularMins = 0L
        var nightDiffMins = 0L
        var overtimeMins = 0L

        val perMinuteRate = hourlyRate / 60.0
        val zoneId = ZoneId.systemDefault()
        val safeNdStart = nightDiffStartHour.coerceIn(18, 23)

        var currentMillis = startMillis
        var minutesProcessed = 0L

        while (minutesProcessed < totalMinutes) {
            val zonedDateTime = Instant.ofEpochMilli(currentMillis).atZone(zoneId)
            val hour = zonedDateTime.hour

            // Night shift window: configured start hour (default 22) until 6:00 AM
            val isNightShift = hour >= safeNdStart || hour < 6

            val isOvertime = minutesProcessed >= 480 // 8 hours = 480 mins
            val currentMultiplier = if (isOvertime) overtimeMultiplier else regularMultiplier

            var minutePay = perMinuteRate * currentMultiplier

            if (isNightShift) {
                minutePay *= nightDiffMultiplier
                nightDiffMins++
            }

            if (isOvertime) {
                overtimeMins++
            }

            if (!isOvertime && !isNightShift) {
                regularMins++
            }

            totalPay += minutePay
            currentMillis += 60_000L
            minutesProcessed++
        }

        return WageDetails(
            totalEarned = totalPay,
            regularMinutes = regularMins,
            nightDiffMinutes = nightDiffMins,
            overtimeMinutes = overtimeMins
        )
    }
}
