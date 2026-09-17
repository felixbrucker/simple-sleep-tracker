package com.felixbrucker.sleeptracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.felixbrucker.sleeptracker.data.database.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Query("SELECT * FROM sleep_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SleepSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSessionEntity): Long

    @Update
    suspend fun updateSession(session: SleepSessionEntity)

    @Query("DELETE FROM sleep_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("SELECT * FROM sleep_sessions WHERE syncedToHealthConnect = 0")
    suspend fun getUnsyncedSessions(): List<SleepSessionEntity>

    @Query("UPDATE sleep_sessions SET syncedToHealthConnect = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("SELECT COUNT(*) FROM sleep_sessions")
    suspend fun getSessionCount(): Int
}
