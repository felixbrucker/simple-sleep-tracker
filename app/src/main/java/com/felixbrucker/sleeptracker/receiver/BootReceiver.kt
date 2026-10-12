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
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    @Inject
    lateinit var preferencesRepo: SleepPreferencesRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var notificationManager: SleepNotificationManager

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED
        ) {
            Log.d(TAG, "Boot/update/time/timezone broadcast received: $action")
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = preferencesRepo.preferencesFlow.first()

                    if (prefs.reminderEnabled) {
                        alarmScheduler.scheduleBedtimeReminder(prefs.reminderHour, prefs.reminderMinute)
                    }

                    // If sleep tracking was actively in progress before reboot, restore tracking notification
                    if (prefs.isTrackingActive && prefs.trackingStartTimeMillis > 0L) {
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
