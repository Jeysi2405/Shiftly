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

object ShiftlyNotificationHelper {
    const val CHANNEL_PAYDAY = "payday_reminders"
    const val CHANNEL_SHIFT = "shift_reminders"
    private const val NOTIFICATION_ID_PAYDAY = 1001
    private const val NOTIFICATION_ID_SHIFT = 1002

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val paydayChannel = NotificationChannel(
            CHANNEL_PAYDAY,
            "Payday Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Cut-off and payday alerts based on your pay cycle."
        }
        val shiftChannel = NotificationChannel(
            CHANNEL_SHIFT,
            "Shift Check-In Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily reminders to log or clock in for your shift."
        }
        manager.createNotificationChannel(paydayChannel)
        manager.createNotificationChannel(shiftChannel)
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

    fun showPaydayReminder(context: Context, schedule: String): Boolean {
        if (!canPostNotifications(context)) return false
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_PAYDAY)
            .setSmallIcon(R.drawable.ic_stat_shiftly)
            .setContentTitle("Payday Cutoff ($schedule)")
            .setContentText("Review your logged shifts and confirm your payout total in Shiftly.")
            .setAutoCancel(true)
            .setContentIntent(launchAppPendingIntent(context))
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PAYDAY, notification)
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
            .setContentTitle("Did you work a shift today?")
            .setContentText("Tap to clock in or log today's duty hours in Shiftly.")
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
}
