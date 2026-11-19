package com.example.shiftly.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.shiftly.data.local.InAppNotificationType
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.notifications.ShiftlyNotificationHelper
import com.example.shiftly.utils.RecurringEngine
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class ShiftlyDailyWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = ShiftlyRepository.getInstance(applicationContext)
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val todayEpoch = today.toEpochDay()
        val zone = ZoneId.systemDefault()

        // 1. Auto-post any due recurring bills & legacy entries
        runCatching {
            repository.processDueRecurringEntries(today)
        }

        // 2. Credit any reached Duties cutoff windows into Allowance
        runCatching {
            repository.processDueCutoffAllowances(today)
        }

        // 3. Schedule exact-delay worker for the earliest upcoming recurring bill due timestamp
        runCatching {
            val allBills = repository.allRecurringBills.first()
            val earliestDue = RecurringEngine.earliestNextDueDateTime(allBills)
            scheduleNextRecurringCheck(applicationContext, earliestDue)
        }

        // 4. Evaluate the 3 allowed reminders + in-app inbox sync
        val settings = repository.settingsSnapshot.first()
        if (!settings.notificationsEnabled) return Result.success()

        runCatching {
            repository.syncInAppNotifications(now)
        }

        if (!ShiftlyNotificationHelper.canPostNotifications(applicationContext)) {
            return Result.success()
        }

        val lastDays = repository.getNotificationLastDays()

        // (a) Recurring bills due within < 24 hours
        if (settings.billReminderEnabled && lastDays.lastBillDay < todayEpoch) {
            val allBills = repository.allRecurringBills.first()
            val dueSoonBills = RecurringEngine.findBillsDueWithinOneDay(
                bills = allBills,
                now = now
            )
            if (dueSoonBills.isNotEmpty()) {
                val currency = repository.currencySymbol.first()
                if (ShiftlyNotificationHelper.showRecurringBillReminder(
                        context = applicationContext,
                        dueBills = dueSoonBills,
                        currencySymbol = currency
                    )
                ) {
                    repository.markBillNotified(todayEpoch)
                    val copy = ShiftlyNotificationHelper.formatRecurringBillReminderCopy(dueSoonBills, currency)
                    val billKey = dueSoonBills.map { "${it.id}_${it.dueEpochDay}" }.sorted().joinToString("_")
                    repository.recordInAppNotification(
                        eventKey = "bill_due_${todayEpoch}_$billKey",
                        type = InAppNotificationType.BILL_DUE,
                        title = copy.title,
                        message = copy.message
                    )
                }
            }
        }

        // (b) Log shift reminder
        val shifts = repository.allShifts.first()
        val hasLoggedToday = shifts.any { it.dateEpochDay == todayEpoch }
        val hour = LocalTime.now().hour

        if (lastDays.lastShiftDay < todayEpoch &&
            RecurringEngine.shouldSendShiftReminder(
                settings = settings,
                hasLoggedShiftToday = hasLoggedToday,
                isTimerRunning = false,
                hourOfDay = hour
            )
        ) {
            if (ShiftlyNotificationHelper.showShiftReminder(applicationContext)) {
                repository.markShiftNotified(todayEpoch)
                val copy = ShiftlyNotificationHelper.formatShiftReminderCopy()
                repository.recordInAppNotification(
                    eventKey = "shift_reminder_$todayEpoch",
                    type = InAppNotificationType.SHIFT_REMINDER,
                    title = copy.title,
                    message = copy.message
                )
            }
        }

        // (c) Log saving & add deposit reminder (payday cutoff or daily reminder)
        val goals = repository.allSavingsGoals.first()
        val activeGoal = goals.firstOrNull { it.goal.isActive } ?: goals.firstOrNull()
        val hasDepositedToday = goals.any { g ->
            g.deposits.any { dep ->
                Instant.ofEpochMilli(dep.timestampMillis).atZone(zone).toLocalDate() == today
            }
        }
        val isPayday = RecurringEngine.isPaydayToday(settings.cutoffScheduleConfig(), today)

        if (lastDays.lastSavingsDay < todayEpoch &&
            RecurringEngine.shouldSendSavingsDepositReminder(
                settings = settings,
                isPaydayToday = isPayday,
                hasDepositedToday = hasDepositedToday,
                hourOfDay = hour
            )
        ) {
            val activeGoalTitle = activeGoal?.goal?.title.orEmpty()
            if (ShiftlyNotificationHelper.showSavingsDepositReminder(
                    context = applicationContext,
                    activeGoalTitle = activeGoalTitle,
                    isPayday = isPayday
                )
            ) {
                repository.markSavingsNotified(todayEpoch)
                val copy = ShiftlyNotificationHelper.formatSavingsDepositReminderCopy(
                    activeGoalTitle = activeGoalTitle,
                    isPayday = isPayday
                )
                repository.recordInAppNotification(
                    eventKey = "savings_reminder_$todayEpoch",
                    type = InAppNotificationType.SAVINGS_REMINDER,
                    title = copy.title,
                    message = copy.message
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "shiftly_daily_maintenance"
        private const val IMMEDIATE_WORK_NAME = "shiftly_immediate_maintenance"
        private const val NEXT_RECURRING_WORK_NAME = "shiftly_next_recurring_execution"

        fun scheduleDailyMaintenance(context: Context) {
            val request = PeriodicWorkRequestBuilder<ShiftlyDailyWorker>(
                12L, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun triggerImmediateCheck(context: Context) {
            val request = OneTimeWorkRequestBuilder<ShiftlyDailyWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun scheduleNextRecurringCheck(context: Context, nextDue: LocalDateTime?) {
            val wm = WorkManager.getInstance(context.applicationContext)
            if (nextDue == null) {
                wm.cancelUniqueWork(NEXT_RECURRING_WORK_NAME)
                return
            }
            val delayMillis = RecurringEngine.millisUntilDue(nextDue)
            val request = OneTimeWorkRequestBuilder<ShiftlyDailyWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniqueWork(
                NEXT_RECURRING_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
