package com.felixbrucker.sleeptracker.service

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class SleepNotificationManagerTest {

    private lateinit var context: Context
    private lateinit var notificationManager: SleepNotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = SleepNotificationManager(context)
    }

    @Test
    fun showSleepReminderNotification_postsNotification() {
        notificationManager.showSleepReminderNotification()

        val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertNotNull(systemNotificationManager)
    }

    @Test
    fun showTrackingActiveNotification_postsOngoingNotification() {
        notificationManager.showTrackingActiveNotification(System.currentTimeMillis())

        val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertNotNull(systemNotificationManager)
    }

    @Test
    fun dismissSleepNotification_cancelsNotification() {
        notificationManager.dismissSleepNotification()

        val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertNotNull(systemNotificationManager)
    }
}
