package com.felixbrucker.sleeptracker.receiver

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.test.core.app.ApplicationProvider
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesRepository
import com.felixbrucker.sleeptracker.data.preferences.sleepDataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class SleepReminderReceiverTest {

    private lateinit var appContext: Context
    private lateinit var preferencesRepo: SleepPreferencesRepository

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        preferencesRepo = SleepPreferencesRepository(appContext.sleepDataStore)
        runBlocking {
            preferencesRepo.setReminderConfig(enabled = true, hour = 22, minute = 30)
            preferencesRepo.setTrackingActive(active = false, startTimeMillis = 0L)
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            preferencesRepo.setReminderConfig(enabled = true, hour = 22, minute = 30)
            preferencesRepo.setTrackingActive(active = false, startTimeMillis = 0L)
        }
    }

    private fun waitForAsyncWork() {
        ShadowLooper.idleMainLooper()
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 1000) {
            Thread.sleep(20)
        }
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun onReceive_whenReminderEnabledAndTrackingInactive_reschedulesNextReminder() {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val receiver = SleepReminderReceiver()
        val intent = Intent(SleepReminderReceiver.ACTION_TRIGGER_REMINDER)
        appContext.registerReceiver(receiver, IntentFilter(SleepReminderReceiver.ACTION_TRIGGER_REMINDER), Context.RECEIVER_EXPORTED)

        appContext.sendBroadcast(intent)
        waitForAsyncWork()

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }

    @Test
    fun onReceive_whenTrackingActive_reschedulesNextReminder() {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runBlocking {
            preferencesRepo.setTrackingActive(active = true, startTimeMillis = System.currentTimeMillis())
        }
        val receiver = SleepReminderReceiver()
        val intent = Intent(SleepReminderReceiver.ACTION_TRIGGER_REMINDER)
        appContext.registerReceiver(receiver, IntentFilter(SleepReminderReceiver.ACTION_TRIGGER_REMINDER), Context.RECEIVER_EXPORTED)

        appContext.sendBroadcast(intent)
        waitForAsyncWork()

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }
}
