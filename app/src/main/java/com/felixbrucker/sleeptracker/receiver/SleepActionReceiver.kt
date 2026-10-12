package com.felixbrucker.sleeptracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesRepository
import com.felixbrucker.sleeptracker.data.repository.SleepRepository
import com.felixbrucker.sleeptracker.service.AlarmScheduler
import com.felixbrucker.sleeptracker.service.SleepNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SleepActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SleepActionReceiver"
        const val ACTION_START_TRACKING = "com.felixbrucker.sleeptracker.ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.felixbrucker.sleeptracker.ACTION_STOP_TRACKING"
        const val ACTION_DISMISS = "com.felixbrucker.sleeptracker.ACTION_DISMISS"
    }

    @Inject
    lateinit var repository: SleepRepository

    @Inject
    lateinit var preferencesRepo: SleepPreferencesRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var notificationManager: SleepNotificationManager

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "Received action: $action")

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
                            alarmScheduler.scheduleBedtimeReminder(prefs.reminderHour, prefs.reminderMinute)
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
