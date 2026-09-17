package com.felixbrucker.sleeptracker.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

@RunWith(AndroidJUnit4::class)
class HealthConnectManagerTest {

    private lateinit var context: Context
    private val insertedRecords = mutableListOf<SleepSessionRecord>()
    private val updatedRecords = mutableListOf<SleepSessionRecord>()
    private var grantedPermissions = mutableSetOf<String>()
    private var throwOnPermissionCheck = false
    private var throwOnInsert = false
    private var throwOnUpdate = false

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        insertedRecords.clear()
        updatedRecords.clear()
        grantedPermissions.clear()
        throwOnPermissionCheck = false
        throwOnInsert = false
        throwOnUpdate = false
    }

    private fun createFakeClient(): HealthConnectClient {
        val permissionController = Proxy.newProxyInstance(
            PermissionController::class.java.classLoader,
            arrayOf(PermissionController::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
                    if (method.name.startsWith("getGrantedPermissions")) {
                        if (throwOnPermissionCheck) throw RuntimeException("Permission check failed")
                        return grantedPermissions.toSet()
                    }
                    return null
                }
            }
        ) as PermissionController

        return Proxy.newProxyInstance(
            HealthConnectClient::class.java.classLoader,
            arrayOf(HealthConnectClient::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
                    when (method.name) {
                        "getPermissionController" -> return permissionController
                        "insertRecords" -> {
                            if (throwOnInsert) throw RuntimeException("Insert failed")
                            @Suppress("UNCHECKED_CAST")
                            val records = args?.get(0) as? List<SleepSessionRecord> ?: emptyList()
                            insertedRecords.addAll(records)
                            return InsertRecordsResponse(records.map { "record_id" })
                        }
                        "updateRecords" -> {
                            if (throwOnUpdate) throw RuntimeException("Update failed")
                            @Suppress("UNCHECKED_CAST")
                            val records = args?.get(0) as? List<SleepSessionRecord> ?: emptyList()
                            updatedRecords.addAll(records)
                            return Unit
                        }
                    }
                    return null
                }
            }
        ) as HealthConnectClient
    }

    @Test
    fun isAvailable_whenClientProviderProvidesClient_returnsTrue() {
        val fakeClient = createFakeClient()
        val manager = HealthConnectManager(context, clientProvider = { fakeClient })

        val available = manager.isAvailable

        assertTrue(available)
    }

    @Test
    fun isAvailable_whenClientProviderReturnsNullAndSdkUnavailable_returnsFalse() {
        val manager = HealthConnectManager(context, clientProvider = { null })

        val available = manager.isAvailable

        assertFalse(available)
    }

    @Test
    fun hasAllPermissions_whenClientNull_returnsFalse() = runTest {
        val manager = HealthConnectManager(context, clientProvider = { null })

        val result = manager.hasAllPermissions()

        assertFalse(result)
    }

    @Test
    fun hasAllPermissions_whenAllRequiredPermissionsGranted_returnsTrue() = runTest {
        grantedPermissions.addAll(HealthConnectManager.REQUIRED_PERMISSIONS)
        val manager = HealthConnectManager(context, clientProvider = { createFakeClient() })

        val result = manager.hasAllPermissions()

        assertTrue(result)
    }

    @Test
    fun hasAllPermissions_whenPermissionsMissing_returnsFalse() = runTest {
        grantedPermissions.add(HealthConnectManager.REQUIRED_PERMISSIONS.first())
        val manager = HealthConnectManager(context, clientProvider = { createFakeClient() })

        val result = manager.hasAllPermissions()

        assertFalse(result)
    }

    @Test
    fun hasAllPermissions_whenExceptionThrown_returnsFalse() = runTest {
        throwOnPermissionCheck = true
        val manager = HealthConnectManager(context, clientProvider = { createFakeClient() })

        val result = manager.hasAllPermissions()

        assertFalse(result)
    }

    @Test
    fun insertSleepSession_whenClientNull_returnsFalse() = runTest {
        val manager = HealthConnectManager(context, clientProvider = { null })

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
        val manager = HealthConnectManager(context, clientProvider = { createFakeClient() })

        val result = manager.insertSleepSession(
            clientRecordId = "sleep_session_42",
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            notes = "Deep sleep"
        )

        assertTrue(result)
        assertEquals(1, insertedRecords.size)
        assertEquals("sleep_session_42", insertedRecords.first().metadata.clientRecordId)
        assertEquals("Deep sleep", insertedRecords.first().notes)
    }

    @Test
    fun insertSleepSession_whenNotesEmpty_setsNotesToNull() = runTest {
        val manager = HealthConnectManager(context, clientProvider = { createFakeClient() })

        val result = manager.insertSleepSession(
            clientRecordId = "sleep_session_43",
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            notes = ""
        )

        assertTrue(result)
        assertEquals(1, insertedRecords.size)
        assertNull(insertedRecords.first().notes)
    }

    @Test
    fun insertSleepSession_whenClientThrowsException_returnsFalse() = runTest {
        throwOnInsert = true
        val manager = HealthConnectManager(context, clientProvider = { createFakeClient() })

        val result = manager.insertSleepSession(
            clientRecordId = "sleep_session_44",
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            notes = "Sample note"
        )

        assertFalse(result)
        assertTrue(insertedRecords.isEmpty())
    }

    @Test
    fun updateSleepSession_whenClientNull_returnsFalse() = runTest {
        val manager = HealthConnectManager(context, clientProvider = { null })

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
        val manager = HealthConnectManager(context, clientProvider = { createFakeClient() })

        val result = manager.updateSleepSession(
            clientRecordId = "sleep_session_10",
            startTimeMillis = 2000L,
            endTimeMillis = 8000L,
            notes = "Updated note"
        )

        assertTrue(result)
        assertEquals(1, updatedRecords.size)
        assertEquals("sleep_session_10", updatedRecords.first().metadata.clientRecordId)
        assertEquals("Updated note", updatedRecords.first().notes)
        assertTrue(insertedRecords.isEmpty())
    }

    @Test
    fun updateSleepSession_whenUpdateThrowsAndInsertSucceeds_fallsBackToInsertAndReturnsTrue() = runTest {
        throwOnUpdate = true
        val manager = HealthConnectManager(context, clientProvider = { createFakeClient() })

        val result = manager.updateSleepSession(
            clientRecordId = "sleep_session_11",
            startTimeMillis = 2000L,
            endTimeMillis = 8000L,
            notes = "Fallback note"
        )

        assertTrue(result)
        assertTrue(updatedRecords.isEmpty())
        assertEquals(1, insertedRecords.size)
        assertEquals("sleep_session_11", insertedRecords.first().metadata.clientRecordId)
    }

    @Test
    fun updateSleepSession_whenUpdateAndInsertBothThrow_returnsFalse() = runTest {
        throwOnUpdate = true
        throwOnInsert = true
        val manager = HealthConnectManager(context, clientProvider = { createFakeClient() })

        val result = manager.updateSleepSession(
            clientRecordId = "sleep_session_12",
            startTimeMillis = 2000L,
            endTimeMillis = 8000L,
            notes = "Failed note"
        )

        assertFalse(result)
        assertTrue(updatedRecords.isEmpty())
        assertTrue(insertedRecords.isEmpty())
    }
}
