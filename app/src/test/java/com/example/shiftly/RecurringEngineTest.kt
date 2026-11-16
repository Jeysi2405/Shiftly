package com.example.shiftly

import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.utils.RecurringEngine
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringEngineTest {

    @Test
    fun computeDueOccurrences_firstRunPostsTodayAndSubsequentSameDaySkips() {
        val today = LocalDate.of(2026, 5, 18)
        val rule = RecurringEntrySetting(
            id = "r1",
            title = "Weekly Allowance",
            amount = 1500.0,
            frequency = "Weekly",
            isIncome = true,
            isEnabled = true,
            lastAppliedEpochDay = 0L
        )

        val firstDue = RecurringEngine.computeDueOccurrences(rule, today)
        assertEquals(1, firstDue.size)
        assertEquals(today.toEpochDay(), firstDue.first().dueEpochDay)

        val updatedRule = rule.copy(lastAppliedEpochDay = today.toEpochDay())
        val sameDayDue = RecurringEngine.computeDueOccurrences(updatedRule, today)
        assertTrue(sameDayDue.isEmpty())
    }

    @Test
    fun computeDueOccurrences_catchesUpMissedWeeksWithoutExceedingToday() {
        val start = LocalDate.of(2026, 5, 1)
        val today = LocalDate.of(2026, 5, 18) // 17 days later -> +7d (May 8), +14d (May 15) = 2 due
        val rule = RecurringEntrySetting(
            id = "r1",
            title = "Weekly Allowance",
            amount = 1000.0,
            frequency = "Weekly",
            isIncome = true,
            isEnabled = true,
            lastAppliedEpochDay = start.toEpochDay()
        )

        val due = RecurringEngine.computeDueOccurrences(rule, today)
        assertEquals(2, due.size)
        assertEquals(LocalDate.of(2026, 5, 8).toEpochDay(), due[0].dueEpochDay)
        assertEquals(LocalDate.of(2026, 5, 15).toEpochDay(), due[1].dueEpochDay)
    }

    @Test
    fun computeDueOccurrences_monthlyRuleRespectsCalendarMonthAndDisabledState() {
        val jan31 = LocalDate.of(2026, 1, 31)
        val mar31 = LocalDate.of(2026, 3, 31)
        val rule = RecurringEntrySetting(
            id = "r2",
            title = "Campus Wi-Fi",
            amount = 199.0,
            frequency = "Monthly",
            isIncome = false,
            isEnabled = true,
            category = "Bills",
            lastAppliedEpochDay = jan31.toEpochDay()
        )

        val due = RecurringEngine.computeDueOccurrences(rule, mar31)
        assertEquals(2, due.size)
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), due[0].dueEpochDay)
        assertEquals(LocalDate.of(2026, 3, 28).toEpochDay(), due[1].dueEpochDay)

        val disabledDue = RecurringEngine.computeDueOccurrences(rule.copy(isEnabled = false), mar31)
        assertTrue(disabledDue.isEmpty())
    }

    @Test
    fun findBillsDueWithinOneDay_includesZeroOrOneDayBillsAndSkipsOthers() {
        val today = LocalDate.of(2026, 5, 18)

        val dueTodayBill = RecurringEntrySetting(
            id = "b_today",
            title = "Water Bill",
            amount = 450.0,
            frequency = "Weekly",
            isIncome = false,
            isEnabled = true,
            lastAppliedEpochDay = today.minusDays(7).toEpochDay() // due today
        )
        val dueTomorrowBill = RecurringEntrySetting(
            id = "b_tomorrow",
            title = "Internet Bill",
            amount = 999.0,
            frequency = "Weekly",
            isIncome = false,
            isEnabled = true,
            lastAppliedEpochDay = today.minusDays(6).toEpochDay() // due tomorrow (+1 day)
        )
        val dueInTwoDaysBill = RecurringEntrySetting(
            id = "b_later",
            title = "Phone Plan",
            amount = 599.0,
            frequency = "Weekly",
            isIncome = false,
            isEnabled = true,
            lastAppliedEpochDay = today.minusDays(5).toEpochDay() // due in 2 days
        )
        val incomeRuleDueTomorrow = RecurringEntrySetting(
            id = "inc_tomorrow",
            title = "Allowance",
            amount = 1500.0,
            frequency = "Weekly",
            isIncome = true,
            isEnabled = true,
            lastAppliedEpochDay = today.minusDays(6).toEpochDay()
        )
        val disabledBill = dueTodayBill.copy(id = "b_disabled", isEnabled = false)

        val result = RecurringEngine.findBillsDueWithinOneDay(
            entries = listOf(dueTodayBill, dueTomorrowBill, dueInTwoDaysBill, incomeRuleDueTomorrow, disabledBill),
            today = today
        )

        assertEquals(listOf("b_today", "b_tomorrow"), result.map { it.id })
    }

    @Test
    fun isPaydayToday_handlesSchedulesIncludingFebruaryCutoff() {
        assertTrue(RecurringEngine.isPaydayToday("Weekly (Fri)", LocalDate.of(2026, 5, 15))) // Friday
        assertFalse(RecurringEngine.isPaydayToday("Weekly (Fri)", LocalDate.of(2026, 5, 18))) // Monday

        assertTrue(RecurringEngine.isPaydayToday("15th & 30th", LocalDate.of(2026, 2, 28))) // End of Feb
        assertTrue(RecurringEngine.isPaydayToday("15th & 30th", LocalDate.of(2026, 5, 15)))
        assertTrue(RecurringEngine.isPaydayToday("15th & 30th", LocalDate.of(2026, 5, 30)))
        assertFalse(RecurringEngine.isPaydayToday("15th & 30th", LocalDate.of(2026, 5, 29)))

        assertTrue(RecurringEngine.isPaydayToday("Monthly", LocalDate.of(2026, 5, 31)))
        assertFalse(RecurringEngine.isPaydayToday("Monthly", LocalDate.of(2026, 5, 30)))
    }

    @Test
    fun shouldSendShiftReminder_respectsSettingsTimerAndLoggedShift() {
        val enabled = ShiftlySettingsSnapshot(notificationsEnabled = true, shiftReminderEnabled = true)
        assertTrue(RecurringEngine.shouldSendShiftReminder(enabled, hasLoggedShiftToday = false, isTimerRunning = false, hourOfDay = 15))
        assertFalse(RecurringEngine.shouldSendShiftReminder(enabled, hasLoggedShiftToday = true, isTimerRunning = false, hourOfDay = 15))
        assertFalse(RecurringEngine.shouldSendShiftReminder(enabled, hasLoggedShiftToday = false, isTimerRunning = true, hourOfDay = 15))
        assertFalse(RecurringEngine.shouldSendShiftReminder(enabled, hasLoggedShiftToday = false, isTimerRunning = false, hourOfDay = 9))
        assertFalse(RecurringEngine.shouldSendShiftReminder(enabled.copy(notificationsEnabled = false), hasLoggedShiftToday = false, isTimerRunning = false, hourOfDay = 15))
    }

    @Test
    fun shouldSendSavingsDepositReminder_respectsPaydayDailyWindowAndDepositState() {
        val enabled = ShiftlySettingsSnapshot(notificationsEnabled = true, savingsReminderEnabled = true)

        assertTrue(RecurringEngine.shouldSendSavingsDepositReminder(enabled, isPaydayToday = true, hasDepositedToday = false, hourOfDay = 8))
        assertTrue(RecurringEngine.shouldSendSavingsDepositReminder(enabled, isPaydayToday = false, hasDepositedToday = false, hourOfDay = 15))
        assertFalse(RecurringEngine.shouldSendSavingsDepositReminder(enabled, isPaydayToday = false, hasDepositedToday = false, hourOfDay = 9))
        assertFalse(RecurringEngine.shouldSendSavingsDepositReminder(enabled, isPaydayToday = true, hasDepositedToday = true, hourOfDay = 15))
        assertFalse(RecurringEngine.shouldSendSavingsDepositReminder(enabled.copy(savingsReminderEnabled = false), isPaydayToday = true, hasDepositedToday = false, hourOfDay = 15))
        assertFalse(RecurringEngine.shouldSendSavingsDepositReminder(enabled.copy(notificationsEnabled = false), isPaydayToday = true, hasDepositedToday = false, hourOfDay = 15))
    }
}
