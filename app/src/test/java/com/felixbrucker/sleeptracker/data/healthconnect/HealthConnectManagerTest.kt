package com.felixbrucker.sleeptracker.data.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthConnectManagerTest {

    private val client: HealthConnectClient = mockk(relaxed = true)
    private val permissionController: PermissionController = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { client.permissionController } returns permissionController
    }

    @Test
    fun isAvailable_whenClientProvided_returnsTrue() {
        val manager = HealthConnectManager(healthConnectClient = client)

        val available = manager.isAvailable

        assertTrue(available)
    }

    @Test
    fun isAvailable_whenClientNull_returnsFalse() {
        val manager = HealthConnectManager(healthConnectClient = null)

        val available = manager.isAvailable

        assertFalse(available)
    }

    @Test
    fun hasAllPermissions_whenClientNull_returnsFalse() = runTest {
        val manager = HealthConnectManager(healthConnectClient = null)

        val result = manager.hasAllPermissions()

        assertFalse(result)
    }

    @Test
    fun hasAllPermissions_whenAllRequiredPermissionsGranted_returnsTrue() = runTest {
        coEvery { permissionController.getGrantedPermissions() } returns HealthConnectManager.REQUIRED_PERMISSIONS
        val manager = HealthConnectManager(healthConnectClient = client)

        val result = manager.hasAllPermissions()

        assertTrue(result)
    }

    @Test
    fun hasAllPermissions_whenPermissionsMissing_returnsFalse() = runTest {
        coEvery { permissionController.getGrantedPermissions() } returns setOf(HealthConnectManager.REQUIRED_PERMISSIONS.first())
        val manager = HealthConnectManager(healthConnectClient = client)

        val result = manager.hasAllPermissions()

        assertFalse(result)
    }

    @Test
    fun hasAllPermissions_whenExceptionThrown_returnsFalse() = runTest {
        coEvery { permissionController.getGrantedPermissions() } throws RuntimeException("Permission error")
        val manager = HealthConnectManager(healthConnectClient = client)

        val result = manager.hasAllPermissions()

        assertFalse(result)
    }

    @Test
    fun insertSleepSession_whenClientNull_returnsFalse() = runTest {
        val manager = HealthConnectManager(healthConnectClient = null)

        val result = manager.insertSleepSession(
            clientRecordId = "sleep_session_1",
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            notes = "Great sleep"
        )

        assertFalse(result)
    }

    @Test
    fun insertSleepSession_whenSuccessful_insertsRecordWithClientRecordIdAndReturnsTrue() = runTest {
        val recordSlot = slot<List<SleepSessionRecord>>()
        coEvery { client.insertRecords(capture(recordSlot)) } returns InsertRecordsResponse(listOf("record_id"))
        val manager = HealthConnectManager(healthConnectClient = client)

        val result = manager.insertSleepSession(
            clientRecordId = "sleep_session_42",
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            notes = "Deep sleep"
        )

        assertTrue(result)
        assertEquals(1, recordSlot.captured.size)
        assertEquals("sleep_session_42", recordSlot.captured.first().metadata.clientRecordId)
        assertEquals("Deep sleep", recordSlot.captured.first().notes)
    }

    @Test
    fun insertSleepSession_whenNotesEmpty_setsNotesToNull() = runTest {
        val recordSlot = slot<List<SleepSessionRecord>>()
        coEvery { client.insertRecords(capture(recordSlot)) } returns InsertRecordsResponse(listOf("record_id"))
        val manager = HealthConnectManager(healthConnectClient = client)

        val result = manager.insertSleepSession(
            clientRecordId = "sleep_session_43",
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            notes = ""
        )

        assertTrue(result)
        assertEquals(1, recordSlot.captured.size)
        assertNull(recordSlot.captured.first().notes)
    }

    @Test
    fun insertSleepSession_whenClientThrowsException_returnsFalse() = runTest {
        coEvery { client.insertRecords(any()) } throws RuntimeException("Insert failed")
        val manager = HealthConnectManager(healthConnectClient = client)

        val result = manager.insertSleepSession(
            clientRecordId = "sleep_session_44",
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            notes = "Sample note"
        )

        assertFalse(result)
    }

    @Test
    fun updateSleepSession_whenClientNull_returnsFalse() = runTest {
        val manager = HealthConnectManager(healthConnectClient = null)

        val result = manager.updateSleepSession(
            clientRecordId = "sleep_session_1",
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            notes = "Updated note"
        )

        assertFalse(result)
    }

    @Test
    fun updateSleepSession_whenUpdateSucceeds_updatesRecordWithClientRecordIdAndReturnsTrue() = runTest {
        val recordSlot = slot<List<SleepSessionRecord>>()
        coEvery { client.updateRecords(capture(recordSlot)) } returns Unit
        val manager = HealthConnectManager(healthConnectClient = client)

        val result = manager.updateSleepSession(
            clientRecordId = "sleep_session_10",
            startTimeMillis = 2000L,
            endTimeMillis = 8000L,
            notes = "Updated note"
        )

        assertTrue(result)
        assertEquals(1, recordSlot.captured.size)
        assertEquals("sleep_session_10", recordSlot.captured.first().metadata.clientRecordId)
        assertEquals("Updated note", recordSlot.captured.first().notes)
    }

    @Test
    fun updateSleepSession_whenUpdateThrowsAndInsertSucceeds_fallsBackToInsertAndReturnsTrue() = runTest {
        coEvery { client.updateRecords(any()) } throws RuntimeException("Update failed")
        coEvery { client.insertRecords(any()) } returns InsertRecordsResponse(listOf("record_id"))
        val manager = HealthConnectManager(healthConnectClient = client)

        val result = manager.updateSleepSession(
            clientRecordId = "sleep_session_11",
            startTimeMillis = 2000L,
            endTimeMillis = 8000L,
            notes = "Fallback note"
        )

        assertTrue(result)
        coVerify { client.insertRecords(any()) }
    }

    @Test
    fun updateSleepSession_whenUpdateAndInsertBothThrow_returnsFalse() = runTest {
        coEvery { client.updateRecords(any()) } throws RuntimeException("Update failed")
        coEvery { client.insertRecords(any()) } throws RuntimeException("Insert failed")
        val manager = HealthConnectManager(healthConnectClient = client)

        val result = manager.updateSleepSession(
            clientRecordId = "sleep_session_12",
            startTimeMillis = 2000L,
            endTimeMillis = 8000L,
            notes = "Failed note"
        )

        assertFalse(result)
    }

    @Test
    fun deleteSleepSession_whenClientNull_returnsFalse() = runTest {
        val manager = HealthConnectManager(healthConnectClient = null)

        val result = manager.deleteSleepSession(clientRecordId = "sleep_session_1")

        assertFalse(result)
    }

    @Test
    fun deleteSleepSession_whenSuccessful_callsDeleteRecordsAndReturnsTrue() = runTest {
        coEvery {
            client.deleteRecords(
                recordType = SleepSessionRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf("sleep_session_42")
            )
        } returns Unit
        val manager = HealthConnectManager(healthConnectClient = client)

        val result = manager.deleteSleepSession(clientRecordId = "sleep_session_42")

        assertTrue(result)
        coVerify {
            client.deleteRecords(
                recordType = SleepSessionRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf("sleep_session_42")
            )
        }
    }

    @Test
    fun deleteSleepSession_whenClientThrowsException_returnsFalse() = runTest {
        coEvery {
            client.deleteRecords(
                recordType = SleepSessionRecord::class,
                recordIdsList = any(),
                clientRecordIdsList = any()
            )
        } throws RuntimeException("Delete failed")
        val manager = HealthConnectManager(healthConnectClient = client)

        val result = manager.deleteSleepSession(clientRecordId = "sleep_session_43")

        assertFalse(result)
    }
}
