package com.felixbrucker.sleeptracker.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class HealthConnectModuleTest {

    @Test
    fun provideHealthConnectClient_returnsInstanceOrNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        HealthConnectModule.provideHealthConnectClient(context)
    }
}
