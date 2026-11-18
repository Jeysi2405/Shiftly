package com.example.shiftly

import com.example.shiftly.data.local.CutoffScheduleConfig
import com.example.shiftly.data.local.RecurringBillEntity
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.utils.CutoffCalculator
import com.example.shiftly.utils.RecurringEngine
import java.time.LocalDate
import java.time.LocalDateTime
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
    fun computeDueBillOccurrences_respectsExactHourMinuteAndAdvancesSchedule() {
        val dueDate = LocalDate.of(2026, 6, 15)
        val bill = RecurringBillEntity(
            id = 5L,
            title = "Fiber Internet",
            amount = 1499.0,
            category = "Bills",
            frequency = "Monthly",
            dueEpochDay = dueDate.toEpochDay(),
            dueHour = 14,
            dueMinute = 30,
            isEnabled = true
        )

        val oneMinuteBefore = LocalDateTime.of(2026, 6, 15, 14, 29)
        assertTrue(RecurringEngine.computeDueBillOccurrences(bill, oneMinuteBefore).isEmpty())

        val exactTime = LocalDateTime.of(2026, 6, 15, 14, 30)
        val due = RecurringEngine.computeDueBillOccurrences(bill, exactTime)
        assertEquals(1, due.size)
        assertEquals(exactTime, due.first())
        assertEquals(LocalDate.of(2026, 7, 15), RecurringEngine.nextScheduledDueDateAfter(bill, exactTime))
    }

    @Test
    fun computeDueBillOccurrences_catchesUpMissedMonthsAndRespectsDisabledFlag() {
        val bill = RecurringBillEntity(
            id = 6L,
            title = "Dorm Rent",
            amount = 4500.0,
            category = "Bills",
            frequency = "Monthly",
            dueEpochDay = LocalDate.of(2026, 1, 31).toEpochDay(),
            dueHour = 9,
            dueMinute = 0,
            isEnabled = true
        )
        val now = LocalDateTime.of(2026, 4, 30, 10, 0)
        val due = RecurringEngine.computeDueBillOccurrences(bill, now)
        assertEquals(
            listOf(
                LocalDateTime.of(2026, 1, 31, 9, 0),
                LocalDateTime.of(2026, 2, 28, 9, 0),
                LocalDateTime.of(2026, 3, 28, 9, 0),
                LocalDateTime.of(2026, 4, 28, 9, 0)
            ),
            due
        )
        assertTrue(RecurringEngine.computeDueBillOccurrences(bill.copy(isEnabled = false), now).isEmpty())
    }

    @Test
    fun findBillsDueWithinOneDay_forRoomBills_checksExact24HourWindow() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val dueIn12Hours = RecurringBillEntity(
            id = 1L,
            title = "Water",
            amount = 300.0,
            category = "Bills",
            frequency = "Monthly",
            dueEpochDay = LocalDate.of(2026, 6, 15).toEpochDay(),
            dueHour = 22,
            dueMinute = 0,
            isEnabled = true
        )
        val dueIn24Hours = dueIn12Hours.copy(
            id = 2L,
            title = "Power",
            dueEpochDay = LocalDate.of(2026, 6, 16).toEpochDay(),
            dueHour = 10,
            dueMinute = 0
        )
        val dueIn25Hours = dueIn24Hours.copy(
            id = 3L,
            title = "Too Far",
            dueHour = 11,
            dueMinute = 0
        )
        val pastDue = dueIn12Hours.copy(id = 4L, title = "Past", dueHour = 9, dueMinute = 59)
        val disabled = dueIn12Hours.copy(id = 5L, title = "Off", isEnabled = false)

        val upcoming = RecurringEngine.findBillsDueWithinOneDay(
            bills = listOf(dueIn12Hours, dueIn24Hours, dueIn25Hours, pastDue, disabled),
            now = now
        )
        assertEquals(listOf(1L, 2L), upcoming.map { it.id })
    }

    @Test
    fun cutoffCalculator_handlesCustomMonthlySemiMonthlyWeeklyAndFebruaryClamping() {
        val monthly19 = CutoffScheduleConfig(mode = CutoffScheduleConfig.MODE_MONTHLY, day1 = 19)
        val midMay = CutoffCalculator.resolveCutoffWindow(LocalDate.of(2026, 5, 15), monthly19)
        assertEquals(LocalDate.of(2026, 4, 20), midMay.startDate)
        assertEquals(LocalDate.of(2026, 5, 19), midMay.endDate)

        val lateMay = CutoffCalculator.resolveCutoffWindow(LocalDate.of(2026, 5, 20), monthly19)
        assertEquals(LocalDate.of(2026, 5, 20), lateMay.startDate)
        assertEquals(LocalDate.of(2026, 6, 19), lateMay.endDate)

        val monthlyEnd = CutoffScheduleConfig(mode = CutoffScheduleConfig.MODE_MONTHLY, day1 = 31)
        val febNonLeap = CutoffCalculator.resolveCutoffWindow(LocalDate.of(2026, 2, 15), monthlyEnd)
        assertEquals(LocalDate.of(2026, 2, 1), febNonLeap.startDate)
        assertEquals(LocalDate.of(2026, 2, 28), febNonLeap.endDate)

        val customSemi = CutoffScheduleConfig(mode = CutoffScheduleConfig.MODE_SEMI_MONTHLY, day1 = 5, day2 = 20)
        val semi1 = CutoffCalculator.resolveCutoffWindow(LocalDate.of(2026, 5, 3), customSemi)
        assertEquals(LocalDate.of(2026, 4, 21), semi1.startDate)
        assertEquals(LocalDate.of(2026, 5, 5), semi1.endDate)
        assertTrue(semi1.isFirstCutoff)

        val semi2 = CutoffCalculator.resolveCutoffWindow(LocalDate.of(2026, 5, 10), customSemi)
        assertEquals(LocalDate.of(2026, 5, 6), semi2.startDate)
        assertEquals(LocalDate.of(2026, 5, 20), semi2.endDate)
        assertFalse(semi2.isFirstCutoff)

        val weeklyMon = CutoffScheduleConfig(mode = CutoffScheduleConfig.MODE_WEEKLY, weeklyDayOfWeek = 1)
        val wedWin = CutoffCalculator.resolveCutoffWindow(LocalDate.of(2026, 5, 20), weeklyMon) // Wed -> ends Mon May 25
        assertEquals(LocalDate.of(2026, 5, 19), wedWin.startDate)
        assertEquals(LocalDate.of(2026, 5, 25), wedWin.endDate)

        val shifts = listOf(
            ShiftEntity(id = 1L, dateEpochDay = LocalDate.of(2026, 5, 4).toEpochDay(), timeInMillis = 0L, timeOutMillis = 0L, durationMinutes = 240, lockedHourlyRate = 100.0, totalEarned = 400.0),
            ShiftEntity(id = 2L, dateEpochDay = LocalDate.of(2026, 5, 12).toEpochDay(), timeInMillis = 0L, timeOutMillis = 0L, durationMinutes = 240, lockedHourlyRate = 100.0, totalEarned = 450.0)
        )
        val reached = CutoffCalculator.resolveReachedCutoffWindows(
            shifts = shifts,
            today = LocalDate.of(2026, 5, 10),
            config = customSemi
        )
        assertEquals(1, reached.size)
        assertEquals(400.0, reached.first().totalEarnings, 0.001)
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
