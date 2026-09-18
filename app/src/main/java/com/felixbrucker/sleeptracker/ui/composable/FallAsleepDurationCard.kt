package com.felixbrucker.sleeptracker.ui.composable

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun FallAsleepDurationCard(
    currentMinutes: Int,
    onMinutesSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInputText by remember { mutableStateOf("") }
    val presets = listOf(0, 5, 10, 15, 20, 30, 45)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fall_asleep_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Time to Fall Asleep",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentMinutes > 0) {
                            "Duration counted after $currentMinutes min"
                        } else {
                            "Counting begins immediately"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Sleep duration starts counting after this duration passes from when you begin tracking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Presets scrollable row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { minutes ->
                    val isSelected = currentMinutes == minutes
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMinutesSelected(minutes) },
                        label = {
                            Text(if (minutes == 0) "Immediate (0m)" else "${minutes}m")
                        },
                        modifier = Modifier.testTag("chip_fall_asleep_${minutes}m")
                    )
                }

                // Custom option chip
                val isCustomSelected = currentMinutes !in presets
                FilterChip(
                    selected = isCustomSelected,
                    onClick = {
                        customInputText = currentMinutes.toString()
                        showCustomDialog = true
                    },
                    label = {
                        Text(if (isCustomSelected) "${currentMinutes}m (Custom)" else "Custom...")
                    },
                    modifier = Modifier.testTag("chip_fall_asleep_custom")
                )
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Time to Fall Asleep") },
            text = {
                Column {
                    Text(
                        text = "Enter the number of minutes (0 - 180):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customInputText,
                        onValueChange = { customInputText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_fall_asleep_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = customInputText.toIntOrNull()?.coerceIn(0, 180) ?: 15
                        onMinutesSelected(parsed)
                        showCustomDialog = false
                    },
                    modifier = Modifier.testTag("confirm_custom_fall_asleep_button")
                ) {
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
