package com.example.shiftly.utils

import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

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
        isTimerRunning: Boolean,
        hourOfDay: Int
    ): Boolean {
        return settings.notificationsEnabled &&
            settings.shiftReminderEnabled &&
            !hasLoggedShiftToday &&
            !isTimerRunning &&
            hourOfDay in 12..21
    }
}
