package com.felixbrucker.sleeptracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.felixbrucker.sleeptracker.data.database.AppDatabase
import com.felixbrucker.sleeptracker.data.database.entity.SleepSessionEntity
import com.felixbrucker.sleeptracker.data.healthconnect.HealthConnectManager
import com.felixbrucker.sleeptracker.data.preferences.SleepPreferencesRepository
import com.felixbrucker.sleeptracker.data.preferences.sleepDataStore
import com.felixbrucker.sleeptracker.data.repository.SleepRepository
import com.felixbrucker.sleeptracker.service.AlarmScheduler
import com.felixbrucker.sleeptracker.service.SleepNotificationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

data class SleepTrackerUiState(
    val isTrackingActive: Boolean = false,
    val trackingStartTimeMillis: Long = 0L,
    val currentDurationMillis: Long = 0L,
    val fallAsleepDurationMinutes: Int = 15,
    val isFallingAsleepPhase: Boolean = false,
    val remainingFallAsleepMinutes: Int = 0,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 22,
    val reminderMinute: Int = 30,
    val autoSyncHealthConnect: Boolean = true,
    val healthConnectAvailable: Boolean = false,
    val healthConnectPermissionsGranted: Boolean = false,
    val sessions: List<SleepSessionEntity> = emptyList(),
    val totalSleepSessions: Int = 0,
    val totalSleepDurationMillis: Long = 0L,
    val averageSleepDurationMillis: Long = 0L,
    val statusMessage: String? = null
)

