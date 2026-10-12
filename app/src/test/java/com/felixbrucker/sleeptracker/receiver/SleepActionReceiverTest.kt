package com.felixbrucker.sleeptracker.receiver

import android.content.Context
import android.content.Intent
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class SleepActionReceiverTest {

    private lateinit var context: Context
    private lateinit var preferencesRepo: SleepPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferencesRepo = SleepPreferencesRepository(context.sleepDataStore)
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
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 1000) {
            Thread.sleep(20)
        }
    }

    @Test
    fun onReceive_withStartTrackingAction_startsTracking() {
        val receiver = SleepActionReceiver()
        val intent = Intent(SleepActionReceiver.ACTION_START_TRACKING)

        receiver.onReceive(context, intent)
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

        receiver.onReceive(context, intent)
        waitForAsyncWork()

        val isTracking = runBlocking { preferencesRepo.preferencesFlow.first().isTrackingActive }
        assertFalse(isTracking)
    }

    @Test
    fun onReceive_withDismissAction_doesNotThrow() {
        val receiver = SleepActionReceiver()
        val intent = Intent(SleepActionReceiver.ACTION_DISMISS)

        receiver.onReceive(context, intent)
    }

    @Test
    fun onReceive_withNullIntent_doesNotThrow() {
        val receiver = SleepActionReceiver()

        receiver.onReceive(context, null)
    }
}
