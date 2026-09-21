package com.felixbrucker.sleeptracker.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Indices on startTimeMillis and syncedToHealthConnect speed up sorting and unsynced query filtering.
@Entity(
    tableName = "sleep_sessions",
    indices = [
        Index(value = ["startTimeMillis"]),
        Index(value = ["syncedToHealthConnect"])
    ]
)
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long,
    val syncedToHealthConnect: Boolean = false,
    val notes: String = ""
)