class SleepViewModel(
    application: Application,
    private val repository: SleepRepository,
    private val notificationManager: SleepNotificationManager,
    private val alarmScheduler: AlarmScheduler,
    private val healthConnectManager: HealthConnectManager
) : AndroidViewModel(application) {

    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _liveDuration = MutableStateFlow(0L)
    private val _healthConnectStatus = MutableStateFlow(
        Pair(healthConnectManager.isAvailable, false)
    )

    val uiState: StateFlow<SleepTrackerUiState> = combine(
        repository.preferencesFlow,
        repository.allSessions,
        _liveDuration,
        _healthConnectStatus,
        _statusMessage
    ) { prefs, sessions, duration, hcStatus, message ->
        val totalSessions = sessions.size
        val totalDuration = sessions.sumOf { it.durationMillis }
        val avgDuration = if (totalSessions > 0) totalDuration / totalSessions else 0L

        val now = System.currentTimeMillis()
        val fallAsleepMillis = prefs.fallAsleepDurationMinutes * 60_000L
        val sleepStartTime = prefs.trackingStartTimeMillis + fallAsleepMillis
        val isFallingAsleep = prefs.isTrackingActive && prefs.trackingStartTimeMillis > 0L && now < sleepStartTime
        val remainingMin = if (isFallingAsleep) {
            val rem = sleepStartTime - now
            ((rem + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
        } else {
            0
        }

        SleepTrackerUiState(
            isTrackingActive = prefs.isTrackingActive,
            trackingStartTimeMillis = prefs.trackingStartTimeMillis,
            currentDurationMillis = if (prefs.isTrackingActive) duration else 0L,
            fallAsleepDurationMinutes = prefs.fallAsleepDurationMinutes,
            isFallingAsleepPhase = isFallingAsleep,
            remainingFallAsleepMinutes = remainingMin,
            reminderEnabled = prefs.reminderEnabled,
            reminderHour = prefs.reminderHour,
            reminderMinute = prefs.reminderMinute,
            autoSyncHealthConnect = prefs.autoSyncHealthConnect,
            healthConnectAvailable = hcStatus.first,
            healthConnectPermissionsGranted = hcStatus.second,
            sessions = sessions,
            totalSleepSessions = totalSessions,
            totalSleepDurationMillis = totalDuration,
            averageSleepDurationMillis = avgDuration,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SleepTrackerUiState(
            healthConnectAvailable = healthConnectManager.isAvailable
        )
    )

    init {
        refreshHealthConnectPermissions()
        startLiveDurationTimer()
    }

    private fun startLiveDurationTimer() {
        viewModelScope.launch {
            while (isActive) {
                val state = uiState.value
                if (state.isTrackingActive && state.trackingStartTimeMillis > 0L) {
                    val now = System.currentTimeMillis()
                    val sleepStartTime = state.trackingStartTimeMillis + (state.fallAsleepDurationMinutes * 60_000L)
                    if (now >= sleepStartTime) {
                        _liveDuration.value = (now - sleepStartTime).coerceAtLeast(0L)
                    } else {
                        _liveDuration.value = 0L
                    }
                } else {
                    _liveDuration.value = 0L
                }
                delay(1.seconds)
            }
        }
    }

    fun refreshHealthConnectPermissions() {
        viewModelScope.launch {
            if (healthConnectManager.isAvailable) {
                val hasPerms = healthConnectManager.hasAllPermissions()
                _healthConnectStatus.value = Pair(true, hasPerms)
            } else {
                _healthConnectStatus.value = Pair(false, false)
            }
        }
    }

    fun startTracking() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            repository.startTracking(startTime)
            notificationManager.showTrackingActiveNotification(startTime)
            _statusMessage.value = "Sleep tracking started"
        }
    }

    fun stopTracking() {
        viewModelScope.launch {
            val session = repository.stopTracking()
            notificationManager.dismissSleepNotification()
            if (session != null) {
                val message = if (session.syncedToHealthConnect) {
                    "Sleep tracked & synced to Health Connect!"
                } else {
                    "Sleep tracked successfully!"
                }
                _statusMessage.value = message
            } else {
                _statusMessage.value = "Tracking stopped before falling asleep (no sleep recorded)"
            }
        }
    }

    fun updateReminderConfig(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.updateReminderConfig(enabled, hour, minute)
            if (enabled) {
                alarmScheduler.scheduleBedtimeReminder(hour, minute)
                _statusMessage.value = "Bedtime reminder updated"
            } else {
                alarmScheduler.cancelBedtimeReminder()
                _statusMessage.value = "Bedtime reminder disabled"
            }
        }
    }

    fun setAutoSyncHealthConnect(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSyncHealthConnect(enabled)
            _statusMessage.value = if (enabled) {
                "Auto-sync to Health Connect enabled"
            } else {
                "Auto-sync disabled"
            }
        }
    }

    fun sendTestReminder() {
        notificationManager.showSleepReminderNotification()
        _statusMessage.value = "Reminder notification triggered"
    }

    fun syncSession(session: SleepSessionEntity) {
        viewModelScope.launch {
            val success = repository.syncSessionToHealthConnect(session)
            _statusMessage.value = if (success) {
                "Session synced to Health Connect"
            } else {
                "Failed to sync with Health Connect"
            }
            refreshHealthConnectPermissions()
        }
    }

    fun syncAllUnsynced() {
        viewModelScope.launch {
            val count = repository.syncAllUnsyncedToHealthConnect()
            _statusMessage.value = if (count > 0) {
                "Synced $count session(s) to Health Connect"
            } else {
                "All sessions already synced or Health Connect unavailable"
            }
            refreshHealthConnectPermissions()
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            _statusMessage.value = "Session deleted"
        }
    }

    fun setFallAsleepDurationMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setFallAsleepDurationMinutes(minutes)
            _statusMessage.value = if (minutes > 0) {
                "Time to fall asleep set to $minutes min"
            } else {
                "Time to fall asleep disabled (immediate)"
            }
        }
    }

    fun updateSession(sessionId: Long, startTimeMillis: Long, endTimeMillis: Long) {
        viewModelScope.launch {
            val updated = repository.updateSession(sessionId, startTimeMillis, endTimeMillis)
            if (updated != null) {
                _statusMessage.value = "Sleep record updated"
            } else {
                _statusMessage.value = "Record not found"
            }
            refreshHealthConnectPermissions()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getInstance(application)
            val preferencesRepo = SleepPreferencesRepository(application.sleepDataStore)
            val healthConnectManager = HealthConnectManager(application)
            val repository = SleepRepository(
                sleepSessionDao = database.sleepSessionDao(),
                preferencesRepository = preferencesRepo,
                healthConnectManager = healthConnectManager
            )
            val notificationManager = SleepNotificationManager(application)
            val alarmScheduler = AlarmScheduler(application)

            return SleepViewModel(
                application = application,
                repository = repository,
                notificationManager = notificationManager,
                alarmScheduler = alarmScheduler,
                healthConnectManager = healthConnectManager
            ) as T
        }
    }
}
