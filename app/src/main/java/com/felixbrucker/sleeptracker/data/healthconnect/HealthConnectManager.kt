package com.felixbrucker.sleeptracker.data.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneId

interface HealthConnectDataSource {
    val isAvailable: Boolean
    suspend fun hasAllPermissions(): Boolean
    suspend fun insertSleepSession(
        clientRecordId: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        notes: String = ""
    ): Boolean
    suspend fun updateSleepSession(
        clientRecordId: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        notes: String = ""
    ): Boolean
}

class HealthConnectManager(
    private val context: Context,
    private val clientProvider: (() -> HealthConnectClient?)? = null
) : HealthConnectDataSource {

    companion object {
        private const val TAG = "HealthConnectManager"

        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getWritePermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class)
        )
    }

    override val isAvailable: Boolean
        get() = clientProvider?.invoke() != null || HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val healthConnectClient: HealthConnectClient?
        get() = clientProvider?.invoke() ?: if (isAvailable) HealthConnectClient.getOrCreate(context) else null

    override suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(REQUIRED_PERMISSIONS)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            false
        }
    }

    override suspend fun insertSleepSession(
        clientRecordId: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        notes: String
    ): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val startInstant = Instant.ofEpochMilli(startTimeMillis)
            val endInstant = Instant.ofEpochMilli(endTimeMillis)
            val zoneOffset = ZoneId.systemDefault().rules.getOffset(startInstant)

            val session = SleepSessionRecord(
                startTime = startInstant,
                startZoneOffset = zoneOffset,
                endTime = endInstant,
                endZoneOffset = zoneOffset,
                title = "Sleep",
                notes = notes.ifEmpty { null },
                metadata = Metadata.manualEntry(clientRecordId = clientRecordId)
            )

            client.insertRecords(listOf(session))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting sleep session into Health Connect", e)
            false
        }
    }

    override suspend fun updateSleepSession(
        clientRecordId: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        notes: String
    ): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val startInstant = Instant.ofEpochMilli(startTimeMillis)
            val endInstant = Instant.ofEpochMilli(endTimeMillis)
            val zoneOffset = ZoneId.systemDefault().rules.getOffset(startInstant)

            val session = SleepSessionRecord(
                startTime = startInstant,
                startZoneOffset = zoneOffset,
                endTime = endInstant,
                endZoneOffset = zoneOffset,
                title = "Sleep",
                notes = notes.ifEmpty { null },
                metadata = Metadata.manualEntry(clientRecordId = clientRecordId)
            )

            try {
                client.updateRecords(listOf(session))
                true
            } catch (updateException: Exception) {
                Log.w(TAG, "updateRecords failed for $clientRecordId, attempting fallback insertRecords", updateException)
                client.insertRecords(listOf(session))
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating sleep session in Health Connect", e)
            false
        }
    }
}
