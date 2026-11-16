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
            dayTypeStr = DayType.ORDINARY.name
        )

        assertEquals(400.0, details.totalEarned, 0.01)
        assertEquals(240L, details.regularMinutes)
        assertEquals(240L, details.regularBaseMinutes)
        assertEquals(0L, details.nightDiffMinutes)
        assertEquals(0L, details.overtimeMinutes)
        assertEquals(400.0, details.regularPay, 0.01)
        assertEquals(0.0, details.nightDiffPay, 0.01)
        assertEquals(0.0, details.overtimePay, 0.01)
        assertEquals(100.0, details.effectiveRegularHourlyRate, 0.01)
        assertEquals(125.0, details.effectiveOvertimeHourlyRate, 0.01)
        assertEquals(details.totalEarned, details.regularPay + details.nightDiffPay + details.overtimePay, 0.01)
    }

    @Test
    fun doleExample1_ordinaryDayPlusOvertime_matchesOfficialRate() {
        // Daily rate = 755.00 -> Hourly rate = 94.375; 8h regular + 2h OT = 990.94
        val date = LocalDate.of(2026, 5, 19)
        val start = millisAt(date, 8)
        val end = millisAt(date, 18) // 10h daytime

        val details = WageCalculator.calculateTotalEarned(
            startMillis = start,
            endMillis = end,
            hourlyRate = 755.0 / 8.0,
            dayTypeStr = DayType.ORDINARY.name
        )

        assertEquals(480L, details.regularBaseMinutes)
        assertEquals(0L, details.nightDiffMinutes)
        assertEquals(120L, details.overtimeMinutes)
        assertEquals(755.00, details.regularPay, 0.01)
        assertEquals(235.94, details.overtimePay, 0.01)
        assertEquals(990.94, details.totalEarned, 0.01)
    }

    @Test
    fun doleExample2_ordinaryDayFullNightShift_appliesFixed10PmTo6AmNsd() {
        // Daily rate = 755.00 -> Hourly rate = 94.375; 10 PM - 6 AM (8h NSD @ +10%) = 830.50
        val date = LocalDate.of(2026, 5, 19)
        val start = millisAt(date, 22)
        val end = millisAt(date.plusDays(1), 6)

        val details = WageCalculator.calculateTotalEarned(
            startMillis = start,
            endMillis = end,
            hourlyRate = 755.0 / 8.0,
            dayTypeStr = DayType.ORDINARY.name
        )

        assertEquals(480L, details.regularBaseMinutes)
        assertEquals(480L, details.nightDiffMinutes)
        assertEquals(0L, details.overtimeMinutes)
        assertEquals(755.00, details.regularPay, 0.01)
        assertEquals(75.50, details.nightDiffPay, 0.01)
        assertEquals(830.50, details.totalEarned, 0.01)
    }

    @Test
    fun doleExample3_specialNonWorkingHolidayPlusOvertime_applies130And169Percent() {
        // Daily rate = 755.00 -> Hourly rate = 94.375; 8h @ 130% = 981.50; 1h OT @ 169% = 159.49; Total = 1140.99
        val date = LocalDate.of(2026, 5, 19)
        val start = millisAt(date, 8)
        val end = millisAt(date, 17) // 9h daytime

        val details = WageCalculator.calculateTotalEarned(
            startMillis = start,
            endMillis = end,
            hourlyRate = 755.0 / 8.0,
            dayTypeStr = DayType.SPECIAL_NON_WORKING.name
        )

        assertEquals(480L, details.regularBaseMinutes)
        assertEquals(60L, details.overtimeMinutes)
        assertEquals(981.50, details.regularPay, 0.01)
        assertEquals(159.49, details.overtimePay, 0.01)
        assertEquals(1140.99, details.totalEarned, 0.01)
    }

    @Test
    fun doleExample4_regularHolidayPlusOvertimeAndNsd_matchesOfficialRate() {
        // Daily rate = 755.00 -> Hourly rate = 94.375
        // 2:00 AM - 12:00 PM (10h): 8h regular (4h in NSD 2 AM-6 AM) + 2h OT outside NSD
        // Regular = 1510.00, OT = 490.75, NSD = 75.50 -> Total = 2076.25
        val date = LocalDate.of(2026, 5, 19)
        val start = millisAt(date, 2)
        val end = millisAt(date, 12)

        val details = WageCalculator.calculateTotalEarned(
            startMillis = start,
            endMillis = end,
            hourlyRate = 755.0 / 8.0,
            dayTypeStr = DayType.REGULAR_HOLIDAY.name
        )

        assertEquals(480L, details.regularBaseMinutes)
        assertEquals(240L, details.nightDiffMinutes)
        assertEquals(120L, details.overtimeMinutes)
        assertEquals(1510.00, details.regularPay, 0.01)
        assertEquals(490.75, details.overtimePay, 0.01)
        assertEquals(75.50, details.nightDiffPay, 0.01)
        assertEquals(2076.25, details.totalEarned, 0.01)
    }

    @Test
    fun restDayAndCombinedHolidayRestRates_applyFixedDoleMultipliers() {
        val date = LocalDate.of(2026, 5, 18)
        val start = millisAt(date, 8)
        val end = millisAt(date, 18) // 10h daytime (8h base + 2h OT)

        val restDay = WageCalculator.calculateTotalEarned(start, end, 100.0, DayType.REST_DAY.name)
        assertEquals(1040.0, restDay.regularPay, 0.01) // 8 * 130
        assertEquals(338.0, restDay.overtimePay, 0.01) // 2 * 169
        assertEquals(1378.0, restDay.totalEarned, 0.01)

        val specialRest = WageCalculator.calculateTotalEarned(start, end, 100.0, DayType.SPECIAL_NON_WORKING_REST.name)
        assertEquals(1200.0, specialRest.regularPay, 0.01) // 8 * 150
        assertEquals(390.0, specialRest.overtimePay, 0.01) // 2 * 195
        assertEquals(1590.0, specialRest.totalEarned, 0.01)

        val regHolidayRest = WageCalculator.calculateTotalEarned(start, end, 100.0, DayType.REGULAR_HOLIDAY_REST.name)
        assertEquals(2080.0, regHolidayRest.regularPay, 0.01) // 8 * 260
        assertEquals(676.0, regHolidayRest.overtimePay, 0.01) // 2 * 338
        assertEquals(2756.0, regHolidayRest.totalEarned, 0.01)
    }

    @Test
    fun midnightToSixAmPreset_calculatesFullRegularBasePlusFullNightDiff() {
        val date = LocalDate.of(2026, 5, 18)
        val start = millisAt(date, 0) // 12:00 AM
        val end = millisAt(date, 6)   // 6:00 AM (6h total, all inside 10 PM - 6 AM)

        val details = WageCalculator.calculateTotalEarned(
            startMillis = start,
            endMillis = end,
            hourlyRate = 100.0,
            dayTypeStr = DayType.ORDINARY.name
        )

        assertEquals(360L, details.regularBaseMinutes)
        assertEquals(360L, details.nightDiffMinutes)
        assertEquals(0L, details.overtimeMinutes)
        assertEquals(600.0, details.regularPay, 0.01)
        assertEquals(60.0, details.nightDiffPay, 0.01)
        assertEquals(0.0, details.overtimePay, 0.01)
        assertEquals(660.0, details.totalEarned, 0.01)
        assertEquals(details.totalEarned, details.regularPay + details.nightDiffPay + details.overtimePay, 0.01)
    }

    @Test
    fun tenPmToEightAmShift_calculatesReceiptRegularNightDiffAndOvertime() {
        val date = LocalDate.of(2026, 5, 18)
        val start = millisAt(date, 22) // 10:00 PM
        val end = millisAt(date.plusDays(1), 8) // 8:00 AM (10h total: 8h reg + 2h OT; 8h ND 10PM-6AM)

        val details = WageCalculator.calculateTotalEarned(
            startMillis = start,
            endMillis = end,
            hourlyRate = 100.0,
            dayTypeStr = DayType.ORDINARY.name
        )

        assertEquals(480L, details.regularBaseMinutes)
        assertEquals(480L, details.nightDiffMinutes)
        assertEquals(120L, details.overtimeMinutes)
        assertEquals(800.0, details.regularPay, 0.01)
        assertEquals(80.0, details.nightDiffPay, 0.01)
        assertEquals(250.0, details.overtimePay, 0.01)
        assertEquals(1130.0, details.totalEarned, 0.01)
        assertEquals(details.totalEarned, details.regularPay + details.nightDiffPay + details.overtimePay, 0.01)
    }
}
