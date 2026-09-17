package com.felixbrucker.sleeptracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.felixbrucker.sleeptracker.data.database.dao.SleepSessionDao
import com.felixbrucker.sleeptracker.data.database.entity.SleepSessionEntity

@Database(
    entities = [SleepSessionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepSessionDao(): SleepSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sleep_tracker.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
