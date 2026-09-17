package com.felixbrucker.sleeptracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.sleepDataStore: DataStore<Preferences> by preferencesDataStore(name = "sleep_preferences")

data class SleepPreferences(
    val isTrackingActive: Boolean = false,
    val trackingStartTimeMillis: Long = 0L,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 22,
    val reminderMinute: Int = 30,
    val autoSyncHealthConnect: Boolean = true,
    val fallAsleepDurationMinutes: Int = 15
)

interface SleepPreferencesDataSource {
    val preferencesFlow: Flow<SleepPreferences>
    suspend fun setTrackingActive(active: Boolean, startTimeMillis: Long = 0L)
    suspend fun setReminderConfig(enabled: Boolean, hour: Int, minute: Int)
    suspend fun setAutoSyncHealthConnect(enabled: Boolean)
    suspend fun setFallAsleepDurationMinutes(minutes: Int)
}

class SleepPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : SleepPreferencesDataSource {

    companion object {
        private val KEY_IS_TRACKING = booleanPreferencesKey("is_tracking_active")
        private val KEY_START_TIME = longPreferencesKey("tracking_start_time_millis")
        private val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        private val KEY_AUTO_SYNC_HEALTH = booleanPreferencesKey("auto_sync_health_connect")
        private val KEY_FALL_ASLEEP_MINUTES = intPreferencesKey("fall_asleep_duration_minutes")
    }

    override val preferencesFlow: Flow<SleepPreferences> = dataStore.data.map { preferences ->
        SleepPreferences(
            isTrackingActive = preferences[KEY_IS_TRACKING] ?: false,
            trackingStartTimeMillis = preferences[KEY_START_TIME] ?: 0L,
            reminderEnabled = preferences[KEY_REMINDER_ENABLED] ?: true,
            reminderHour = preferences[KEY_REMINDER_HOUR] ?: 22,
            reminderMinute = preferences[KEY_REMINDER_MINUTE] ?: 30,
            autoSyncHealthConnect = preferences[KEY_AUTO_SYNC_HEALTH] ?: true,
            fallAsleepDurationMinutes = preferences[KEY_FALL_ASLEEP_MINUTES] ?: 15
        )
    }

    override suspend fun setTrackingActive(active: Boolean, startTimeMillis: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_TRACKING] = active
            preferences[KEY_START_TIME] = if (active) startTimeMillis else 0L
        }
    }

    override suspend fun setReminderConfig(enabled: Boolean, hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_REMINDER_ENABLED] = enabled
            preferences[KEY_REMINDER_HOUR] = hour
            preferences[KEY_REMINDER_MINUTE] = minute
        }
    }

    override suspend fun setAutoSyncHealthConnect(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_SYNC_HEALTH] = enabled
        }
    }

    override suspend fun setFallAsleepDurationMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_FALL_ASLEEP_MINUTES] = minutes.coerceIn(0, 180)
        }
    }
}
