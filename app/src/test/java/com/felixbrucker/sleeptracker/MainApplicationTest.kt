package com.felixbrucker.sleeptracker

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class MainApplicationTest {

    @Test
    fun mainApplication_onCreate_instantiatesSuccessfully() {
        val app = MainApplication()
        app.onCreate()

        assertNotNull(app)
    }
}
