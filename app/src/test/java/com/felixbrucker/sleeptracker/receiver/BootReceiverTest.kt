package com.felixbrucker.sleeptracker.receiver

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesRepository
import com.felixbrucker.sleeptracker.data.preferences.sleepDataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var preferencesRepo: SleepPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferencesRepo = SleepPreferencesRepository(context.sleepDataStore)
        runBlocking {
            preferencesRepo.setTrackingActive(active = false, startTimeMillis = 0L)
            preferencesRepo.setReminderConfig(enabled = true, hour = 22, minute = 30)
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            preferencesRepo.setTrackingActive(active = false, startTimeMillis = 0L)
            preferencesRepo.setReminderConfig(enabled = true, hour = 22, minute = 30)
        }
    }

    private fun waitForAsyncWork() {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 1000) {
            Thread.sleep(20)
        }
    }

    @Test
    fun onReceive_withTimezoneChangedAndReminderEnabled_schedulesBedtimeReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runBlocking {
            preferencesRepo.setReminderConfig(enabled = true, hour = 22, minute = 30)
        }
        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_TIMEZONE_CHANGED)

        receiver.onReceive(context, intent)
        waitForAsyncWork()

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }

    @Test
    fun onReceive_withTimezoneChangedAndReminderDisabled_doesNotScheduleBedtimeReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runBlocking {
            preferencesRepo.setReminderConfig(enabled = false, hour = 22, minute = 30)
        }
        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_TIMEZONE_CHANGED)

        receiver.onReceive(context, intent)
        waitForAsyncWork()

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertTrue(scheduledAlarms.isEmpty())
    }

    @Test
    fun onReceive_withTimeChangedAndReminderEnabled_schedulesBedtimeReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runBlocking {
            preferencesRepo.setReminderConfig(enabled = true, hour = 21, minute = 15)
        }
        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_TIME_CHANGED)

        receiver.onReceive(context, intent)
        waitForAsyncWork()

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }

    @Test
    fun onReceive_withBootCompletedAndReminderEnabled_schedulesBedtimeReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runBlocking {
            preferencesRepo.setReminderConfig(enabled = true, hour = 23, minute = 0)
        }
        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)
        waitForAsyncWork()

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }

    @Test
    fun onReceive_withPackageReplacedAndReminderEnabled_schedulesBedtimeReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runBlocking {
            preferencesRepo.setReminderConfig(enabled = true, hour = 20, minute = 0)
        }
        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED)

        receiver.onReceive(context, intent)
        waitForAsyncWork()

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }

    @Test
    fun onReceive_withTrackingActive_restoresTrackingNotificationAndReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runBlocking {
            preferencesRepo.setReminderConfig(enabled = true, hour = 22, minute = 30)
            preferencesRepo.setTrackingActive(active = true, startTimeMillis = System.currentTimeMillis())
        }
        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)
        waitForAsyncWork()

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }

    @Test
    fun onReceive_withUnhandledAction_doesNotScheduleAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runBlocking {
            preferencesRepo.setReminderConfig(enabled = true, hour = 22, minute = 30)
        }
        val receiver = BootReceiver()
        val intent = Intent("com.example.UNHANDLED_ACTION")

        receiver.onReceive(context, intent)

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertTrue(scheduledAlarms.isEmpty())
    }

    @Test
    fun onReceive_withNullIntent_doesNotScheduleAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runBlocking {
            preferencesRepo.setReminderConfig(enabled = true, hour = 22, minute = 30)
        }
        val receiver = BootReceiver()

        receiver.onReceive(context, null)

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertTrue(scheduledAlarms.isEmpty())
    }
}
