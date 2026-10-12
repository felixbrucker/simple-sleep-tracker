package com.felixbrucker.sleeptracker.ui.viewmodel

import com.felixbrucker.sleeptracker.data.database.entity.SleepSessionEntity
import com.felixbrucker.sleeptracker.data.healthconnect.HealthConnectManager
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferences
import com.felixbrucker.sleeptracker.data.repository.SleepRepository
import com.felixbrucker.sleeptracker.service.AlarmScheduler
import com.felixbrucker.sleeptracker.service.SleepNotificationManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository: SleepRepository = mockk(relaxed = true)
    private val notificationManager: SleepNotificationManager = mockk(relaxed = true)
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)
    private val healthConnectManager: HealthConnectManager = mockk(relaxed = true)
    private lateinit var viewModel: SleepViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.pagedSessions } returns flowOf()
        every { repository.preferencesFlow } returns flowOf(SleepPreferences(isTrackingActive = false))
        every { repository.totalSessionCount } returns flowOf(0)
        every { repository.averageWeeklyDurationMillis } returns flowOf(0L)
        every { repository.averageMonthlyDurationMillis } returns flowOf(0L)
        every { healthConnectManager.isAvailable } returns true
        coEvery { healthConnectManager.hasAllPermissions() } returns true

        viewModel = SleepViewModel(
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
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value

        assertFalse(state.isTrackingActive)
        assertEquals(0L, state.currentDurationMillis)
    }

    @Test
    fun clearStatusMessage_clearsMessageInState() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.clearStatusMessage()

        val state = viewModel.uiState.value
        assertEquals(null, state.statusMessage)
    }

    @Test
    fun uiState_calculatesWeeklyAndMonthlyAverages() = runTest {
        every { repository.totalSessionCount } returns flowOf(2)
        every { repository.averageWeeklyDurationMillis } returns flowOf(28_800_000L)
        every { repository.averageMonthlyDurationMillis } returns flowOf(25_200_000L)
        val vm = SleepViewModel(
            repository = repository,
            notificationManager = notificationManager,
            alarmScheduler = alarmScheduler,
            healthConnectManager = healthConnectManager
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        val state = vm.uiState.value
        assertEquals(2, state.totalSleepSessions)
        assertEquals(28_800_000L, state.averageWeeklyDurationMillis)
        assertEquals(25_200_000L, state.averageMonthlyDurationMillis)
    }

    @Test
    fun startTracking_startsTrackingAndShowsNotification() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        coEvery { repository.startTracking(any()) } returns Unit

        viewModel.startTracking()

        coVerify { repository.startTracking(any()) }
        verify { notificationManager.showTrackingActiveNotification(any()) }
        assertEquals("Sleep tracking started", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun stopTracking_whenSessionSynced_updatesStatusMessage() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        val session = SleepSessionEntity(id = 1L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, syncedToHealthConnect = true)
        coEvery { repository.stopTracking(any()) } returns session

        viewModel.stopTracking()

        coVerify { repository.stopTracking(any()) }
        verify { notificationManager.dismissSleepNotification() }
        assertEquals("Sleep tracked & synced to Health Connect!", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun stopTracking_whenSessionNotSynced_updatesStatusMessage() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        val session = SleepSessionEntity(id = 1L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, syncedToHealthConnect = false)
        coEvery { repository.stopTracking(any()) } returns session

        viewModel.stopTracking()

        assertEquals("Sleep tracked successfully!", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun stopTracking_whenSessionNull_updatesStatusMessage() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        coEvery { repository.stopTracking(any()) } returns null

        viewModel.stopTracking()

        assertEquals("Tracking stopped before falling asleep (no sleep recorded)", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun updateReminderConfig_whenEnabled_schedulesReminder() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        coEvery { repository.updateReminderConfig(true, 22, 30) } returns Unit

        viewModel.updateReminderConfig(enabled = true, hour = 22, minute = 30)

        coVerify { repository.updateReminderConfig(true, 22, 30) }
        verify { alarmScheduler.scheduleBedtimeReminder(22, 30) }
        assertEquals("Bedtime reminder updated", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun updateReminderConfig_whenDisabled_cancelsReminder() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        coEvery { repository.updateReminderConfig(false, 22, 30) } returns Unit

        viewModel.updateReminderConfig(enabled = false, hour = 22, minute = 30)

        coVerify { repository.updateReminderConfig(false, 22, 30) }
        verify { alarmScheduler.cancelBedtimeReminder() }
        assertEquals("Bedtime reminder disabled", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun setAutoSyncHealthConnect_whenEnabled_updatesPreference() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        coEvery { repository.setAutoSyncHealthConnect(true) } returns Unit

        viewModel.setAutoSyncHealthConnect(enabled = true)

        coVerify { repository.setAutoSyncHealthConnect(true) }
        assertEquals("Auto-sync to Health Connect enabled", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun sendTestReminder_triggersNotification() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.sendTestReminder()

        verify { notificationManager.showSleepReminderNotification() }
        assertEquals("Reminder notification triggered", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun syncSession_whenSuccessful_updatesStatusMessage() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        val session = SleepSessionEntity(id = 1L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L)
        coEvery { repository.syncSessionToHealthConnect(session) } returns true

        viewModel.syncSession(session)

        assertEquals("Session synced to Health Connect", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun syncAllUnsynced_whenCountGreaterThanZero_updatesStatusMessage() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        coEvery { repository.syncAllUnsyncedToHealthConnect() } returns 2

        viewModel.syncAllUnsynced()

        assertEquals("Synced 2 session(s) to Health Connect", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun deleteSession_deletesInRepository() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        coEvery { repository.deleteSession(1L) } returns Unit

        viewModel.deleteSession(1L)

        coVerify { repository.deleteSession(1L) }
        assertEquals("Session deleted", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun setFallAsleepDurationMinutes_updatesRepository() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        coEvery { repository.setFallAsleepDurationMinutes(15) } returns Unit

        viewModel.setFallAsleepDurationMinutes(15)

        coVerify { repository.setFallAsleepDurationMinutes(15) }
        assertEquals("Time to fall asleep set to 15 min", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun updateSession_whenSuccessful_updatesStatusMessage() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        val session = SleepSessionEntity(id = 1L, startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L)
        coEvery { repository.updateSession(1L, 1000L, 5000L) } returns session

        viewModel.updateSession(1L, 1000L, 5000L)

        assertEquals("Sleep record updated", viewModel.uiState.value.statusMessage)
    }
}
