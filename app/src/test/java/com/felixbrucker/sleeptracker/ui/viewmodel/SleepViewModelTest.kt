package com.felixbrucker.sleeptracker.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.felixbrucker.sleeptracker.data.database.dao.SleepSessionDao
import com.felixbrucker.sleeptracker.data.database.entity.SleepSessionEntity
import com.felixbrucker.sleeptracker.data.healthconnect.HealthConnectManager
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferences
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesDataSource
import com.felixbrucker.sleeptracker.data.repository.SleepRepository
import com.felixbrucker.sleeptracker.service.AlarmScheduler
import com.felixbrucker.sleeptracker.service.SleepNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class SleepViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var fakeDao: FakeSleepSessionDao
    private lateinit var fakePreferencesRepo: FakeSleepPreferencesDataSource
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var repository: SleepRepository
    private lateinit var notificationManager: SleepNotificationManager
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var viewModel: SleepViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        fakeDao = FakeSleepSessionDao()
        fakePreferencesRepo = FakeSleepPreferencesDataSource()
        healthConnectManager = HealthConnectManager(application)
        repository = SleepRepository(
            sleepSessionDao = fakeDao,
            preferencesRepository = fakePreferencesRepo,
            healthConnectManager = healthConnectManager
        )
        notificationManager = SleepNotificationManager(application)
        alarmScheduler = AlarmScheduler(application)
        viewModel = SleepViewModel(
            application = application,
            repository = repository,
            notificationManager = notificationManager,
            alarmScheduler = alarmScheduler,
            healthConnectManager = healthConnectManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_whenTrackingInactive_hasZeroLiveDuration() = runTest {
        val state = viewModel.uiState.value

        assertFalse(state.isTrackingActive)
        assertEquals(0L, state.currentDurationMillis)
    }

    @Test
    fun clearStatusMessage_clearsMessageInState() = runTest {
        viewModel.clearStatusMessage()

        val state = viewModel.uiState.value
        assertEquals(null, state.statusMessage)
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
}
