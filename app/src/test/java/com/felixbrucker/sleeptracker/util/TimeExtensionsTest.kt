package com.felixbrucker.sleeptracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class TimeExtensionsTest {

    @Test
    fun formattedDuration_withZeroMillis_returnsZeroMinutes() {
        val durationMillis = 0L

        val result = durationMillis.toFormattedDuration()

        assertEquals("0m", result)
    }

    @Test
    fun formattedDuration_withNegativeMillis_returnsZeroMinutes() {
        val durationMillis = -1000L

        val result = durationMillis.toFormattedDuration()

        assertEquals("0m", result)
    }

    @Test
    fun formattedDuration_withSecondsOnly_returnsSecondsString() {
        val durationMillis = 45000L

        val result = durationMillis.toFormattedDuration()

        assertEquals("45s", result)
    }

    @Test
    fun formattedDuration_withMinutesOnly_returnsMinutesString() {
        val durationMillis = 60000L * 30

        val result = durationMillis.toFormattedDuration()

        assertEquals("30m", result)
    }

    @Test
    fun formattedDuration_withHoursOnly_returnsHoursString() {
        val durationMillis = 3600000L * 5

        val result = durationMillis.toFormattedDuration()

        assertEquals("5h", result)
    }

    @Test
    fun formattedDuration_withHoursAndMinutes_returnsHoursAndMinutesString() {
        val durationMillis = 3600000L * 7 + 60000L * 25

        val result = durationMillis.toFormattedDuration()

        assertEquals("7h 25m", result)
    }

    @Test
    fun formattedTime_withZeroMillis_returnsPlaceholder() {
        val timeMillis = 0L

        val result = timeMillis.toFormattedTime(ZoneId.of("UTC"))

        assertEquals("--:--", result)
    }

    @Test
    fun formattedTime_withValidTimestamp_returnsFormattedTimeString() {
        val timeMillis = 1773748800000L

        val result = timeMillis.toFormattedTime(ZoneId.of("UTC"))

        assertEquals("12:00", result)
    }

    @Test
    fun formattedTime_withEveningTimestamp_returns24HourFormat() {
        val timeMillis = 1773748800000L + (10 * 3600 * 1000L) + (45 * 60 * 1000L)

        val result = timeMillis.toFormattedTime(ZoneId.of("UTC"))

        assertEquals("22:45", result)
    }

    @Test
    fun formatTimeDisplay_withEveningHour_returns24HourFormat() {
        val hour = 22
        val minute = 30

        val result = formatTimeDisplay(hour, minute)

        assertEquals("22:30", result)
    }

    @Test
    fun formatTimeDisplay_withSingleDigitHourAndMinute_padsZeroes() {
        val hour = 7
        val minute = 5

        val result = formatTimeDisplay(hour, minute)

        assertEquals("07:05", result)
    }

    @Test
    fun formatTimeDisplay_withMidnight_returnsZeroes() {
        val hour = 0
        val minute = 0

        val result = formatTimeDisplay(hour, minute)

        assertEquals("00:00", result)
    }

    @Test
    fun formattedDateTime_withZeroMillis_returnsEmptyString() {
        val dateMillis = 0L

        val result = dateMillis.toFormattedDateTime(ZoneId.of("UTC"))

        assertEquals("", result)
    }

    @Test
    fun formattedDateTime_withValidTimestamp_returns24HourFormat() {
        val timeMillis = 1773748800000L + (8 * 3600 * 1000L) + (15 * 60 * 1000L)

        val result = timeMillis.toFormattedDateTime(ZoneId.of("UTC"))

        assertTrue(result.endsWith("20:15"))
    }

    @Test
    fun formattedDate_withZeroMillis_returnsEmptyString() {
        val dateMillis = 0L

        val result = dateMillis.toFormattedDate(ZoneId.of("UTC"))

        assertEquals("", result)
    }

    @Test
    fun formattedDate_withValidTimestamp_returnsFormattedDateString() {
        val dateMillis = 1773748800000L

        val result = dateMillis.toFormattedDate(ZoneId.of("UTC"))

        assertTrue(result.isNotEmpty())
    }

    @Test
    fun padTwoDigits_withSingleDigit_addsLeadingZero() {
        val number = 7

        val result = number.padTwoDigits()

        assertEquals("07", result)
    }

    @Test
    fun padTwoDigits_withDoubleDigit_preservesNumber() {
        val number = 42

        val result = number.padTwoDigits()

        assertEquals("42", result)
    }

    @Test
    fun calculateNextAlarmTime_whenScheduledTimeIsLaterToday_returnsSameDay() {
        val baseNowMillis = 1773738000000L
        val targetHour = 23
        val targetMinute = 0

        val result = calculateNextAlarmTime(targetHour, targetMinute, baseNowMillis)

        assertTrue(result > baseNowMillis)
    }

    @Test
    fun calculateNextAlarmTime_whenScheduledTimeHasPassedToday_returnsNextDay() {
        val baseNowMillis = 1773788400000L
        val targetHour = 1
        val targetMinute = 0

        val result = calculateNextAlarmTime(targetHour, targetMinute, baseNowMillis)

        assertTrue(result > baseNowMillis)
    }
}
