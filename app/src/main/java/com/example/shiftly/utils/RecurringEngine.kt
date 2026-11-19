package com.example.shiftly.utils

import androidx.compose.runtime.Immutable
import com.example.shiftly.data.local.CutoffScheduleConfig
import com.example.shiftly.data.local.RecurringBillEntity
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.temporal.ChronoUnit

@Immutable
data class DueRecurringOccurrence(
    val rule: RecurringEntrySetting,
    val dueEpochDay: Long
)

object RecurringEngine {
    private const val MAX_CATCH_UP_OCCURRENCES = 12

    fun dueDateTimeOf(bill: RecurringBillEntity): LocalDateTime {
        val date = LocalDate.ofEpochDay(bill.dueEpochDay)
        val time = LocalTime.of(bill.dueHour.coerceIn(0, 23), bill.dueMinute.coerceIn(0, 59))
        return LocalDateTime.of(date, time)
    }

    fun advanceDate(date: LocalDate, frequency: String): LocalDate {
        val isWeekly = frequency.equals("Weekly", ignoreCase = true)
        return if (isWeekly) date.plusDays(7) else date.plusMonths(1)
    }

    /**
     * Evaluates a Room [RecurringBillEntity] against exact [now] timestamp.
     * Returns all due [LocalDateTime] occurrences up to [MAX_CATCH_UP_OCCURRENCES],
     * starting from [bill.dueEpochDay] at [bill.dueHour]:[bill.dueMinute].
     */
    fun computeDueBillOccurrences(
        bill: RecurringBillEntity,
        now: LocalDateTime = LocalDateTime.now()
    ): List<LocalDateTime> {
        if (!bill.isEnabled || bill.amount <= 0.0 || bill.title.isBlank()) {
            return emptyList()
        }
        val results = mutableListOf<LocalDateTime>()
        var cursorDate = LocalDate.ofEpochDay(bill.dueEpochDay)
        val dueTime = LocalTime.of(bill.dueHour.coerceIn(0, 23), bill.dueMinute.coerceIn(0, 59))

        while (results.size < MAX_CATCH_UP_OCCURRENCES) {
            val candidate = LocalDateTime.of(cursorDate, dueTime)
            if (candidate.isAfter(now)) break
            results.add(candidate)
            cursorDate = advanceDate(cursorDate, bill.frequency)
        }
        return results
    }

    /**
     * Advances a bill's due date strictly past [now], returning the next scheduled [LocalDate].
     */
    fun nextScheduledDueDateAfter(
        bill: RecurringBillEntity,
        now: LocalDateTime = LocalDateTime.now()
    ): LocalDate {
        var cursorDate = LocalDate.ofEpochDay(bill.dueEpochDay)
        val dueTime = LocalTime.of(bill.dueHour.coerceIn(0, 23), bill.dueMinute.coerceIn(0, 59))
        var guard = 0
        while (!LocalDateTime.of(cursorDate, dueTime).isAfter(now) && guard < 120) {
            cursorDate = advanceDate(cursorDate, bill.frequency)
            guard++
        }
        return cursorDate
    }

    fun findBillsDueWithinOneDay(
        bills: List<RecurringBillEntity>,
        now: LocalDateTime = LocalDateTime.now()
    ): List<RecurringBillEntity> {
        val cutoff = now.plusHours(24)
        return bills.filter { bill ->
            if (!bill.isEnabled || bill.amount <= 0.0 || bill.title.isBlank()) {
                return@filter false
            }
            val due = dueDateTimeOf(bill)
            !due.isBefore(now) && !due.isAfter(cutoff)
        }
    }

    fun earliestNextDueDateTime(bills: List<RecurringBillEntity>): LocalDateTime? {
        return bills
            .asSequence()
            .filter { it.isEnabled && it.amount > 0.0 && it.title.isNotBlank() }
            .map { dueDateTimeOf(it) }
            .minOrNull()
    }

    fun millisUntilDue(due: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): Long {
        return Duration.between(now, due).toMillis().coerceAtLeast(1_000L)
    }

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
            else -> isPaydayToday(CutoffScheduleConfig.fromLegacySchedule(payCycleSchedule), date)
        }
    }

    fun isPaydayToday(config: CutoffScheduleConfig, date: LocalDate): Boolean {
        val window = CutoffCalculator.resolveCutoffWindow(date = date, config = config)
        return window.endDate == date
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
