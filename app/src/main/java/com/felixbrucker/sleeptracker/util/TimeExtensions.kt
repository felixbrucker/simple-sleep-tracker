package com.felixbrucker.sleeptracker.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

/**
 * Fluent extension functions for time calculations and formatting.
 */

fun Long.toFormattedDuration(): String {
    if (this <= 0L) return "0m"
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

// Thread-safe cached formatters to prevent repeated allocations in hot rendering paths.
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())

fun Long.toFormattedTime(zoneId: ZoneId = ZoneId.systemDefault()): String {
    if (this <= 0L) return "--:--"
    val instant = Instant.ofEpochMilli(this)
    return instant.atZone(zoneId).format(timeFormatter)
}

fun Long.toFormattedDate(zoneId: ZoneId = ZoneId.systemDefault()): String {
    if (this <= 0L) return ""
    val instant = Instant.ofEpochMilli(this)
    return instant.atZone(zoneId).format(dateFormatter)
}

fun Long.toFormattedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): String {
    if (this <= 0L) return ""
    val instant = Instant.ofEpochMilli(this)
    return instant.atZone(zoneId).format(dateTimeFormatter)
}

fun Int.padTwoDigits(): String = String.format(Locale.getDefault(), "%02d", this)

fun formatTimeDisplay(hour: Int, minute: Int): String {
    val localDateTime = LocalDateTime.of(2026, 1, 1, hour, minute)
    return localDateTime.format(timeFormatter)
}

fun calculateNextAlarmTime(
    hour: Int,
    minute: Int,
    currentTimeMillis: Long = System.currentTimeMillis()
): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = currentTimeMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (calendar.timeInMillis <= currentTimeMillis) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    return calendar.timeInMillis
}
