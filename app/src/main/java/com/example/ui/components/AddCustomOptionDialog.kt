package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SecondaryLight
import com.example.ui.viewmodel.PollViewModel

@Composable
fun AddCustomOptionDialog(
    pollId: String,
    viewModel: PollViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var venueAddress by remember { mutableStateOf("") }
    var timeSlot by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("90 mins") }
    var subtitle by remember { mutableStateOf("") }
    var priceRating by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddLocationAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Suggest Venue & Time",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Propose an alternative venue, time slot, and duration for group voting:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        error = null
                    },
                    label = { Text("Option Title / Venue Name *") },
                    placeholder = { Text("e.g. Westside Indoor Arena") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("custom_option_title_input")
                )

                OutlinedTextField(
                    value = venueAddress,
                    onValueChange = {
                        venueAddress = it
                        error = null
                    },
                    label = { Text("Venue Location / Address *") },
                    placeholder = { Text("e.g. 500 West Ave") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = SecondaryLight, modifier = Modifier.size(16.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("custom_option_venue_input")
                )

                OutlinedTextField(
                    value = timeSlot,
                    onValueChange = {
                        timeSlot = it
                        error = null
                    },
                    label = { Text("Date & Kickoff / Start Time *") },
                    placeholder = { Text("e.g. Saturday 6:00 PM - 7:30 PM") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("custom_option_time_input")
                )

                // Duration presets
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Duration *",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val presets = listOf("45 mins", "1 hour", "90 mins", "2 hours", "3 hours", "All Day")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = duration == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { duration = preset },
                                label = { Text(preset, fontSize = 11.sp) },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Notes / Highlights (Optional)") },
                    placeholder = { Text("e.g. Free parking & locker rooms") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        error = "Please enter an option name or venue"
                        return@Button
                    }
                    if (venueAddress.isBlank()) {
                        error = "Please enter the venue address"
                        return@Button
                    }
                    if (timeSlot.isBlank()) {
                        error = "Please enter the date/time slot"
                        return@Button
                    }
                    viewModel.addCustomOption(
                        pollId = pollId,
                        text = title,
                        subtitle = subtitle,
                        dateTime = timeSlot,
                        venueAddress = venueAddress,
                        duration = duration,
                        priceRating = priceRating
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("submit_custom_option_button")
            ) {
                Text("Add Option to Poll")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
