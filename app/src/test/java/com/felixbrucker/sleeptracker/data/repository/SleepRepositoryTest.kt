package com.felixbrucker.sleeptracker.data.repository

import com.felixbrucker.sleeptracker.data.database.dao.SleepSessionDao
import com.felixbrucker.sleeptracker.data.database.entity.SleepSessionEntity
import com.felixbrucker.sleeptracker.data.healthconnect.HealthConnectManager
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferences
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SleepRepositoryTest {

    private val sleepSessionDao: SleepSessionDao = mockk(relaxed = true)
    private val preferencesRepository: SleepPreferencesRepository = mockk(relaxed = true)
    private val healthConnectManager: HealthConnectManager = mockk(relaxed = true)
    private lateinit var repository: SleepRepository

    @Before
    fun setUp() {
        every { preferencesRepository.preferencesFlow } returns flowOf(SleepPreferences())
        every { sleepSessionDao.getSessionCountFlow() } returns flowOf(0)
        every { sleepSessionDao.getAverageWeeklyDuration() } returns flowOf(0.0)
        every { sleepSessionDao.getAverageMonthlyDuration() } returns flowOf(0.0)

        repository = SleepRepository(
            sleepSessionDao = sleepSessionDao,
            preferencesRepository = preferencesRepository,
            healthConnectManager = healthConnectManager
        )
    }

    @Test
    fun startTracking_setsTrackingActiveInPreferences() = runTest {
        val startTime = 123456789L

        repository.startTracking(startTime)

        coVerify { preferencesRepository.setTrackingActive(active = true, startTimeMillis = startTime) }
    }

    @Test
    fun stopTracking_whenNotActive_returnsNull() = runTest {
        every { preferencesRepository.preferencesFlow } returns flowOf(SleepPreferences(isTrackingActive = false))

        val result = repository.stopTracking(200000L)

        assertNull(result)
        coVerify(exactly = 0) { sleepSessionDao.insertSession(any()) }
    }

    @Test
    fun stopTracking_whenActiveWithoutAutoSync_savesSessionWithoutSync() = runTest {
        val startTime = 100000L
        val endTime = 200000L
        val prefs = SleepPreferences(isTrackingActive = true, trackingStartTimeMillis = startTime, fallAsleepDurationMinutes = 0, autoSyncHealthConnect = false)
        every { preferencesRepository.preferencesFlow } returns flowOf(prefs)
        coEvery { sleepSessionDao.insertSession(any()) } returns 1L

        val result = repository.stopTracking(endTime)

        assertNotNull(result)
        assertEquals(100000L, result?.durationMillis)
        assertFalse(result?.syncedToHealthConnect ?: true)
        coVerify { sleepSessionDao.insertSession(any()) }
        coVerify { preferencesRepository.setTrackingActive(active = false, startTimeMillis = 0L) }
        coVerify(exactly = 0) { healthConnectManager.insertSleepSession(any(), any(), any(), any()) }
    }

    @Test
    fun stopTracking_whenActiveWithAutoSync_savesSessionAndMarksSynced() = runTest {
        val startTime = 100000L
        val endTime = 200000L
        val prefs = SleepPreferences(isTrackingActive = true, trackingStartTimeMillis = startTime, fallAsleepDurationMinutes = 0, autoSyncHealthConnect = true)
        every { preferencesRepository.preferencesFlow } returns flowOf(prefs)
        coEvery { sleepSessionDao.insertSession(any()) } returns 1L
        coEvery { healthConnectManager.insertSleepSession("sleep_session_1", 100000L, 200000L) } returns true

        val result = repository.stopTracking(endTime)

        assertNotNull(result)
        assertTrue(result?.syncedToHealthConnect ?: false)
        coVerify { sleepSessionDao.markAsSynced(1L) }
    }

    @Test
    fun deleteSession_removesSessionFromDao() = runTest {
        val sessionId = 42L

        repository.deleteSession(sessionId)

        coVerify { sleepSessionDao.deleteSessionById(sessionId) }
    }

    @Test
    fun updateReminderConfig_updatesValuesInPreferences() = runTest {
        val enabled = true
        val hour = 23
        val minute = 45

        repository.updateReminderConfig(enabled, hour, minute)

        coVerify { preferencesRepository.setReminderConfig(enabled, hour, minute) }
    }

    @Test
    fun setAutoSyncHealthConnect_updatesPreferenceValue() = runTest {
        val enabled = false

        repository.setAutoSyncHealthConnect(enabled)

        coVerify { preferencesRepository.setAutoSyncHealthConnect(enabled) }
    }

    @Test
    fun syncSessionToHealthConnect_whenUnsyncedAndSyncSucceeds_insertsAndMarksSessionAsSynced() = runTest {
        val session = SleepSessionEntity(id = 1L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, syncedToHealthConnect = false)
        coEvery { healthConnectManager.insertSleepSession("sleep_session_1", 1000L, 5000L, "") } returns true

        val result = repository.syncSessionToHealthConnect(session)

        assertTrue(result)
        coVerify { sleepSessionDao.markAsSynced(1L) }
    }

    @Test
    fun syncSessionToHealthConnect_whenAlreadySyncedAndSyncSucceeds_updatesAndMaintainsSynced() = runTest {
        val session = SleepSessionEntity(id = 1L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, syncedToHealthConnect = true)
        coEvery { healthConnectManager.updateSleepSession("sleep_session_1", 1000L, 5000L, "") } returns true

        val result = repository.syncSessionToHealthConnect(session)

        assertTrue(result)
        coVerify { sleepSessionDao.markAsSynced(1L) }
    }

    @Test
    fun syncSessionToHealthConnect_whenUnsyncedAndSyncFails_leavesSessionUnsynced() = runTest {
        val session = SleepSessionEntity(id = 1L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, syncedToHealthConnect = false)
        coEvery { healthConnectManager.insertSleepSession("sleep_session_1", 1000L, 5000L, "") } returns false

        val result = repository.syncSessionToHealthConnect(session)

        assertFalse(result)
        coVerify(exactly = 0) { sleepSessionDao.markAsSynced(any()) }
    }

    @Test
    fun syncSessionToHealthConnect_whenAlreadySyncedAndSyncFails_returnsFalse() = runTest {
        val session = SleepSessionEntity(id = 1L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, syncedToHealthConnect = true)
        coEvery { healthConnectManager.updateSleepSession("sleep_session_1", 1000L, 5000L, "") } returns false

        val result = repository.syncSessionToHealthConnect(session)

        assertFalse(result)
        coVerify(exactly = 0) { sleepSessionDao.markAsSynced(any()) }
    }

    @Test
    fun syncAllUnsyncedToHealthConnect_syncsAllPendingSessions() = runTest {
        val session1 = SleepSessionEntity(id = 1L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, syncedToHealthConnect = false)
        val session2 = SleepSessionEntity(id = 2L, startTimeMillis = 6000L, endTimeMillis = 9000L, durationMillis = 3000L, syncedToHealthConnect = false)
        coEvery { sleepSessionDao.getUnsyncedSessions() } returns listOf(session1, session2)
        coEvery { healthConnectManager.insertSleepSession(any(), any(), any(), any()) } returns true

        val count = repository.syncAllUnsyncedToHealthConnect()

        assertEquals(2, count)
        coVerify { sleepSessionDao.markAsSynced(1L) }
        coVerify { sleepSessionDao.markAsSynced(2L) }
    }

    @Test
    fun stopTracking_withFallAsleepDelay_countsDurationOnlyAfterDelay() = runTest {
        val trackingStart = 100_000L
        val fallAsleepMinutes = 15
        val trackingEnd = 2_000_000L
        val prefs = SleepPreferences(isTrackingActive = true, trackingStartTimeMillis = trackingStart, fallAsleepDurationMinutes = fallAsleepMinutes, autoSyncHealthConnect = false)
        every { preferencesRepository.preferencesFlow } returns flowOf(prefs)
        coEvery { sleepSessionDao.insertSession(any()) } returns 1L

        val result = repository.stopTracking(trackingEnd)

        assertNotNull(result)
        assertEquals(100_000L + 15 * 60_000L, result?.startTimeMillis)
        assertEquals(trackingEnd, result?.endTimeMillis)
    }

    @Test
    fun stopTracking_whenStoppedBeforeFallAsleepTimeFinished_returnsNullAndDoesNotStoreRecord() = runTest {
        val trackingStart = 100_000L
        val fallAsleepMinutes = 30
        val trackingEnd = 500_000L
        val prefs = SleepPreferences(isTrackingActive = true, trackingStartTimeMillis = trackingStart, fallAsleepDurationMinutes = fallAsleepMinutes, autoSyncHealthConnect = false)
        every { preferencesRepository.preferencesFlow } returns flowOf(prefs)

        val result = repository.stopTracking(trackingEnd)

        assertNull(result)
        coVerify(exactly = 0) { sleepSessionDao.insertSession(any()) }
        coVerify { preferencesRepository.setTrackingActive(active = false, startTimeMillis = 0L) }
    }

    @Test
    fun stopTracking_whenStoppedExactlyAtFallAsleepTimeFinished_returnsNullAndDoesNotStoreRecord() = runTest {
        val trackingStart = 100_000L
        val fallAsleepMinutes = 15
        val trackingEnd = trackingStart + fallAsleepMinutes * 60_000L
        val prefs = SleepPreferences(isTrackingActive = true, trackingStartTimeMillis = trackingStart, fallAsleepDurationMinutes = fallAsleepMinutes, autoSyncHealthConnect = false)
        every { preferencesRepository.preferencesFlow } returns flowOf(prefs)

        val result = repository.stopTracking(trackingEnd)

        assertNull(result)
        coVerify(exactly = 0) { sleepSessionDao.insertSession(any()) }
    }

    @Test
    fun setFallAsleepDurationMinutes_validMinutes_updatesPreference() = runTest {
        val minutes = 25

        repository.setFallAsleepDurationMinutes(minutes)

        coVerify { preferencesRepository.setFallAsleepDurationMinutes(25) }
    }

    @Test
    fun setFallAsleepDurationMinutes_aboveMax_coercesToMax() = runTest {
        val minutes = 300

        repository.setFallAsleepDurationMinutes(minutes)

        coVerify { preferencesRepository.setFallAsleepDurationMinutes(180) }
    }

    @Test
    fun setFallAsleepDurationMinutes_belowZero_coercesToZero() = runTest {
        val minutes = -10

        repository.setFallAsleepDurationMinutes(minutes)

        coVerify { preferencesRepository.setFallAsleepDurationMinutes(0) }
    }

    @Test
    fun updateSession_whenSessionDoesNotExist_returnsNull() = runTest {
        coEvery { sleepSessionDao.getSessionById(999L) } returns null

        val result = repository.updateSession(999L, 1000L, 5000L)

        assertNull(result)
    }

    @Test
    fun updateSession_whenSessionExistsWithoutAutoSync_updatesTimesAndMarksUnsynced() = runTest {
        val original = SleepSessionEntity(id = 10L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, syncedToHealthConnect = true)
        coEvery { sleepSessionDao.getSessionById(10L) } returns original
        val prefs = SleepPreferences(autoSyncHealthConnect = false)
        every { preferencesRepository.preferencesFlow } returns flowOf(prefs)

        val updated = repository.updateSession(10L, 2000L, 8000L)

        assertNotNull(updated)
        assertEquals(2000L, updated?.startTimeMillis)
        assertEquals(8000L, updated?.endTimeMillis)
        assertEquals(6000L, updated?.durationMillis)
        assertFalse(updated?.syncedToHealthConnect ?: true)
        coVerify { sleepSessionDao.updateSession(any()) }
    }

    @Test
    fun updateSession_whenSessionExistsWithAutoSyncSuccess_updatesAndMarksSynced() = runTest {
        val original = SleepSessionEntity(id = 10L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, syncedToHealthConnect = false)
        coEvery { sleepSessionDao.getSessionById(10L) } returns original
        val prefs = SleepPreferences(autoSyncHealthConnect = true)
        every { preferencesRepository.preferencesFlow } returns flowOf(prefs)
        coEvery { healthConnectManager.updateSleepSession("sleep_session_10", 3000L, 9000L, "") } returns true

        val updated = repository.updateSession(10L, 3000L, 9000L)

        assertNotNull(updated)
        assertTrue(updated?.syncedToHealthConnect ?: false)
        coVerify { sleepSessionDao.markAsSynced(10L) }
    }

    @Test
    fun updateSession_whenSessionExistsWithAutoSyncFailure_updatesAndLeavesUnsynced() = runTest {
        val original = SleepSessionEntity(id = 10L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, syncedToHealthConnect = false)
        coEvery { sleepSessionDao.getSessionById(10L) } returns original
        val prefs = SleepPreferences(autoSyncHealthConnect = true)
        every { preferencesRepository.preferencesFlow } returns flowOf(prefs)
        coEvery { healthConnectManager.updateSleepSession("sleep_session_10", 3000L, 9000L, "") } returns false

        val updated = repository.updateSession(10L, 3000L, 9000L)

        assertNotNull(updated)
        assertFalse(updated?.syncedToHealthConnect ?: true)
        coVerify(exactly = 0) { sleepSessionDao.markAsSynced(any()) }
    }

    @Test
    fun clientRecordId_returnsFormattedStringWithSessionId() {
        val sessionId = 123L

        val recordId = SleepRepository.clientRecordId(sessionId)

        assertEquals("sleep_session_123", recordId)
    }

    @Test
    fun weeklyAndMonthlyAverages_delegateToDao() = runTest {
        every { sleepSessionDao.getAverageWeeklyDuration() } returns flowOf(4000.0)
        every { sleepSessionDao.getAverageMonthlyDuration() } returns flowOf(5000.0)
        val repo = SleepRepository(
            sleepSessionDao = sleepSessionDao,
            preferencesRepository = preferencesRepository,
            healthConnectManager = healthConnectManager
        )

        val weekly = repo.averageWeeklyDurationMillis.first()
        val monthly = repo.averageMonthlyDurationMillis.first()

        assertEquals(4000L, weekly)
        assertEquals(5000L, monthly)
    }
}
