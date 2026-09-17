package com.felixbrucker.sleeptracker.data.repository

import com.felixbrucker.sleeptracker.data.database.dao.SleepSessionDao
import com.felixbrucker.sleeptracker.data.database.entity.SleepSessionEntity
import com.felixbrucker.sleeptracker.data.healthconnect.HealthConnectDataSource
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferences
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SleepRepositoryTest {

    private lateinit var fakeDao: FakeSleepSessionDao
    private lateinit var fakePreferencesRepo: FakeSleepPreferencesDataSource
    private lateinit var fakeHealthConnect: FakeHealthConnectDataSource
    private lateinit var repository: SleepRepository

    @Before
    fun setUp() {
        fakeDao = FakeSleepSessionDao()
        fakePreferencesRepo = FakeSleepPreferencesDataSource()
        fakeHealthConnect = FakeHealthConnectDataSource()
        repository = SleepRepository(
            sleepSessionDao = fakeDao,
            preferencesRepository = fakePreferencesRepo,
            healthConnectManager = fakeHealthConnect
        )
    }

    @Test
    fun startTracking_setsTrackingActiveInPreferences() = runTest {
        val startTime = 123456789L

        repository.startTracking(startTime)

        val prefs = fakePreferencesRepo.preferencesFlow.first()
        assertTrue(prefs.isTrackingActive)
        assertEquals(startTime, prefs.trackingStartTimeMillis)
    }

    @Test
    fun stopTracking_whenNotActive_returnsNull() = runTest {
        val endTime = 200000L

        val result = repository.stopTracking(endTime)

        assertNull(result)
        assertEquals(0, fakeDao.savedSessions.size)
    }

    @Test
    fun stopTracking_whenActiveWithoutAutoSync_savesSessionWithoutSync() = runTest {
        val startTime = 100000L
        val endTime = 200000L
        fakePreferencesRepo.setTrackingActive(active = true, startTimeMillis = startTime)
        fakePreferencesRepo.setFallAsleepDurationMinutes(0)
        fakePreferencesRepo.setAutoSyncHealthConnect(false)

        val result = repository.stopTracking(endTime)

        assertNotNull(result)
        assertEquals(100000L, result?.durationMillis)
        assertFalse(result?.syncedToHealthConnect ?: true)
        assertEquals(1, fakeDao.savedSessions.size)
        val prefs = fakePreferencesRepo.preferencesFlow.first()
        assertFalse(prefs.isTrackingActive)
    }

    @Test
    fun stopTracking_whenActiveWithAutoSync_savesSessionAndMarksSynced() = runTest {
        val startTime = 100000L
        val endTime = 200000L
        fakePreferencesRepo.setTrackingActive(active = true, startTimeMillis = startTime)
        fakePreferencesRepo.setFallAsleepDurationMinutes(0)
        fakePreferencesRepo.setAutoSyncHealthConnect(true)
        fakeHealthConnect.insertResult = true

        val result = repository.stopTracking(endTime)

        assertNotNull(result)
        assertTrue(result?.syncedToHealthConnect ?: false)
        assertEquals(1, fakeDao.savedSessions.size)
        assertTrue(fakeDao.savedSessions.first().syncedToHealthConnect)
        assertEquals(listOf("sleep_session_1"), fakeHealthConnect.insertedClientRecordIds)
    }

    @Test
    fun deleteSession_removesSessionFromDao() = runTest {
        val session = SleepSessionEntity(
            id = 42L,
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            durationMillis = 4000L
        )
        fakeDao.savedSessions.add(session)

        repository.deleteSession(42L)

        assertEquals(0, fakeDao.savedSessions.size)
    }

    @Test
    fun updateReminderConfig_updatesValuesInPreferences() = runTest {
        val enabled = true
        val hour = 23
        val minute = 45

        repository.updateReminderConfig(enabled, hour, minute)

        val prefs = fakePreferencesRepo.preferencesFlow.first()
        assertEquals(enabled, prefs.reminderEnabled)
        assertEquals(hour, prefs.reminderHour)
        assertEquals(minute, prefs.reminderMinute)
    }

    @Test
    fun setAutoSyncHealthConnect_updatesPreferenceValue() = runTest {
        val enabled = false

        repository.setAutoSyncHealthConnect(enabled)

        val prefs = fakePreferencesRepo.preferencesFlow.first()
        assertFalse(prefs.autoSyncHealthConnect)
    }

    @Test
    fun syncSessionToHealthConnect_whenUnsyncedAndSyncSucceeds_insertsAndMarksSessionAsSynced() = runTest {
        val session = SleepSessionEntity(
            id = 1L,
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            durationMillis = 4000L,
            syncedToHealthConnect = false
        )
        fakeDao.savedSessions.add(session)
        fakeHealthConnect.insertResult = true

        val result = repository.syncSessionToHealthConnect(session)

        assertTrue(result)
        assertTrue(fakeDao.savedSessions.first().syncedToHealthConnect)
        assertEquals(listOf("sleep_session_1"), fakeHealthConnect.insertedClientRecordIds)
        assertTrue(fakeHealthConnect.updatedClientRecordIds.isEmpty())
    }

    @Test
    fun syncSessionToHealthConnect_whenAlreadySyncedAndSyncSucceeds_updatesAndMaintainsSynced() = runTest {
        val session = SleepSessionEntity(
            id = 1L,
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            durationMillis = 4000L,
            syncedToHealthConnect = true
        )
        fakeDao.savedSessions.add(session)
        fakeHealthConnect.updateResult = true

        val result = repository.syncSessionToHealthConnect(session)

        assertTrue(result)
        assertTrue(fakeDao.savedSessions.first().syncedToHealthConnect)
        assertEquals(listOf("sleep_session_1"), fakeHealthConnect.updatedClientRecordIds)
        assertTrue(fakeHealthConnect.insertedClientRecordIds.isEmpty())
    }

    @Test
    fun syncSessionToHealthConnect_whenUnsyncedAndSyncFails_leavesSessionUnsynced() = runTest {
        val session = SleepSessionEntity(
            id = 1L,
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            durationMillis = 4000L,
            syncedToHealthConnect = false
        )
        fakeDao.savedSessions.add(session)
        fakeHealthConnect.insertResult = false

        val result = repository.syncSessionToHealthConnect(session)

        assertFalse(result)
        assertFalse(fakeDao.savedSessions.first().syncedToHealthConnect)
    }

    @Test
    fun syncSessionToHealthConnect_whenAlreadySyncedAndSyncFails_returnsFalse() = runTest {
        val session = SleepSessionEntity(
            id = 1L,
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            durationMillis = 4000L,
            syncedToHealthConnect = true
        )
        fakeDao.savedSessions.add(session)
        fakeHealthConnect.updateResult = false

        val result = repository.syncSessionToHealthConnect(session)

        assertFalse(result)
    }

    @Test
    fun syncAllUnsyncedToHealthConnect_syncsAllPendingSessions() = runTest {
        val session1 = SleepSessionEntity(id = 1L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, syncedToHealthConnect = false)
        val session2 = SleepSessionEntity(id = 2L, startTimeMillis = 6000L, endTimeMillis = 9000L, durationMillis = 3000L, syncedToHealthConnect = false)
        fakeDao.savedSessions.addAll(listOf(session1, session2))
        fakeHealthConnect.insertResult = true

        val count = repository.syncAllUnsyncedToHealthConnect()

        assertEquals(2, count)
        assertTrue(fakeDao.savedSessions.all { it.syncedToHealthConnect })
        assertEquals(listOf("sleep_session_1", "sleep_session_2"), fakeHealthConnect.insertedClientRecordIds)
    }

    @Test
    fun stopTracking_withFallAsleepDelay_countsDurationOnlyAfterDelay() = runTest {
        val trackingStart = 100_000L
        val fallAsleepMinutes = 15
        val fallAsleepMillis = fallAsleepMinutes * 60_000L
        val expectedSleepStart = trackingStart + fallAsleepMillis
        val trackingEnd = 2_000_000L
        val expectedDuration = trackingEnd - expectedSleepStart
        fakePreferencesRepo.setTrackingActive(active = true, startTimeMillis = trackingStart)
        fakePreferencesRepo.setFallAsleepDurationMinutes(fallAsleepMinutes)
        fakePreferencesRepo.setAutoSyncHealthConnect(false)

        val result = repository.stopTracking(trackingEnd)

        assertNotNull(result)
        assertEquals(expectedSleepStart, result?.startTimeMillis)
        assertEquals(trackingEnd, result?.endTimeMillis)
        assertEquals(expectedDuration, result?.durationMillis)
        assertEquals(1, fakeDao.savedSessions.size)
    }

    @Test
    fun stopTracking_whenStoppedBeforeFallAsleepTimeFinished_returnsNullAndDoesNotStoreRecord() = runTest {
        val trackingStart = 100_000L
        val fallAsleepMinutes = 30
        val trackingEnd = 500_000L
        fakePreferencesRepo.setTrackingActive(active = true, startTimeMillis = trackingStart)
        fakePreferencesRepo.setFallAsleepDurationMinutes(fallAsleepMinutes)
        fakePreferencesRepo.setAutoSyncHealthConnect(false)

        val result = repository.stopTracking(trackingEnd)

        assertNull(result)
        assertTrue(fakeDao.savedSessions.isEmpty())
        assertFalse(fakePreferencesRepo.preferencesFlow.first().isTrackingActive)
    }

    @Test
    fun stopTracking_whenStoppedExactlyAtFallAsleepTimeFinished_returnsNullAndDoesNotStoreRecord() = runTest {
        val trackingStart = 100_000L
        val fallAsleepMinutes = 15
        val trackingEnd = trackingStart + fallAsleepMinutes * 60_000L
        fakePreferencesRepo.setTrackingActive(active = true, startTimeMillis = trackingStart)
        fakePreferencesRepo.setFallAsleepDurationMinutes(fallAsleepMinutes)
        fakePreferencesRepo.setAutoSyncHealthConnect(false)

        val result = repository.stopTracking(trackingEnd)

        assertNull(result)
        assertTrue(fakeDao.savedSessions.isEmpty())
        assertFalse(fakePreferencesRepo.preferencesFlow.first().isTrackingActive)
    }

    @Test
    fun setFallAsleepDurationMinutes_validMinutes_updatesPreference() = runTest {
        val minutes = 25

        repository.setFallAsleepDurationMinutes(minutes)

        val prefs = fakePreferencesRepo.preferencesFlow.first()
        assertEquals(25, prefs.fallAsleepDurationMinutes)
    }

    @Test
    fun setFallAsleepDurationMinutes_aboveMax_coercesToMax() = runTest {
        val minutes = 300

        repository.setFallAsleepDurationMinutes(minutes)

        val prefs = fakePreferencesRepo.preferencesFlow.first()
        assertEquals(180, prefs.fallAsleepDurationMinutes)
    }

    @Test
    fun setFallAsleepDurationMinutes_belowZero_coercesToZero() = runTest {
        val minutes = -10

        repository.setFallAsleepDurationMinutes(minutes)

        val prefs = fakePreferencesRepo.preferencesFlow.first()
        assertEquals(0, prefs.fallAsleepDurationMinutes)
    }

    @Test
    fun updateSession_whenSessionDoesNotExist_returnsNull() = runTest {
        val nonExistentId = 999L

        val result = repository.updateSession(nonExistentId, 1000L, 5000L)

        assertNull(result)
    }

    @Test
    fun updateSession_whenSessionExistsWithoutAutoSync_updatesTimesAndMarksUnsynced() = runTest {
        val original = SleepSessionEntity(
            id = 10L,
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            durationMillis = 4000L,
            syncedToHealthConnect = true
        )
        fakeDao.savedSessions.add(original)
        fakePreferencesRepo.setAutoSyncHealthConnect(false)
        val newStart = 2000L
        val newEnd = 8000L

        val updated = repository.updateSession(10L, newStart, newEnd)

        assertNotNull(updated)
        assertEquals(newStart, updated?.startTimeMillis)
        assertEquals(newEnd, updated?.endTimeMillis)
        assertEquals(6000L, updated?.durationMillis)
        assertFalse(updated?.syncedToHealthConnect ?: true)
        val saved = fakeDao.savedSessions.first { it.id == 10L }
        assertEquals(6000L, saved.durationMillis)
        assertFalse(saved.syncedToHealthConnect)
    }

    @Test
    fun updateSession_whenSessionExistsWithAutoSyncSuccess_updatesAndMarksSynced() = runTest {
        val original = SleepSessionEntity(
            id = 10L,
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            durationMillis = 4000L,
            syncedToHealthConnect = false
        )
        fakeDao.savedSessions.add(original)
        fakePreferencesRepo.setAutoSyncHealthConnect(true)
        fakeHealthConnect.updateResult = true
        val newStart = 3000L
        val newEnd = 9000L

        val updated = repository.updateSession(10L, newStart, newEnd)

        assertNotNull(updated)
        assertEquals(newStart, updated?.startTimeMillis)
        assertEquals(newEnd, updated?.endTimeMillis)
        assertEquals(6000L, updated?.durationMillis)
        assertTrue(updated?.syncedToHealthConnect ?: false)
        val saved = fakeDao.savedSessions.first { it.id == 10L }
        assertTrue(saved.syncedToHealthConnect)
        assertEquals(listOf("sleep_session_10"), fakeHealthConnect.updatedClientRecordIds)
        assertTrue(fakeHealthConnect.insertedClientRecordIds.isEmpty())
    }

    @Test
    fun updateSession_whenSessionExistsWithAutoSyncFailure_updatesAndLeavesUnsynced() = runTest {
        val original = SleepSessionEntity(
            id = 10L,
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            durationMillis = 4000L,
            syncedToHealthConnect = false
        )
        fakeDao.savedSessions.add(original)
        fakePreferencesRepo.setAutoSyncHealthConnect(true)
        fakeHealthConnect.updateResult = false
        val newStart = 3000L
        val newEnd = 9000L

        val updated = repository.updateSession(10L, newStart, newEnd)

        assertNotNull(updated)
        assertFalse(updated?.syncedToHealthConnect ?: true)
        val saved = fakeDao.savedSessions.first { it.id == 10L }
        assertFalse(saved.syncedToHealthConnect)
    }

    @Test
    fun clientRecordId_returnsFormattedStringWithSessionId() {
        val sessionId = 123L

        val recordId = SleepRepository.clientRecordId(sessionId)

        assertEquals("sleep_session_123", recordId)
    }

    private class FakeSleepSessionDao : SleepSessionDao {
        val savedSessions = mutableListOf<SleepSessionEntity>()
        private val sessionsFlow = MutableStateFlow<List<SleepSessionEntity>>(emptyList())

        override fun getAllSessions(): Flow<List<SleepSessionEntity>> = sessionsFlow

        override suspend fun getSessionById(id: Long): SleepSessionEntity? =
            savedSessions.find { it.id == id }

        override suspend fun insertSession(session: SleepSessionEntity): Long {
            val assignedId = if (session.id == 0L) (savedSessions.size + 1).toLong() else session.id
            val inserted = session.copy(id = assignedId)
            savedSessions.add(inserted)
            sessionsFlow.value = savedSessions.toList()
            return assignedId
        }

        override suspend fun updateSession(session: SleepSessionEntity) {
            val index = savedSessions.indexOfFirst { it.id == session.id }
            if (index >= 0) {
                savedSessions[index] = session
                sessionsFlow.value = savedSessions.toList()
            }
        }

        override suspend fun deleteSessionById(id: Long) {
            savedSessions.removeAll { it.id == id }
            sessionsFlow.value = savedSessions.toList()
        }

        override suspend fun getUnsyncedSessions(): List<SleepSessionEntity> =
            savedSessions.filter { !it.syncedToHealthConnect }

        override suspend fun markAsSynced(id: Long) {
            val index = savedSessions.indexOfFirst { it.id == id }
            if (index >= 0) {
                savedSessions[index] = savedSessions[index].copy(syncedToHealthConnect = true)
                sessionsFlow.value = savedSessions.toList()
            }
        }

        override suspend fun getSessionCount(): Int = savedSessions.size
    }

    private class FakeSleepPreferencesDataSource : SleepPreferencesDataSource {
        private val _flow = MutableStateFlow(SleepPreferences())
        override val preferencesFlow: Flow<SleepPreferences> = _flow

        override suspend fun setTrackingActive(active: Boolean, startTimeMillis: Long) {
            _flow.value = _flow.value.copy(
                isTrackingActive = active,
                trackingStartTimeMillis = if (active) startTimeMillis else 0L
            )
        }

        override suspend fun setReminderConfig(enabled: Boolean, hour: Int, minute: Int) {
            _flow.value = _flow.value.copy(
                reminderEnabled = enabled,
                reminderHour = hour,
                reminderMinute = minute
            )
        }

        override suspend fun setAutoSyncHealthConnect(enabled: Boolean) {
            _flow.value = _flow.value.copy(
                autoSyncHealthConnect = enabled
            )
        }

        override suspend fun setFallAsleepDurationMinutes(minutes: Int) {
            _flow.value = _flow.value.copy(
                fallAsleepDurationMinutes = minutes
            )
        }
    }

    private class FakeHealthConnectDataSource : HealthConnectDataSource {
        var insertResult = true
        var updateResult = true
        val insertedClientRecordIds = mutableListOf<String>()
        val updatedClientRecordIds = mutableListOf<String>()
        override val isAvailable: Boolean = true

        override suspend fun hasAllPermissions(): Boolean = true

        override suspend fun insertSleepSession(
            clientRecordId: String,
            startTimeMillis: Long,
            endTimeMillis: Long,
            notes: String
        ): Boolean {
            insertedClientRecordIds.add(clientRecordId)
            return insertResult
        }

        override suspend fun updateSleepSession(
            clientRecordId: String,
            startTimeMillis: Long,
            endTimeMillis: Long,
            notes: String
        ): Boolean {
            updatedClientRecordIds.add(clientRecordId)
            return updateResult
        }
    }
}
