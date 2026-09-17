package com.felixbrucker.sleeptracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.felixbrucker.sleeptracker.data.database.AppDatabase
import com.felixbrucker.sleeptracker.data.healthconnect.HealthConnectManager
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesRepository
import com.felixbrucker.sleeptracker.data.preferences.sleepDataStore
import com.felixbrucker.sleeptracker.data.repository.SleepRepository
import com.felixbrucker.sleeptracker.service.AlarmScheduler
import com.felixbrucker.sleeptracker.service.SleepNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SleepActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SleepActionReceiver"
        const val ACTION_START_TRACKING = "com.felixbrucker.sleeptracker.ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.felixbrucker.sleeptracker.ACTION_STOP_TRACKING"
        const val ACTION_DISMISS = "com.felixbrucker.sleeptracker.ACTION_DISMISS"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "Received action: $action")

        val notificationManager = SleepNotificationManager(context)
        val database = AppDatabase.getInstance(context)
        val preferencesRepo = SleepPreferencesRepository(context.sleepDataStore)
        val healthConnectManager = HealthConnectManager(context)
        val repository = SleepRepository(
            sleepSessionDao = database.sleepSessionDao(),
            preferencesRepository = preferencesRepo,
            healthConnectManager = healthConnectManager
        )

        when (action) {
            ACTION_START_TRACKING -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val startTime = System.currentTimeMillis()
                        repository.startTracking(startTime)
                        // Update notification to active ongoing tracking state
                        notificationManager.showTrackingActiveNotification(startTime)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start tracking", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_STOP_TRACKING -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repository.stopTracking()
                        // Stop sleep tracking via notification and dismiss notification
                        notificationManager.dismissSleepNotification()

                        // Reschedule next bedtime reminder
                        val prefs = preferencesRepo.preferencesFlow.first()
                        if (prefs.reminderEnabled) {
                            val scheduler = AlarmScheduler(context)
                            scheduler.scheduleBedtimeReminder(prefs.reminderHour, prefs.reminderMinute)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to stop tracking", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_DISMISS -> {
                notificationManager.dismissSleepNotification()
            }
        }
    }
}
