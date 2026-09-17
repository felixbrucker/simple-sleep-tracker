package com.felixbrucker.sleeptracker.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepPreferencesTest {

    @Test
    fun defaultValues_areInitializedCorrectly() {
        val prefs = SleepPreferences()

        assertFalse(prefs.isTrackingActive)
        assertEquals(0L, prefs.trackingStartTimeMillis)
        assertTrue(prefs.reminderEnabled)
        assertEquals(22, prefs.reminderHour)
        assertEquals(30, prefs.reminderMinute)
        assertTrue(prefs.autoSyncHealthConnect)
        assertEquals(15, prefs.fallAsleepDurationMinutes)
    }

    @Test
    fun copy_withCustomFallAsleepMinutes_updatesCorrectly() {
        val original = SleepPreferences()

        val updated = original.copy(fallAsleepDurationMinutes = 30)

        assertEquals(30, updated.fallAsleepDurationMinutes)
        assertEquals(original.reminderHour, updated.reminderHour)
    }

    @Test
    fun copy_withTrackingActive_updatesCorrectly() {
        val original = SleepPreferences()

        val updated = original.copy(isTrackingActive = true, trackingStartTimeMillis = 5000L)

        assertTrue(updated.isTrackingActive)
        assertEquals(5000L, updated.trackingStartTimeMillis)
    }
}
