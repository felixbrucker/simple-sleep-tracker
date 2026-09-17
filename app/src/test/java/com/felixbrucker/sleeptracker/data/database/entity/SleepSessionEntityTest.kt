package com.felixbrucker.sleeptracker.data.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepSessionEntityTest {

    @Test
    fun defaultValues_whenInstantiated_areCorrect() {
        val startMillis = 1000L
        val endMillis = 5000L
        val durationMillis = 4000L

        val session = SleepSessionEntity(
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            durationMillis = durationMillis
        )

        assertEquals(0L, session.id)
        assertFalse(session.syncedToHealthConnect)
        assertEquals("", session.notes)
    }

    @Test
    fun copySession_withUpdatedSyncStatus_createsExpectedEntity() {
        val session = SleepSessionEntity(
            id = 10L,
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            durationMillis = 4000L,
            syncedToHealthConnect = false,
            notes = "test notes"
        )

        val updated = session.copy(syncedToHealthConnect = true)

        assertTrue(updated.syncedToHealthConnect)
        assertEquals(10L, updated.id)
        assertEquals("test notes", updated.notes)
    }
}
