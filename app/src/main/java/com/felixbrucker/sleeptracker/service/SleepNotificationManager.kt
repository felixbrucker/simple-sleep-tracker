package com.felixbrucker.sleeptracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.felixbrucker.sleeptracker.MainActivity
import com.felixbrucker.sleeptracker.receiver.SleepActionReceiver
import com.felixbrucker.sleeptracker.util.toFormattedTime

class SleepNotificationManager(private val context: Context) {

    companion object {
        const val NOTIFICATION_ID_SLEEP = 1001
        const val CHANNEL_ID_REMINDER = "sleep_reminder_channel"
        const val CHANNEL_ID_TRACKING = "sleep_tracking_channel"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                "Bedtime Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications prompting to start sleep tracking"
            }

            val trackingChannel = NotificationChannel(
                CHANNEL_ID_TRACKING,
                "Sleep Tracking Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification while sleep tracking is active"
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(trackingChannel)
        }
    }

    fun showSleepReminderNotification() {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startTrackingIntent = Intent(context, SleepActionReceiver::class.java).apply {
            action = SleepActionReceiver.ACTION_START_TRACKING
        }
        val startTrackingPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            startTrackingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, SleepActionReceiver::class.java).apply {
            action = SleepActionReceiver.ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Time for Bed")
            .setContentText("Ready for sleep? Start tracking to monitor your rest.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("It's your scheduled bedtime. Tap below to begin tracking your sleep duration.")
            )
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.ic_media_play,
                "Start Sleep Tracking",
                startTrackingPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID_SLEEP, notification)
    }

    fun showTrackingActiveNotification(startTimeMillis: Long) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopTrackingIntent = Intent(context, SleepActionReceiver::class.java).apply {
            action = SleepActionReceiver.ACTION_STOP_TRACKING
        }
        val stopTrackingPendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            stopTrackingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedStartTime = startTimeMillis.toFormattedTime()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TRACKING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Sleep Tracking Active")
            .setContentText("Started at $formattedStartTime")
            .setWhen(startTimeMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Sleep Tracking",
                stopTrackingPendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID_SLEEP, notification)
    }

    fun dismissSleepNotification() {
        notificationManager.cancel(NOTIFICATION_ID_SLEEP)
    }
}
