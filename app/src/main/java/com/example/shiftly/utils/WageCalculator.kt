package com.example.shiftly.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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

object WageCalculator {

    fun calculateTotalEarned(
        startMillis: Long,
        endMillis: Long,
        hourlyRate: Double,
        dayTypeStr: String
    ): Double {
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
            DayType.ORDINARY -> 1.25
            DayType.REST_DAY -> 1.30 * 1.30          // 1.69
            DayType.SPECIAL_NON_WORKING -> 1.30 * 1.30 // 1.69
            DayType.SPECIAL_NON_WORKING_REST -> 1.50 * 1.30 // 1.95
            DayType.REGULAR_HOLIDAY -> 2.00 * 1.30 // 2.60
            DayType.REGULAR_HOLIDAY_REST -> 2.60 * 1.30 // 3.38
            DayType.SPECIAL_WORKING -> 1.25 // Ordinary overtime rate? Wait, markdown says nothing special about special working overtime. Let's assume ordinary * 1.25, or 1.0 * 1.25
            DayType.SPECIAL_WORKING_REST -> 1.30 * 1.30 // Rest day overtime
        }

        val totalMinutes = ((endMillis - startMillis) / 60_000L).coerceAtLeast(0L)

        // We will calculate minute by minute to accurately apply night differential
        // 10:00 PM to 6:00 AM (22:00 to 06:00) is night differential
        var totalPay = 0.0
        val perMinuteRate = hourlyRate / 60.0

        val zoneId = ZoneId.systemDefault()

        var currentMillis = startMillis
        var minutesProcessed = 0L

        while (minutesProcessed < totalMinutes) {
            val zonedDateTime = Instant.ofEpochMilli(currentMillis).atZone(zoneId)
            val hour = zonedDateTime.hour

            // Night shift if hour is >= 22 or < 6
            val isNightShift = hour >= 22 || hour < 6

            val isOvertime = minutesProcessed >= 480 // 8 hours = 480 mins
            val currentMultiplier = if (isOvertime) overtimeMultiplier else regularMultiplier

            var minutePay = perMinuteRate * currentMultiplier

            if (isNightShift) {
                // Night shift adds 10% to the *applicable* hourly rate
                minutePay *= 1.10
            }

            totalPay += minutePay
            currentMillis += 60_000L
            minutesProcessed++
        }

        return totalPay
    }
}