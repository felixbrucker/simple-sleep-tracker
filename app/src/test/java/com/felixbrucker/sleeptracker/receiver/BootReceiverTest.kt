package com.felixbrucker.sleeptracker.receiver

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferences
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class BootReceiverTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun onReceive_withTimezoneChangedAndReminderEnabled_schedulesBedtimeReminder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fakePrefsRepo = FakeSleepPreferencesDataSource(
            SleepPreferences(reminderEnabled = true, reminderHour = 22, reminderMinute = 30)
        )
        val receiver = BootReceiver(fakePrefsRepo, testDispatcher)
        val intent = Intent(Intent.ACTION_TIMEZONE_CHANGED)

        receiver.onReceive(context, intent)

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }

    @Test
    fun onReceive_withTimezoneChangedAndReminderDisabled_doesNotScheduleBedtimeReminder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fakePrefsRepo = FakeSleepPreferencesDataSource(
            SleepPreferences(reminderEnabled = false, reminderHour = 22, reminderMinute = 30)
        )
        val receiver = BootReceiver(fakePrefsRepo, testDispatcher)
        val intent = Intent(Intent.ACTION_TIMEZONE_CHANGED)

        receiver.onReceive(context, intent)

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertTrue(scheduledAlarms.isEmpty())
    }

    @Test
    fun onReceive_withTimeChangedAndReminderEnabled_schedulesBedtimeReminder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fakePrefsRepo = FakeSleepPreferencesDataSource(
            SleepPreferences(reminderEnabled = true, reminderHour = 21, reminderMinute = 15)
        )
        val receiver = BootReceiver(fakePrefsRepo, testDispatcher)
        val intent = Intent(Intent.ACTION_TIME_CHANGED)

        receiver.onReceive(context, intent)

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }

    @Test
    fun onReceive_withBootCompletedAndReminderEnabled_schedulesBedtimeReminder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fakePrefsRepo = FakeSleepPreferencesDataSource(
            SleepPreferences(reminderEnabled = true, reminderHour = 23, reminderMinute = 0)
        )
        val receiver = BootReceiver(fakePrefsRepo, testDispatcher)
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }

    @Test
    fun onReceive_withPackageReplacedAndReminderEnabled_schedulesBedtimeReminder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fakePrefsRepo = FakeSleepPreferencesDataSource(
            SleepPreferences(reminderEnabled = true, reminderHour = 20, reminderMinute = 0)
        )
        val receiver = BootReceiver(fakePrefsRepo, testDispatcher)
        val intent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED)

        receiver.onReceive(context, intent)

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }

    @Test
    fun onReceive_withTrackingActive_restoresTrackingNotificationAndReminder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fakePrefsRepo = FakeSleepPreferencesDataSource(
            SleepPreferences(
                isTrackingActive = true,
                trackingStartTimeMillis = System.currentTimeMillis(),
                reminderEnabled = true,
                reminderHour = 22,
                reminderMinute = 30
            )
        )
        val receiver = BootReceiver(fakePrefsRepo, testDispatcher)
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
    }

    @Test
    fun onReceive_withUnhandledAction_doesNotScheduleAlarm() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fakePrefsRepo = FakeSleepPreferencesDataSource(
            SleepPreferences(reminderEnabled = true, reminderHour = 22, reminderMinute = 30)
        )
        val receiver = BootReceiver(fakePrefsRepo, testDispatcher)
        val intent = Intent("com.example.UNHANDLED_ACTION")

        receiver.onReceive(context, intent)

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertTrue(scheduledAlarms.isEmpty())
    }

    @Test
    fun onReceive_withNullIntent_doesNotScheduleAlarm() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fakePrefsRepo = FakeSleepPreferencesDataSource(
            SleepPreferences(reminderEnabled = true, reminderHour = 22, reminderMinute = 30)
        )
        val receiver = BootReceiver(fakePrefsRepo, testDispatcher)

        receiver.onReceive(context, null)

        val scheduledAlarms = shadowOf(alarmManager).scheduledAlarms
        assertTrue(scheduledAlarms.isEmpty())
    }

    private class FakeSleepPreferencesDataSource(
        initialPreferences: SleepPreferences = SleepPreferences()
    ) : SleepPreferencesDataSource {
        private val _flow = MutableStateFlow(initialPreferences)
        override val preferencesFlow: Flow<SleepPreferences> = _flow

        override suspend fun setTrackingActive(active: Boolean, startTimeMillis: Long) {
            _flow.value = _flow.value.copy(
                isTrackingActive = active,
                trackingStartTimeMillis = if (active) startTimeMillis else 0L
            )
        }

        override suspend fun setReminderConfig(enabled: Boolean, hour: Int, minute: Int) {
            _flow.value = _flow.value.copy(
                reminderEnabled = enabled,
                reminderHour = hour,
                reminderMinute = minute
            )
        }

        override suspend fun setAutoSyncHealthConnect(enabled: Boolean) {
            _flow.value = _flow.value.copy(autoSyncHealthConnect = enabled)
        }

        override suspend fun setFallAsleepDurationMinutes(minutes: Int) {
            _flow.value = _flow.value.copy(fallAsleepDurationMinutes = minutes)
        }
    }
}
