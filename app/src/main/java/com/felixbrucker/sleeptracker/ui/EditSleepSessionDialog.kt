package com.felixbrucker.sleeptracker.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.felixbrucker.sleeptracker.data.database.entity.SleepSessionEntity
import com.felixbrucker.sleeptracker.util.formatTimeDisplay
import com.felixbrucker.sleeptracker.util.toFormattedDate
import com.felixbrucker.sleeptracker.util.toFormattedDuration
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSleepSessionDialog(
    session: SleepSessionEntity,
    onDismiss: () -> Unit,
    onConfirm: (startTimeMillis: Long, endTimeMillis: Long) -> Unit
) {
    val initialStartCal = remember(session) {
        Calendar.getInstance().apply { timeInMillis = session.startTimeMillis }
    }
    val initialEndCal = remember(session) {
        Calendar.getInstance().apply { timeInMillis = session.endTimeMillis }
    }

    // 0: Editing Start Time, 1: Editing End Time
    var activeTab by remember { mutableIntStateOf(0) }

    var startHour by remember { mutableIntStateOf(initialStartCal.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableIntStateOf(initialStartCal.get(Calendar.MINUTE)) }

    var endHour by remember { mutableIntStateOf(initialEndCal.get(Calendar.HOUR_OF_DAY)) }
    var endMinute by remember { mutableIntStateOf(initialEndCal.get(Calendar.MINUTE)) }

    val initialIsNextDay = remember(session) {
        val startDay = initialStartCal.get(Calendar.DAY_OF_YEAR)
        val endDay = initialEndCal.get(Calendar.DAY_OF_YEAR)
        startDay != endDay || session.endTimeMillis < session.startTimeMillis
    }
    var endsNextDay by remember { mutableStateOf(initialIsNextDay) }

    val startTimePickerState = rememberTimePickerState(
        initialHour = startHour,
        initialMinute = startMinute,
        is24Hour = true
    )
    val endTimePickerState = rememberTimePickerState(
        initialHour = endHour,
        initialMinute = endMinute,
        is24Hour = true
    )

    val selectedStartHour = if (activeTab == 0) startTimePickerState.hour else startHour
    val selectedStartMinute = if (activeTab == 0) startTimePickerState.minute else startMinute

    val selectedEndHour = if (activeTab == 1) endTimePickerState.hour else endHour
    val selectedEndMinute = if (activeTab == 1) endTimePickerState.minute else endMinute

    val calculatedStartMillis = remember(selectedStartHour, selectedStartMinute, session) {
        (initialStartCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, selectedStartHour)
            set(Calendar.MINUTE, selectedStartMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val calculatedEndMillis = remember(selectedEndHour, selectedEndMinute, endsNextDay, session) {
        (initialStartCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, selectedEndHour)
            set(Calendar.MINUTE, selectedEndMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (endsNextDay) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
    }

    val previewDurationMillis = (calculatedEndMillis - calculatedStartMillis).coerceAtLeast(0L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Sleep Record",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = session.startTimeMillis.toFormattedDate(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Selector tabs for Start / End time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Start Time selector card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTab == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (activeTab == 1) {
                                    endHour = endTimePickerState.hour
                                    endMinute = endTimePickerState.minute
                                }
                                activeTab = 0
                            }
                            .border(
                                width = if (activeTab == 0) 2.dp else 0.dp,
                                color = if (activeTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("edit_start_time_tab")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = if (activeTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Start Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activeTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatTimeDisplay(selectedStartHour, selectedStartMinute),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // End Time selector card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTab == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (activeTab == 0) {
                                    startHour = startTimePickerState.hour
                                    startMinute = startTimePickerState.minute
                                }
                                activeTab = 1
                            }
                            .border(
                                width = if (activeTab == 1) 2.dp else 0.dp,
                                color = if (activeTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("edit_end_time_tab")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = if (activeTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "End Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activeTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatTimeDisplay(selectedEndHour, selectedEndMinute),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Overnight toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ends next day (+1 day)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = endsNextDay,
                        onCheckedChange = { endsNextDay = it },
                        modifier = Modifier.testTag("edit_ends_next_day_switch")
                    )
                }

                // Calculated Duration preview
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Duration: ${previewDurationMillis.toFormattedDuration()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Time picker for current active tab
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeTab == 0) {
                        TimePicker(state = startTimePickerState)
                    } else {
                        TimePicker(state = endTimePickerState)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalStartHour = if (activeTab == 0) startTimePickerState.hour else startHour
                    val finalStartMin = if (activeTab == 0) startTimePickerState.minute else startMinute
                    val finalEndHour = if (activeTab == 1) endTimePickerState.hour else endHour
                    val finalEndMin = if (activeTab == 1) endTimePickerState.minute else endMinute

                    val startMillis = (initialStartCal.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, finalStartHour)
                        set(Calendar.MINUTE, finalStartMin)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val endMillis = (initialStartCal.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, finalEndHour)
                        set(Calendar.MINUTE, finalEndMin)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        if (endsNextDay) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }.timeInMillis

                    onConfirm(startMillis, endMillis)
                },
                modifier = Modifier.testTag("save_edit_session_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_edit_session_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
