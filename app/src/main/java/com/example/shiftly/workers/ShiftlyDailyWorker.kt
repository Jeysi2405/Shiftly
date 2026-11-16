package com.example.shiftly.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.notifications.ShiftlyNotificationHelper
import com.example.shiftly.utils.RecurringEngine
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class ShiftlyDailyWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = ShiftlyRepository(applicationContext)
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()

        // 1. Auto-post any due recurring income/expense entries
        runCatching {
            repository.processDueRecurringEntries(today)
        }

        // 2. Evaluate Payday & Shift reminders
        val settings = repository.settingsSnapshot.first()
        if (!settings.notificationsEnabled) return Result.success()
        if (!ShiftlyNotificationHelper.canPostNotifications(applicationContext)) {
            return Result.success()
        }

        val (lastPaydayDay, lastShiftDay) = repository.getLastNotifiedDays()

        if (settings.paydayReminderEnabled &&
            lastPaydayDay < todayEpoch &&
            RecurringEngine.isPaydayToday(settings.payCycleSchedule, today)
        ) {
            if (ShiftlyNotificationHelper.showPaydayReminder(applicationContext, settings.payCycleSchedule)) {
                repository.markPaydayNotified(todayEpoch)
            }
        }

        val shifts = repository.allShifts.first()
        val timer = repository.activeTimerState.first()
        val hasLoggedToday = shifts.any { it.dateEpochDay == todayEpoch }
        val hour = LocalTime.now().hour

        if (lastShiftDay < todayEpoch &&
            RecurringEngine.shouldSendShiftReminder(
                settings = settings,
                hasLoggedShiftToday = hasLoggedToday,
                isTimerRunning = timer.isRunning,
                hourOfDay = hour
            )
        ) {
            if (ShiftlyNotificationHelper.showShiftReminder(applicationContext)) {
                repository.markShiftNotified(todayEpoch)
            }
        }

        return Result.success()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "shiftly_daily_maintenance"
        private const val IMMEDIATE_WORK_NAME = "shiftly_immediate_maintenance"

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
    }
}
