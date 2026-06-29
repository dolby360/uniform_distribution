package com.uniformdist.app.ui.screens.itemslist

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkWornDialog(
    isLogging: Boolean,
    onConfirm: (wornAtIso: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
            // Don't allow future dates — you can't have worn something tomorrow.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            showDatePicker = false
                            onConfirm(isoForPickedDate(millis))
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null && !isLogging,
                ) {
                    Text("Log wear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }, enabled = !isLogging) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
        return
    }

    AlertDialog(
        onDismissRequest = { if (!isLogging) onDismiss() },
        title = { Text("When did you wear this?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Pick a day to log a wear event.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onConfirm(null) },
                    enabled = !isLogging,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isLogging) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Today")
                    }
                }
                FilledTonalButton(
                    onClick = { onConfirm(isoForDaysAgo(1)) },
                    enabled = !isLogging,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Yesterday")
                }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    enabled = !isLogging,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Pick a date…")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLogging) {
                Text("Cancel")
            }
        },
    )
}

private fun isoForDaysAgo(days: Long): String {
    // Anchor at noon local time so it's unambiguously "that day" everywhere
    // and won't drift into a neighboring day under timezone math.
    val zone = ZoneId.systemDefault()
    val date = LocalDate.now(zone).minusDays(days)
    val instant = date.atTime(OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC)).toInstant()
    return instant.toString()
}

private fun isoForPickedDate(utcMillis: Long): String {
    // The DatePicker returns midnight UTC of the selected calendar date.
    // Bump to noon UTC for the same day-stability reason as above.
    val date = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val instant = date.atTime(OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC)).toInstant()
    return instant.toString()
}
