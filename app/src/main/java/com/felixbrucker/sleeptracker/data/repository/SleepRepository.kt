package com.felixbrucker.sleeptracker.data.repository

import com.felixbrucker.sleeptracker.data.database.dao.SleepSessionDao
import com.felixbrucker.sleeptracker.data.database.entity.SleepSessionEntity
import com.felixbrucker.sleeptracker.data.healthconnect.HealthConnectDataSource
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferences
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SleepRepository(
    private val sleepSessionDao: SleepSessionDao,
    private val preferencesRepository: SleepPreferencesDataSource,
    private val healthConnectManager: HealthConnectDataSource
) {

    val allSessions: Flow<List<SleepSessionEntity>> = sleepSessionDao.getAllSessions()

    val preferencesFlow: Flow<SleepPreferences> = preferencesRepository.preferencesFlow

    suspend fun startTracking(startTimeMillis: Long = System.currentTimeMillis()) {
        preferencesRepository.setTrackingActive(active = true, startTimeMillis = startTimeMillis)
    }

    suspend fun stopTracking(endTimeMillis: Long = System.currentTimeMillis()): SleepSessionEntity? {
        val currentPrefs = preferencesRepository.preferencesFlow.first()
        if (!currentPrefs.isTrackingActive) return null

        val startTime = currentPrefs.trackingStartTimeMillis
        val trackingStart = if (startTime > 0L) startTime else endTimeMillis
        val fallAsleepMillis = (currentPrefs.fallAsleepDurationMinutes * 60_000L).coerceAtLeast(0L)
        val sleepStartTime = trackingStart + fallAsleepMillis

        // If stopped before the fall asleep time finished, do not store a sleep record (no sleep)
        if (endTimeMillis <= sleepStartTime) {
            preferencesRepository.setTrackingActive(active = false, startTimeMillis = 0L)
            return null
        }

        val duration = endTimeMillis - sleepStartTime

        val session = SleepSessionEntity(
            startTimeMillis = sleepStartTime,
            endTimeMillis = endTimeMillis,
            durationMillis = duration,
            syncedToHealthConnect = false
        )

        val insertedId = sleepSessionDao.insertSession(session)
        val savedSession = session.copy(id = insertedId)

        preferencesRepository.setTrackingActive(active = false, startTimeMillis = 0L)

        if (currentPrefs.autoSyncHealthConnect) {
            val synced = healthConnectManager.insertSleepSession(
                clientRecordId = clientRecordId(insertedId),
                startTimeMillis = sleepStartTime,
                endTimeMillis = endTimeMillis
            )
            if (synced) {
                sleepSessionDao.markAsSynced(insertedId)
                return savedSession.copy(syncedToHealthConnect = true)
            }
        }

        return savedSession
    }

    suspend fun updateSession(id: Long, startTimeMillis: Long, endTimeMillis: Long): SleepSessionEntity? {
        val existing = sleepSessionDao.getSessionById(id) ?: return null
        val effectiveEnd = if (endTimeMillis >= startTimeMillis) endTimeMillis else startTimeMillis
        val duration = (effectiveEnd - startTimeMillis).coerceAtLeast(0L)
        val updated = existing.copy(
            startTimeMillis = startTimeMillis,
            endTimeMillis = effectiveEnd,
            durationMillis = duration,
            syncedToHealthConnect = false
        )
        sleepSessionDao.updateSession(updated)

        val currentPrefs = preferencesRepository.preferencesFlow.first()
        if (currentPrefs.autoSyncHealthConnect) {
            val synced = healthConnectManager.updateSleepSession(
                clientRecordId = clientRecordId(id),
                startTimeMillis = startTimeMillis,
                endTimeMillis = effectiveEnd,
                notes = updated.notes
            )
            if (synced) {
                sleepSessionDao.markAsSynced(id)
                return updated.copy(syncedToHealthConnect = true)
            }
        }
        return updated
    }

    suspend fun setFallAsleepDurationMinutes(minutes: Int) {
        val coerced = minutes.coerceIn(0, 180)
        preferencesRepository.setFallAsleepDurationMinutes(coerced)
    }

    suspend fun deleteSession(id: Long) {
        sleepSessionDao.deleteSessionById(id)
    }

    suspend fun updateReminderConfig(enabled: Boolean, hour: Int, minute: Int) {
        preferencesRepository.setReminderConfig(enabled, hour, minute)
    }

    suspend fun setAutoSyncHealthConnect(enabled: Boolean) {
        preferencesRepository.setAutoSyncHealthConnect(enabled)
    }

    suspend fun syncSessionToHealthConnect(session: SleepSessionEntity): Boolean {
        val clientRecordId = clientRecordId(session.id)
        val success = if (session.syncedToHealthConnect) {
            healthConnectManager.updateSleepSession(
                clientRecordId = clientRecordId,
                startTimeMillis = session.startTimeMillis,
                endTimeMillis = session.endTimeMillis,
                notes = session.notes
            )
        } else {
            healthConnectManager.insertSleepSession(
                clientRecordId = clientRecordId,
                startTimeMillis = session.startTimeMillis,
                endTimeMillis = session.endTimeMillis,
                notes = session.notes
            )
        }
        if (success) {
            sleepSessionDao.markAsSynced(session.id)
        }
        return success
    }

    suspend fun syncAllUnsyncedToHealthConnect(): Int {
        val unsynced = sleepSessionDao.getUnsyncedSessions()
        var successCount = 0
        for (session in unsynced) {
            val success = healthConnectManager.insertSleepSession(
                clientRecordId = clientRecordId(session.id),
                startTimeMillis = session.startTimeMillis,
                endTimeMillis = session.endTimeMillis,
                notes = session.notes
            )
            if (success) {
                sleepSessionDao.markAsSynced(session.id)
                successCount++
            }
        }
        return successCount
    }

    companion object {
        fun clientRecordId(id: Long): String = "sleep_session_$id"
    }
}
