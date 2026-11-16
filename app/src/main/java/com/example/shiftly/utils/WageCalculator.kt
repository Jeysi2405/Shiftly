package com.example.shiftly.utils

import java.time.Instant
import java.time.ZoneId

enum class DayType(
    val label: String,
    val multiplierSummary: String,
    val holidayBadge: String? = null
) {
    ORDINARY("Regular Day", "", null),
    SPECIAL_NON_WORKING("Special Non-Working Holiday", "", "30%"),
    REGULAR_HOLIDAY("Regular Holiday", "", "Double Pay"),
    SPECIAL_WORKING("Special Working Holiday", "", null),
    SPECIAL_WORKING_REST("Special Working + Rest Day", "", "30%"),
    SPECIAL_NON_WORKING_REST("Special Non-Working + Rest Day", "", "50%"),
    REGULAR_HOLIDAY_REST("Regular Holiday + Rest Day", "", "160%"),
    REST_DAY("Rest Day", "", null);

    companion object {
        val orderedForSelection: List<DayType> = listOf(
            ORDINARY,
            SPECIAL_NON_WORKING,
            REGULAR_HOLIDAY,
            SPECIAL_WORKING,
            SPECIAL_WORKING_REST,
            SPECIAL_NON_WORKING_REST,
            REGULAR_HOLIDAY_REST
        )
    }
}

data class WageDetails(
    val totalEarned: Double,
    val regularMinutes: Long,
    val nightDiffMinutes: Long,
    val overtimeMinutes: Long,
    val regularBaseMinutes: Long = 0L,
    val regularPay: Double = 0.0,
    val nightDiffPay: Double = 0.0,
    val overtimePay: Double = 0.0,
    val effectiveRegularHourlyRate: Double = 0.0,
    val effectiveOvertimeHourlyRate: Double = 0.0
)

/**
 * Computes shift earnings using fixed Philippine DOLE labor rules:
 * - Regular/Holiday base rates (first 8 hours) and OT rates (excess of 8 hours)
 * - Fixed Night Shift Differential (NSD): 10:00 PM to 6:00 AM at +10% stacked on applicable rate
 */
object WageCalculator {
    const val FIXED_NIGHT_DIFF_START_HOUR = 22 // 10:00 PM
    const val FIXED_NIGHT_DIFF_END_HOUR = 6    // 6:00 AM
    const val FIXED_NIGHT_DIFF_MULTIPLIER = 1.10 // +10% NSD
    const val FIXED_ORDINARY_OVERTIME_MULTIPLIER = 1.25 // +25% Ordinary OT
    const val FIXED_HOLIDAY_REST_OT_FACTOR = 1.30 // +30% of applicable day rate

    fun calculateTotalEarned(
        startMillis: Long,
        endMillis: Long,
        hourlyRate: Double,
        dayTypeStr: String
    ): WageDetails {
        val type = try {
            DayType.valueOf(dayTypeStr)
        } catch (_: Exception) {
            DayType.ORDINARY
        }

        // Fixed DOLE multipliers for the first 8 hours (regular shift)
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

        // Fixed DOLE multipliers for beyond 8 hours (overtime)
        val overtimeMultiplier = when (type) {
            DayType.ORDINARY -> FIXED_ORDINARY_OVERTIME_MULTIPLIER
            DayType.REST_DAY -> 1.30 * FIXED_HOLIDAY_REST_OT_FACTOR               // 1.69
            DayType.SPECIAL_NON_WORKING -> 1.30 * FIXED_HOLIDAY_REST_OT_FACTOR    // 1.69
            DayType.SPECIAL_NON_WORKING_REST -> 1.50 * FIXED_HOLIDAY_REST_OT_FACTOR // 1.95
            DayType.REGULAR_HOLIDAY -> 2.00 * FIXED_HOLIDAY_REST_OT_FACTOR        // 2.60
            DayType.REGULAR_HOLIDAY_REST -> 2.60 * FIXED_HOLIDAY_REST_OT_FACTOR   // 3.38
            DayType.SPECIAL_WORKING -> FIXED_ORDINARY_OVERTIME_MULTIPLIER
            DayType.SPECIAL_WORKING_REST -> 1.30 * FIXED_HOLIDAY_REST_OT_FACTOR   // 1.69
        }

        val totalMinutes = ((endMillis - startMillis) / 60_000L).coerceAtLeast(0L)

        var totalPay = 0.0
        var regularMins = 0L
        var regularBaseMins = 0L
        var nightDiffMins = 0L
        var overtimeMins = 0L

        var regularPay = 0.0
        var nightDiffPay = 0.0
        var overtimePay = 0.0

        val safeRate = if (hourlyRate.isFinite() && hourlyRate > 0.0) hourlyRate else 0.0
        val perMinuteRate = safeRate / 60.0
        val zoneId = ZoneId.systemDefault()

        var currentMillis = startMillis
        var minutesProcessed = 0L

        while (minutesProcessed < totalMinutes) {
            val zonedDateTime = Instant.ofEpochMilli(currentMillis).atZone(zoneId)
            val hour = zonedDateTime.hour

            // Fixed DOLE Night Shift Differential window: 10:00 PM (22) until 6:00 AM (6)
            val isNightShift = hour >= FIXED_NIGHT_DIFF_START_HOUR || hour < FIXED_NIGHT_DIFF_END_HOUR

            val isOvertime = minutesProcessed >= 480 // 8 hours = 480 mins
            val currentMultiplier = if (isOvertime) overtimeMultiplier else regularMultiplier

            val baseMinutePay = perMinuteRate * currentMultiplier
            var minutePay = baseMinutePay

            if (isOvertime) {
                overtimeMins++
                overtimePay += baseMinutePay
            } else {
                regularBaseMins++
                regularPay += baseMinutePay
            }

            if (isNightShift) {
                val ndBonus = baseMinutePay * (FIXED_NIGHT_DIFF_MULTIPLIER - 1.0)
                minutePay *= FIXED_NIGHT_DIFF_MULTIPLIER
                nightDiffMins++
                nightDiffPay += ndBonus
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
            overtimeMinutes = overtimeMins,
            regularBaseMinutes = regularBaseMins,
            regularPay = regularPay,
            nightDiffPay = nightDiffPay,
            overtimePay = overtimePay,
            effectiveRegularHourlyRate = safeRate * regularMultiplier,
            effectiveOvertimeHourlyRate = safeRate * overtimeMultiplier
        )
    }
}
