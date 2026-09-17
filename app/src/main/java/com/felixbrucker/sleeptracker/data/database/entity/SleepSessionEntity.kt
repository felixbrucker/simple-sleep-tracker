package com.felixbrucker.sleeptracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long,
    val syncedToHealthConnect: Boolean = false,
    val notes: String = ""
)
