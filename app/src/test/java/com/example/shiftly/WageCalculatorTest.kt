package com.example.shiftly

import com.example.shiftly.utils.DayType
import com.example.shiftly.utils.WageCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WageCalculatorTest {

    private fun millisAt(date: LocalDate, hour: Int, minute: Int = 0): Long =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    @Test
    fun ordinaryDayShift_calculatesExactRegularHours() {
        val date = LocalDate.of(2026, 5, 18)
        val start = millisAt(date, 9)
        val end = millisAt(date, 13) // 4h regular

        val details = WageCalculator.calculateTotalEarned(
            startMillis = start,
            endMillis = end,
            hourlyRate = 100.0,
            dayTypeStr = DayType.ORDINARY.name,
            nightDiffStartHour = 22,
            nightDiffMultiplier = 1.10,
            ordinaryOvertimeMultiplier = 1.25
        )

        assertEquals(400.0, details.totalEarned, 0.01)
        assertEquals(240L, details.regularMinutes)
        assertEquals(0L, details.nightDiffMinutes)
        assertEquals(0L, details.overtimeMinutes)
    }

    @Test
    fun customNightDiffStartAndMultiplier_appliesAcrossMidnight() {
        val date = LocalDate.of(2026, 5, 18)
        val start = millisAt(date, 20) // 8 PM
        val end = millisAt(date.plusDays(1), 2) // 2 AM (6h total)

        // With ND start at 21 (9 PM) and +20% ND multiplier:
        // 20:00-21:00 = 1h regular @ 1.0x = 100.0
        // 21:00-02:00 = 5h ND @ 1.20x = 600.0
        val details = WageCalculator.calculateTotalEarned(
            startMillis = start,
            endMillis = end,
            hourlyRate = 100.0,
            dayTypeStr = DayType.ORDINARY.name,
            nightDiffStartHour = 21,
            nightDiffMultiplier = 1.20,
            ordinaryOvertimeMultiplier = 1.25
        )

        assertEquals(700.0, details.totalEarned, 0.01)
        assertEquals(60L, details.regularMinutes)
        assertEquals(300L, details.nightDiffMinutes)
        assertEquals(0L, details.overtimeMinutes)
    }

    @Test
    fun customOvertimeMultiplier_appliesAfterEightHours() {
        val date = LocalDate.of(2026, 5, 18)
        val start = millisAt(date, 8)
        val end = millisAt(date, 18) // 10h daytime (8h regular + 2h OT)

        // 8h * 100 + 2h * 100 * 1.50 = 800 + 300 = 1100
        val details = WageCalculator.calculateTotalEarned(
            startMillis = start,
            endMillis = end,
            hourlyRate = 100.0,
            dayTypeStr = DayType.ORDINARY.name,
            nightDiffStartHour = 22,
            nightDiffMultiplier = 1.10,
            ordinaryOvertimeMultiplier = 1.50
        )

        assertEquals(1100.0, details.totalEarned, 0.01)
        assertEquals(480L, details.regularMinutes)
        assertEquals(0L, details.nightDiffMinutes)
        assertEquals(120L, details.overtimeMinutes)
    }

    @Test
    fun regularHolidayShift_appliesDoubleRateAndHolidayOvertime() {
        val date = LocalDate.of(2026, 5, 18)
        val start = millisAt(date, 8)
        val end = millisAt(date, 17) // 9h daytime (8h @ 2.0x + 1h @ 2.6x)

        // 8 * 200 + 1 * 260 = 1860
        val details = WageCalculator.calculateTotalEarned(
            startMillis = start,
            endMillis = end,
            hourlyRate = 100.0,
            dayTypeStr = DayType.REGULAR_HOLIDAY.name
        )

        assertEquals(1860.0, details.totalEarned, 0.01)
        assertEquals(480L, details.regularMinutes)
        assertEquals(60L, details.overtimeMinutes)
    }
}
