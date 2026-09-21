package com.felixbrucker.sleeptracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.felixbrucker.sleeptracker.data.database.dao.SleepSessionDao
import com.felixbrucker.sleeptracker.data.database.entity.SleepSessionEntity

@Database(
    entities = [SleepSessionEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepSessionDao(): SleepSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sleep_sessions_startTimeMillis` ON `sleep_sessions` (`startTimeMillis`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sleep_sessions_syncedToHealthConnect` ON `sleep_sessions` (`syncedToHealthConnect`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sleep_tracker.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
