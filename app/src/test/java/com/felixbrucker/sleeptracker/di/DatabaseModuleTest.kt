package com.felixbrucker.sleeptracker.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class DatabaseModuleTest {

    @Test
    fun provideAppDatabaseAndDao_returnsNonNullInstances() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val db = DatabaseModule.provideAppDatabase(context)
        val dao = DatabaseModule.provideSleepSessionDao(db)

        assertNotNull(db)
        assertNotNull(dao)
        db.close()
    }
}
