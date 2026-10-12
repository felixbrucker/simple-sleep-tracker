package com.felixbrucker.sleeptracker.receiver

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.test.core.app.ApplicationProvider
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesRepository
import com.felixbrucker.sleeptracker.data.preferences.sleepDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class SleepActionReceiverTest {

    private lateinit var appContext: Context
    private lateinit var preferencesRepo: SleepPreferencesRepository

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        preferencesRepo = SleepPreferencesRepository(appContext.sleepDataStore)
        runBlocking {
            preferencesRepo.setTrackingActive(active = false, startTimeMillis = 0L)
        }
    }

    @After
    fun tearDown() {
        runBlocking {
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
    fun onReceive_withStartTrackingAction_startsTracking() {
        val receiver = SleepActionReceiver()
        val intent = Intent(SleepActionReceiver.ACTION_START_TRACKING)
        appContext.registerReceiver(receiver, IntentFilter(SleepActionReceiver.ACTION_START_TRACKING), Context.RECEIVER_EXPORTED)

        appContext.sendBroadcast(intent)
        waitForAsyncWork()

        val isTracking = runBlocking { preferencesRepo.preferencesFlow.first().isTrackingActive }
        assertTrue(isTracking)
    }

    @Test
    fun onReceive_withStopTrackingAction_stopsTracking() {
        runBlocking {
            preferencesRepo.setTrackingActive(active = true, startTimeMillis = System.currentTimeMillis() - 3600000L)
        }
        val receiver = SleepActionReceiver()
        val intent = Intent(SleepActionReceiver.ACTION_STOP_TRACKING)
        appContext.registerReceiver(receiver, IntentFilter(SleepActionReceiver.ACTION_STOP_TRACKING), Context.RECEIVER_EXPORTED)

        appContext.sendBroadcast(intent)
        waitForAsyncWork()

        val isTracking = runBlocking { preferencesRepo.preferencesFlow.first().isTrackingActive }
        assertFalse(isTracking)
    }

    @Test
    fun onReceive_withDismissAction_doesNotThrow() {
        val receiver = SleepActionReceiver()
        val intent = Intent(SleepActionReceiver.ACTION_DISMISS)
        appContext.registerReceiver(receiver, IntentFilter(SleepActionReceiver.ACTION_DISMISS), Context.RECEIVER_EXPORTED)

        appContext.sendBroadcast(intent)
    }

    @Test
    fun onReceive_withNullIntent_doesNotThrow() {
        val receiver = SleepActionReceiver()

        receiver.onReceive(appContext, null)
    }
}
