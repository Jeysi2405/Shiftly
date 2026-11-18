package com.example.shiftly.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.shiftly.MainActivity
import com.example.shiftly.R
import com.example.shiftly.data.local.RecurringBillEntity
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ShiftlyNotificationHelper {
    const val CHANNEL_RECURRING_BILLS = "recurring_bill_reminders"
    const val CHANNEL_SHIFT = "shift_reminders"
    const val CHANNEL_SAVINGS_DEPOSIT = "savings_deposit_reminders"

    private const val NOTIFICATION_ID_BILLS = 1001
    private const val NOTIFICATION_ID_SHIFT = 1002
    private const val NOTIFICATION_ID_SAVINGS = 1003

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val billsChannel = NotificationChannel(
            CHANNEL_RECURRING_BILLS,
            "Recurring Bill Due Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Alerts less than 1 day before a recurring bill due date."
        }
        val shiftChannel = NotificationChannel(
            CHANNEL_SHIFT,
            "Log Shift Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily reminder to log your shift hours."
        }
        val savingsChannel = NotificationChannel(
            CHANNEL_SAVINGS_DEPOSIT,
            "Savings & Deposit Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminders to log savings and add a deposit."
        }
        manager.createNotificationChannel(billsChannel)
        manager.createNotificationChannel(shiftChannel)
        manager.createNotificationChannel(savingsChannel)
    }

    fun canPostNotifications(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun launchAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showRecurringBillReminder(
        context: Context,
        dueBills: List<RecurringEntrySetting>,
        currencySymbol: String
    ): Boolean {
        if (dueBills.isEmpty() || !canPostNotifications(context)) return false
        ensureChannels(context)
        val title = if (dueBills.size == 1) {
            "Bill Due Soon: ${dueBills.first().title}"
        } else {
            "${dueBills.size} Recurring Bills Due Soon"
        }
        val body = if (dueBills.size == 1) {
            val bill = dueBills.first()
            "${ShiftlyFormatters.formatCurrency(currencySymbol, bill.amount)} is due in less than 1 day."
        } else {
            val total = dueBills.sumOf { it.amount }
            val names = dueBills.take(2).joinToString(", ") { it.title }
            val extra = if (dueBills.size > 2) " +${dueBills.size - 2} more" else ""
            "$names$extra (${ShiftlyFormatters.formatCurrency(currencySymbol, total)}) due in less than 1 day."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_RECURRING_BILLS)
            .setSmallIcon(R.drawable.ic_stat_shiftly)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(launchAppPendingIntent(context))
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BILLS, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    @JvmName("showRecurringBillEntityReminder")
    fun showRecurringBillReminder(
        context: Context,
        dueBills: List<RecurringBillEntity>,
        currencySymbol: String
    ): Boolean {
        if (dueBills.isEmpty() || !canPostNotifications(context)) return false
        ensureChannels(context)
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        val title = if (dueBills.size == 1) {
            "Bill Due Soon: ${dueBills.first().title}"
        } else {
            "${dueBills.size} Recurring Bills Due Soon"
        }
        val body = if (dueBills.size == 1) {
            val bill = dueBills.first()
            val timeStr = LocalTime.of(bill.dueHour.coerceIn(0, 23), bill.dueMinute.coerceIn(0, 59))
                .format(timeFormatter)
            "${ShiftlyFormatters.formatCurrency(currencySymbol, bill.amount)} is due soon ($timeStr)."
        } else {
            val total = dueBills.sumOf { it.amount }
            val names = dueBills.take(2).joinToString(", ") { it.title }
            val extra = if (dueBills.size > 2) " +${dueBills.size - 2} more" else ""
            "$names$extra (${ShiftlyFormatters.formatCurrency(currencySymbol, total)}) due within 24 hours."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_RECURRING_BILLS)
            .setSmallIcon(R.drawable.ic_stat_shiftly)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(launchAppPendingIntent(context))
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BILLS, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun showShiftReminder(context: Context): Boolean {
        if (!canPostNotifications(context)) return false
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_SHIFT)
            .setSmallIcon(R.drawable.ic_stat_shiftly)
            .setContentTitle("Log Shift Reminder")
            .setContentText("Did you work today? Tap to log your shift hours in Shiftly.")
            .setAutoCancel(true)
            .setContentIntent(launchAppPendingIntent(context))
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SHIFT, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun showSavingsDepositReminder(
        context: Context,
        activeGoalTitle: String,
        isPayday: Boolean
    ): Boolean {
        if (!canPostNotifications(context)) return false
        ensureChannels(context)
        val goalLabel = if (activeGoalTitle.isNotBlank()) " for $activeGoalTitle" else ""
        val title = if (isPayday) {
            "Payday! Add a Savings Deposit"
        } else {
            "Log Saving Reminder"
        }
        val body = if (isPayday) {
            "Your payout cutoff is here — tap to add a deposit$goalLabel."
        } else {
            "Don't forget to add a deposit$goalLabel today in Shiftly."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_SAVINGS_DEPOSIT)
            .setSmallIcon(R.drawable.ic_stat_shiftly)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(launchAppPendingIntent(context))
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SAVINGS, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
