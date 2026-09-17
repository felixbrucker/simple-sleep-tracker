package com.felixbrucker.sleeptracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesRepository
import com.felixbrucker.sleeptracker.data.preferences.sleepDataStore
import com.felixbrucker.sleeptracker.service.AlarmScheduler
import com.felixbrucker.sleeptracker.service.SleepNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Device booted or app updated, restoring state")
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val preferencesRepo = SleepPreferencesRepository(context.sleepDataStore)
                    val prefs = preferencesRepo.preferencesFlow.first()

                    if (prefs.reminderEnabled) {
                        val scheduler = AlarmScheduler(context)
                        scheduler.scheduleBedtimeReminder(prefs.reminderHour, prefs.reminderMinute)
                    }

                    // If sleep tracking was actively in progress before reboot, restore tracking notification
                    if (prefs.isTrackingActive && prefs.trackingStartTimeMillis > 0L) {
                        val notificationManager = SleepNotificationManager(context)
                        notificationManager.showTrackingActiveNotification(prefs.trackingStartTimeMillis)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring sleep tracker on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
