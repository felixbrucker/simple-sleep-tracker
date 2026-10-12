package com.felixbrucker.sleeptracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesRepository
import com.felixbrucker.sleeptracker.service.AlarmScheduler
import com.felixbrucker.sleeptracker.service.SleepNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SleepReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SleepReminderReceiver"
        const val ACTION_TRIGGER_REMINDER = "com.felixbrucker.sleeptracker.ACTION_TRIGGER_REMINDER"
    }

    @Inject
    lateinit var preferencesRepo: SleepPreferencesRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var notificationManager: SleepNotificationManager

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Sleep reminder alarm received")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = preferencesRepo.preferencesFlow.first()

                if (prefs.reminderEnabled && !prefs.isTrackingActive) {
                    notificationManager.showSleepReminderNotification()
                }

                // Reschedule for next day if enabled
                if (prefs.reminderEnabled) {
                        alarmScheduler.scheduleBedtimeReminder(prefs.reminderHour, prefs.reminderMinute)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling sleep reminder alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
