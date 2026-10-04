package com.felixbrucker.sleeptracker.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.felixbrucker.sleeptracker.data.database.AppDatabase
import com.felixbrucker.sleeptracker.data.database.entity.SleepSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class SleepSessionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SleepSessionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.sleepSessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getAverageWeeklyDuration_withMultipleSessionsPerDay_sumsDailyBeforeAveraging() = runTest {
        val now = System.currentTimeMillis()
        val fourHours = 4 * 3600 * 1000L
        val twoHours = 2 * 3600 * 1000L
        val eightHours = 8 * 3600 * 1000L
        val oneDayMillis = 24 * 3600 * 1000L
        val session1Today = SleepSessionEntity(startTimeMillis = now - fourHours, endTimeMillis = now, durationMillis = fourHours)
        val session2Today = SleepSessionEntity(startTimeMillis = now - fourHours - twoHours, endTimeMillis = now - fourHours, durationMillis = twoHours)
        val sessionYesterday = SleepSessionEntity(startTimeMillis = now - oneDayMillis - eightHours, endTimeMillis = now - oneDayMillis, durationMillis = eightHours)
        dao.insertSession(session1Today)
        dao.insertSession(session2Today)
        dao.insertSession(sessionYesterday)

        val result = dao.getAverageWeeklyDuration().first()

        assertEquals(7 * 3600 * 1000.0, result, 1.0)
    }

    @Test
    fun getAverageWeeklyDuration_whenNoSessions_returnsZero() = runTest {
        val result = dao.getAverageWeeklyDuration().first()

        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun getAverageWeeklyDuration_withSessionsOlderThanSevenDays_ignoresOldSessions() = runTest {
        val now = System.currentTimeMillis()
        val eightHours = 8 * 3600 * 1000L
        val tenDaysMillis = 10 * 24 * 3600 * 1000L
        val oldSession = SleepSessionEntity(startTimeMillis = now - tenDaysMillis, endTimeMillis = now - tenDaysMillis + eightHours, durationMillis = eightHours)
        val todaySession = SleepSessionEntity(startTimeMillis = now - eightHours, endTimeMillis = now, durationMillis = eightHours)
        dao.insertSession(oldSession)
        dao.insertSession(todaySession)

        val result = dao.getAverageWeeklyDuration().first()

        assertEquals(eightHours.toDouble(), result, 1.0)
    }

    @Test
    fun getAverageMonthlyDuration_withMultipleSessionsPerDay_sumsDailyBeforeAveraging() = runTest {
        val now = System.currentTimeMillis()
        val threeHours = 3 * 3600 * 1000L
        val tenHours = 10 * 3600 * 1000L
        val oneDayMillis = 24 * 3600 * 1000L
        val session1Today = SleepSessionEntity(startTimeMillis = now - threeHours, endTimeMillis = now, durationMillis = threeHours)
        val session2Today = SleepSessionEntity(startTimeMillis = now - threeHours - threeHours, endTimeMillis = now - threeHours, durationMillis = threeHours)
        val sessionYesterday = SleepSessionEntity(startTimeMillis = now - oneDayMillis - tenHours, endTimeMillis = now - oneDayMillis, durationMillis = tenHours)
        dao.insertSession(session1Today)
        dao.insertSession(session2Today)
        dao.insertSession(sessionYesterday)

        val result = dao.getAverageMonthlyDuration().first()

        assertEquals(8 * 3600 * 1000.0, result, 1.0)
    }

    @Test
    fun getAverageMonthlyDuration_whenNoSessions_returnsZero() = runTest {
        val result = dao.getAverageMonthlyDuration().first()

        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun getAverageMonthlyDuration_withSessionsOlderThanThirtyDays_ignoresOldSessions() = runTest {
        val now = System.currentTimeMillis()
        val sixHours = 6 * 3600 * 1000L
        val thirtyFiveDaysMillis = 35 * 24 * 3600 * 1000L
        val oldSession = SleepSessionEntity(startTimeMillis = now - thirtyFiveDaysMillis, endTimeMillis = now - thirtyFiveDaysMillis + sixHours, durationMillis = sixHours)
        val todaySession = SleepSessionEntity(startTimeMillis = now - sixHours, endTimeMillis = now, durationMillis = sixHours)
        dao.insertSession(oldSession)
        dao.insertSession(todaySession)

        val result = dao.getAverageMonthlyDuration().first()

        assertEquals(sixHours.toDouble(), result, 1.0)
    }

    @Test
    fun insertAndGetSessionById_returnsCorrectSession() = runTest {
        val session = SleepSessionEntity(startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, notes = "test")
        val insertedId = dao.insertSession(session)

        val retrieved = dao.getSessionById(insertedId)

        assertNotNull(retrieved)
        assertEquals(insertedId, retrieved?.id)
        assertEquals("test", retrieved?.notes)
    }

    @Test
    fun getSessionById_whenNotFound_returnsNull() = runTest {
        val retrieved = dao.getSessionById(999L)

        assertNull(retrieved)
    }

    @Test
    fun updateSession_modifiesExistingSession() = runTest {
        val session = SleepSessionEntity(startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L)
        val insertedId = dao.insertSession(session)
        val updatedSession = session.copy(id = insertedId, notes = "updated note")
        dao.updateSession(updatedSession)

        val retrieved = dao.getSessionById(insertedId)

        assertEquals("updated note", retrieved?.notes)
    }

    @Test
    fun deleteSessionById_removesSession() = runTest {
        val session = SleepSessionEntity(startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L)
        val insertedId = dao.insertSession(session)
        dao.deleteSessionById(insertedId)

        val retrieved = dao.getSessionById(insertedId)

        assertNull(retrieved)
    }

    @Test
    fun getUnsyncedSessionsAndMarkAsSynced_managesSyncStatusCorrectly() = runTest {
        val session = SleepSessionEntity(startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L, syncedToHealthConnect = false)
        val insertedId = dao.insertSession(session)
        val unsyncedBefore = dao.getUnsyncedSessions()
        dao.markAsSynced(insertedId)
        val unsyncedAfter = dao.getUnsyncedSessions()

        assertEquals(1, unsyncedBefore.size)
        assertEquals(insertedId, unsyncedBefore[0].id)
        assertTrue(unsyncedAfter.isEmpty())
    }

    @Test
    fun getSessionCount_returnsTotalCount() = runTest {
        val session1 = SleepSessionEntity(startTimeMillis = 1000L, endTimeMillis = 5000L, durationMillis = 4000L)
        val session2 = SleepSessionEntity(startTimeMillis = 6000L, endTimeMillis = 10000L, durationMillis = 4000L)
        dao.insertSession(session1)
        dao.insertSession(session2)

        val count = dao.getSessionCount()
        val countFlow = dao.getSessionCountFlow().first()

        assertEquals(2, count)
        assertEquals(2, countFlow)
    }
}
