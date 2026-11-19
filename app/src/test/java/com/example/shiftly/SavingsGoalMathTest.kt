package com.example.shiftly

import com.example.shiftly.data.local.SavingsDepositEntity
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.utils.SavingsGoalMath
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsGoalMathTest {

    @Test
    fun computeProgress_calculatesSavedLeftAndNeededPerDay() {
        val today = LocalDate.of(2026, 5, 16)
        val goalWithDeposits = SavingsGoalWithDeposits(
            goal = SavingsGoalEntity(
                id = 1L,
                title = "Laptop",
                targetAmount = 10000.0,
                targetDateEpochDay = today.plusDays(10).toEpochDay(),
                isActive = true
            ),
            deposits = listOf(
                SavingsDepositEntity(id = 1L, goalId = 1L, amount = 1500.0, timestampMillis = 1000L),
                SavingsDepositEntity(id = 2L, goalId = 1L, amount = 500.0, timestampMillis = 2000L)
            )
        )

        val progress = SavingsGoalMath.computeProgress(goalWithDeposits, today)
        assertEquals(2000.0, progress.savedAmount, 0.001)
        assertEquals(8000.0, progress.leftToSave, 0.001)
        assertEquals(10L, progress.daysRemaining)
        assertEquals(800.0, progress.neededPerDay, 0.001)
        assertEquals(0.2f, progress.progressFraction, 0.001f)
        assertFalse(progress.isCompleted)
        assertFalse(progress.isPastDue)
    }

    @Test
    fun computeProgress_completedGoalCapsLeftAndNeededAtZero() {
        val today = LocalDate.of(2026, 5, 16)
        val progress = SavingsGoalMath.computeProgress(
            savedAmount = 5500.0,
            targetAmount = 5000.0,
            targetDate = today.plusDays(5),
            today = today
        )

        assertEquals(0.0, progress.leftToSave, 0.001)
        assertEquals(0.0, progress.neededPerDay, 0.001)
        assertEquals(1.0f, progress.progressFraction, 0.001f)
        assertTrue(progress.isCompleted)
        assertFalse(progress.isPastDue)
    }

    @Test
    fun computeProgress_pastDueGoalRequiresFullRemainingAmount() {
        val today = LocalDate.of(2026, 5, 16)
        val progress = SavingsGoalMath.computeProgress(
            savedAmount = 1000.0,
            targetAmount = 3000.0,
            targetDate = today.minusDays(2),
            today = today
        )

        assertEquals(2000.0, progress.leftToSave, 0.001)
        assertEquals(0L, progress.daysRemaining)
        assertEquals(2000.0, progress.neededPerDay, 0.001)
        assertTrue(progress.isPastDue)
        assertFalse(progress.isCompleted)
    }
}
