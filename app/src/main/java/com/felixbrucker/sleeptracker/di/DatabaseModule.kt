package com.felixbrucker.sleeptracker.di

import android.content.Context
import com.felixbrucker.sleeptracker.data.database.AppDatabase
import com.felixbrucker.sleeptracker.data.database.dao.SleepSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.getInstance(context)

    @Provides
    fun provideSleepSessionDao(
        database: AppDatabase
    ): SleepSessionDao = database.sleepSessionDao()
}
