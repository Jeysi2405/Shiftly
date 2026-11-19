package com.example.shiftly

import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.utils.CutoffCalculator
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CutoffCalculatorTest {

    @Test
    fun resolveCutoffWindow_firstCutoffCoversDay1To15() {
        val window = CutoffCalculator.resolveCutoffWindow(LocalDate.of(2026, 5, 10))
        assertTrue(window.isFirstCutoff)
        assertEquals(LocalDate.of(2026, 5, 1), window.startDate)
        assertEquals(LocalDate.of(2026, 5, 15), window.endDate)
        assertEquals("1st Cutoff • May 1–15", window.label)
    }

    @Test
    fun resolveCutoffWindow_secondCutoffAdaptsToMonthLength() {
        val mayWindow = CutoffCalculator.resolveCutoffWindow(LocalDate.of(2026, 5, 16))
        assertFalse(mayWindow.isFirstCutoff)
        assertEquals(LocalDate.of(2026, 5, 16), mayWindow.startDate)
        assertEquals(LocalDate.of(2026, 5, 31), mayWindow.endDate)

        val febNonLeap = CutoffCalculator.resolveCutoffWindow(LocalDate.of(2025, 2, 20))
        assertEquals(LocalDate.of(2025, 2, 16), febNonLeap.startDate)
        assertEquals(LocalDate.of(2025, 2, 28), febNonLeap.endDate)

        val febLeap = CutoffCalculator.resolveCutoffWindow(LocalDate.of(2024, 2, 20))
        assertEquals(LocalDate.of(2024, 2, 29), febLeap.endDate)
    }

    @Test
    fun summarizeCutoffShifts_resetsWhenMovingAcrossDay15Boundary() {
        val day14 = LocalDate.of(2026, 5, 14).toEpochDay()
        val day15 = LocalDate.of(2026, 5, 15).toEpochDay()
        val day16 = LocalDate.of(2026, 5, 16).toEpochDay()
        val day18 = LocalDate.of(2026, 5, 18).toEpochDay()

        val shifts = listOf(
            ShiftEntity(
                id = 1, dateEpochDay = day14, timeInMillis = 0, timeOutMillis = 0,
                durationMinutes = 240, lockedHourlyRate = 100.0, totalEarned = 400.0
            ),
            ShiftEntity(
                id = 2, dateEpochDay = day15, timeInMillis = 0, timeOutMillis = 0,
                durationMinutes = 180, lockedHourlyRate = 100.0, totalEarned = 300.0
            ),
            ShiftEntity(
                id = 3, dateEpochDay = day16, timeInMillis = 0, timeOutMillis = 0,
                durationMinutes = 300, lockedHourlyRate = 100.0, totalEarned = 500.0
            ),
            ShiftEntity(
                id = 4, dateEpochDay = day18, timeInMillis = 0, timeOutMillis = 0,
                durationMinutes = 120, lockedHourlyRate = 100.0, totalEarned = 200.0
            ),
            // Second shift on day 18 to verify distinct day counting
            ShiftEntity(
                id = 5, dateEpochDay = day18, timeInMillis = 0, timeOutMillis = 0,
                durationMinutes = 60, lockedHourlyRate = 100.0, totalEarned = 100.0
            )
        )

        val firstCutoffSummary = CutoffCalculator.summarizeCutoffShifts(
            shifts = shifts,
            referenceDate = LocalDate.of(2026, 5, 15)
        )
        assertEquals(700.0, firstCutoffSummary.totalEarnings, 0.001)
        assertEquals(2, firstCutoffSummary.shiftDaysCount)
        assertEquals(7.0, firstCutoffSummary.totalHours, 0.001)

        val secondCutoffSummary = CutoffCalculator.summarizeCutoffShifts(
            shifts = shifts,
            referenceDate = LocalDate.of(2026, 5, 20)
        )
        assertEquals(800.0, secondCutoffSummary.totalEarnings, 0.001)
        assertEquals(2, secondCutoffSummary.shiftDaysCount)
        assertEquals(8.0, secondCutoffSummary.totalHours, 0.001)
    }
}
