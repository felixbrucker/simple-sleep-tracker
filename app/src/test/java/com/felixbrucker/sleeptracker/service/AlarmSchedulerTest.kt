package com.felixbrucker.sleeptracker.service

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class AlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmScheduler: AlarmScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmScheduler = AlarmScheduler(context)
    }

    @Test
    fun scheduleBedtimeReminder_schedulesAlarmOnAlarmManager() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmScheduler.scheduleBedtimeReminder(22, 30)

        val scheduled = shadowOf(alarmManager).scheduledAlarms
        assertEquals(1, scheduled.size)
    }

    @Test
    fun cancelBedtimeReminder_cancelsAlarmOnAlarmManager() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmScheduler.scheduleBedtimeReminder(22, 30)

        alarmScheduler.cancelBedtimeReminder()

        val scheduled = shadowOf(alarmManager).scheduledAlarms
        assertEquals(0, scheduled.size)
    }
}
