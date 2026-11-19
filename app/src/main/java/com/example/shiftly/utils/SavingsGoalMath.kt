package com.example.shiftly.utils

import androidx.compose.runtime.Immutable
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Immutable
data class SavingsGoalProgress(
    val savedAmount: Double,
    val targetAmount: Double,
    val leftToSave: Double,
    val daysRemaining: Long,
    val neededPerDay: Double,
    val progressFraction: Float,
    val isCompleted: Boolean,
    val isPastDue: Boolean
)

object SavingsGoalMath {

    fun computeProgress(
        goalWithDeposits: SavingsGoalWithDeposits,
        today: LocalDate = LocalDate.now()
    ): SavingsGoalProgress {
        val saved = goalWithDeposits.deposits.sumOf { it.amount }.coerceAtLeast(0.0)
        val target = goalWithDeposits.goal.targetAmount.coerceAtLeast(0.0)
        val targetDate = LocalDate.ofEpochDay(goalWithDeposits.goal.targetDateEpochDay)
        return computeProgress(
            savedAmount = saved,
            targetAmount = target,
            targetDate = targetDate,
            today = today
        )
    }

    fun computeProgress(
        savedAmount: Double,
        targetAmount: Double,
        targetDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): SavingsGoalProgress {
        val safeSaved = savedAmount.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        val safeTarget = targetAmount.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        val left = (safeTarget - safeSaved).coerceAtLeast(0.0)
        val rawDays = ChronoUnit.DAYS.between(today, targetDate)
        val isPastDue = rawDays < 0L
        val effectiveDays = rawDays.coerceAtLeast(1L)
        val neededPerDay = if (left <= 0.0) 0.0 else left / effectiveDays.toDouble()
        val fraction = if (safeTarget > 0.0) {
            (safeSaved / safeTarget).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
        return SavingsGoalProgress(
            savedAmount = safeSaved,
            targetAmount = safeTarget,
            leftToSave = left,
            daysRemaining = rawDays.coerceAtLeast(0L),
            neededPerDay = neededPerDay,
            progressFraction = fraction,
            isCompleted = safeTarget > 0.0 && left <= 0.0,
            isPastDue = isPastDue
        )
    }
}
