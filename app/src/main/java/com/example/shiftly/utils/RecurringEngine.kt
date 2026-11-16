package com.example.shiftly.utils

import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

data class DueRecurringOccurrence(
    val rule: RecurringEntrySetting,
    val dueEpochDay: Long
)

object RecurringEngine {
    private const val MAX_CATCH_UP_OCCURRENCES = 12

    fun computeDueOccurrences(
        entry: RecurringEntrySetting,
        today: LocalDate
    ): List<DueRecurringOccurrence> {
        if (!entry.isEnabled || entry.amount <= 0.0 || entry.title.isBlank()) {
            return emptyList()
        }

        val todayEpoch = today.toEpochDay()
        if (entry.lastAppliedEpochDay <= 0L) {
            return listOf(DueRecurringOccurrence(rule = entry, dueEpochDay = todayEpoch))
        }
        if (entry.lastAppliedEpochDay >= todayEpoch) {
            return emptyList()
        }

        val isWeekly = entry.frequency.equals("Weekly", ignoreCase = true)
        var cursor = LocalDate.ofEpochDay(entry.lastAppliedEpochDay)
        val due = mutableListOf<DueRecurringOccurrence>()

        while (due.size < MAX_CATCH_UP_OCCURRENCES) {
            cursor = if (isWeekly) cursor.plusDays(7) else cursor.plusMonths(1)
            if (cursor.isAfter(today)) break
            due.add(DueRecurringOccurrence(rule = entry, dueEpochDay = cursor.toEpochDay()))
        }
        return due
    }

    /**
     * Computes the upcoming due date for an active recurring entry.
     */
    fun nextDueDate(entry: RecurringEntrySetting, today: LocalDate): LocalDate? {
        if (!entry.isEnabled || entry.amount <= 0.0 || entry.title.isBlank()) return null
        if (entry.lastAppliedEpochDay <= 0L) return today
        val isWeekly = entry.frequency.equals("Weekly", ignoreCase = true)
        val last = LocalDate.ofEpochDay(entry.lastAppliedEpochDay)
        return if (isWeekly) last.plusDays(7) else last.plusMonths(1)
    }

    /**
     * Returns active recurring bills (expenses) whose due date is less than 1 day away
     * (due today or due tomorrow).
     */
    fun findBillsDueWithinOneDay(
        entries: List<RecurringEntrySetting>,
        today: LocalDate
    ): List<RecurringEntrySetting> {
        return entries.filter { entry ->
            if (!entry.isEnabled || entry.isIncome || entry.amount <= 0.0 || entry.title.isBlank()) {
                return@filter false
            }
            val dueDate = nextDueDate(entry, today) ?: return@filter false
            val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
            daysUntilDue in 0L..1L
        }
    }

    fun isPaydayToday(payCycleSchedule: String, date: LocalDate): Boolean {
        return when (payCycleSchedule) {
            "Weekly (Fri)" -> date.dayOfWeek == DayOfWeek.FRIDAY
            "15th & 30th" -> {
                date.dayOfMonth == 15 ||
                    date.dayOfMonth == 30 ||
                    (date.month == Month.FEBRUARY && date.dayOfMonth == date.lengthOfMonth())
            }
            "Monthly" -> date.dayOfMonth == date.lengthOfMonth()
            else -> false
        }
    }

    fun shouldSendShiftReminder(
        settings: ShiftlySettingsSnapshot,
        hasLoggedShiftToday: Boolean,
        isTimerRunning: Boolean = false,
        hourOfDay: Int
    ): Boolean {
        return settings.notificationsEnabled &&
            settings.shiftReminderEnabled &&
            !hasLoggedShiftToday &&
            !isTimerRunning &&
            hourOfDay in 12..21
    }

    /**
     * Sends a reminder to log savings / add a deposit either on every payday cutoff
     * or daily (12:00–21:00) when no deposit has been logged yet today.
     */
    fun shouldSendSavingsDepositReminder(
        settings: ShiftlySettingsSnapshot,
        isPaydayToday: Boolean,
        hasDepositedToday: Boolean,
        hourOfDay: Int
    ): Boolean {
        if (!settings.notificationsEnabled || !settings.savingsReminderEnabled) return false
        if (hasDepositedToday) return false
        return isPaydayToday || hourOfDay in 12..21
    }
}
