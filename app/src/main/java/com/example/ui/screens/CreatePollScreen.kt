package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PollOptionEntity
import com.example.data.model.PollTemplate
import com.example.ui.theme.SecondaryLight
import com.example.ui.viewmodel.PollViewModel
import java.util.UUID

data class EditableOption(
    var id: String = UUID.randomUUID().toString(),
    var text: String = "",
    var venueAddress: String = "",
    var dateTimeSlot: String = "",
    var duration: String = "90 mins",
    var subtitle: String = "",
    var priceRating: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePollScreen(
    viewModel: PollViewModel,
    initialTemplate: PollTemplate?,
    onNavigateBack: () -> Unit,
    onPollCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentVoterName = viewModel.currentVoterName.value
    val allGroups by viewModel.allGroups.collectAsState()

    var title by remember { mutableStateOf(initialTemplate?.title ?: "") }
    var category by remember { mutableStateOf(initialTemplate?.category ?: "SOCCER") }
    var categoryIcon by remember { mutableStateOf(initialTemplate?.categoryIcon ?: "⚽") }
    var description by remember { mutableStateOf(initialTemplate?.description ?: "") }
    var creatorName by remember { mutableStateOf(currentVoterName) }

    var selectedGroupId by remember { mutableStateOf<String?>(initialTemplate?.defaultGroupId ?: "group_soccer") }
    var selectedGroupName by remember { mutableStateOf<String?>(initialTemplate?.defaultGroupName ?: "Weekend Soccer Squad ⚽") }
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    var allowMultiple by remember { mutableStateOf(initialTemplate?.allowMultipleChoices ?: true) }
    var allowCustom by remember { mutableStateOf(initialTemplate?.allowCustomOptions ?: true) }
    var isAnonymous by remember { mutableStateOf(initialTemplate?.isAnonymous ?: false) }
    var deadlineHours by remember { mutableStateOf(initialTemplate?.defaultDeadlineHours ?: 6) }
    var targetHeadcountText by remember { mutableStateOf(initialTemplate?.defaultTargetHeadcount?.toString() ?: "10") }
    var generalLocation by remember { mutableStateOf("") }

    val optionsList = remember {
        mutableStateListOf<EditableOption>().apply {
            if (initialTemplate != null) {
                initialTemplate.defaultOptions.forEach { opt ->
                    add(
                        EditableOption(
                            text = opt.text,
                            venueAddress = opt.venueAddress ?: "",
                            dateTimeSlot = opt.dateTimeSlot ?: "",
                            duration = opt.duration ?: "90 mins",
                            subtitle = opt.subtitle ?: "",
                            priceRating = opt.priceRating ?: ""
                        )
                    )
                }
            } else {
                add(
                    EditableOption(
                        text = "Riverside Turf Arena (Pitch 2)",
                        venueAddress = "240 River Road Field, South Park",
                        dateTimeSlot = "Saturday 4:00 PM - 5:30 PM",
                        duration = "90 mins",
                        subtitle = "Turf grass, floodlights",
                        priceRating = "$12 / player"
                    )
                )
                add(
                    EditableOption(
                        text = "Central Park Community Pitch",
                        venueAddress = "Central Park Field 4",
                        dateTimeSlot = "Saturday 5:30 PM - 7:00 PM",
                        duration = "90 mins",
                        subtitle = "Natural grass, public access",
                        priceRating = "Free"
                    )
                )
            }
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categoryPresets = listOf(
        Triple("SOCCER", "⚽", "Soccer Game"),
        Triple("DRINKS", "🍻", "Social Drinks"),
        Triple("FOOD", "🍕", "Team Lunch"),
        Triple("EVENT", "🎉", "Group Outing"),
        Triple("DECISION", "🎯", "Quick Vote"),
        Triple("FEEDBACK", "⭐", "Feedback")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialTemplate != null) "Create from Template" else "Create New Poll",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("create_poll_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step 1: Category Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Category & Type",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryPresets.forEach { (catKey, emoji, label) ->
                        val isSelected = category == catKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    category = catKey
                                    categoryIcon = emoji
                                    if (title.isBlank()) {
                                        title = "$emoji "
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = emoji, fontSize = 16.sp)
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Step 2: Target Group Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "2. Target Group (Sends Push Notification to Members)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = groupDropdownExpanded,
                        onExpandedChange = { groupDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedGroupName ?: "None (Public Poll)",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("group_selector_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = groupDropdownExpanded,
                            onDismissRequest = { groupDropdownExpanded = false }
                        ) {
                            allGroups.forEach { group ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = group.icon, fontSize = 16.sp)
                                            Text(text = group.name, fontWeight = FontWeight.SemiBold)
                                        }
                                    },
                                    onClick = {
                                        selectedGroupId = group.id
                                        selectedGroupName = group.name
                                        groupDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("🌐 Public / All Friends", fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedGroupId = null
                                    selectedGroupName = null
                                    groupDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Step 3: Poll Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3. Poll Details",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            errorMessage = null
                        },
                        label = { Text("Poll Question / Event Title *") },
                        placeholder = { Text("e.g. Saturday 5v5 Soccer: Pitch Venue & Kickoff Time") },
                        modifier = Modifier.fillMaxWidth().testTag("poll_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Notes (Optional)") },
                        placeholder = { Text("e.g. Vote on preferred venue, pitch and kickoff time!") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("poll_description_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = creatorName,
                            onValueChange = { creatorName = it },
                            label = { Text("Organizer Name") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = targetHeadcountText,
                            onValueChange = { targetHeadcountText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Target Quorum") },
                            placeholder = { Text("e.g. 10") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Step 4: Voting Options with Required Attributes: Venue, Time Slot, and Duration!
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "4. Voting Options (${optionsList.size})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Attributes: Venue 📍, Time 🕒, and Duration ⏱️",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                optionsList.add(
                                    EditableOption(
                                        text = "Option ${optionsList.size + 1}",
                                        venueAddress = "Venue Address",
                                        dateTimeSlot = "Saturday 5:00 PM",
                                        duration = "90 mins"
                                    )
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Option", fontSize = 12.sp)
                        }
                    }

                    optionsList.forEachIndexed { index, option ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Option #${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (optionsList.size > 2) {
                                    IconButton(
                                        onClick = { optionsList.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete option",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // 1. Option Name / Pitch
                            OutlinedTextField(
                                value = option.text,
                                onValueChange = {
                                    optionsList[index] = option.copy(text = it)
                                    errorMessage = null
                                },
                                label = { Text("Option Title / Venue Name *") },
                                placeholder = { Text("e.g. Riverside Turf Arena (Pitch 2)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("option_text_input_$index")
                            )

                            // 2. Venue Address (Required)
                            OutlinedTextField(
                                value = option.venueAddress,
                                onValueChange = {
                                    optionsList[index] = option.copy(venueAddress = it)
                                    errorMessage = null
                                },
                                label = { Text("Venue Location / Address *") },
                                placeholder = { Text("e.g. 240 River Road Field, South Park") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = SecondaryLight, modifier = Modifier.size(16.dp))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("option_venue_input_$index")
                            )

                            // 3. Date & Time Slot (Required)
                            OutlinedTextField(
                                value = option.dateTimeSlot,
                                onValueChange = {
                                    optionsList[index] = option.copy(dateTimeSlot = it)
                                    errorMessage = null
                                },
                                label = { Text("Date & Kickoff / Start Time *") },
                                placeholder = { Text("e.g. Saturday 4:00 PM - 5:30 PM") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("option_time_input_$index")
                            )

                            // 4. Duration (Required) with convenient preset selector chips
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "Duration *",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                val durationPresets = listOf("45 mins", "1 hour", "90 mins", "2 hours", "3 hours", "All Day")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    durationPresets.forEach { preset ->
                                        val isSelected = option.duration == preset
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { optionsList[index] = option.copy(duration = preset) },
                                            label = { Text(preset, fontSize = 11.sp) },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = option.duration,
                                    onValueChange = {
                                        optionsList[index] = option.copy(duration = it)
                                        errorMessage = null
                                    },
                                    label = { Text("Custom Duration") },
                                    placeholder = { Text("e.g. 90 mins / 2.5 hours") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("option_duration_input_$index")
                                )
                            }
                        }
                    }
                }
            }

            // Step 5: Voting Deadlines & Push Alerts
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "5. Voting Deadline & Notifications",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Text(
                        text = "Participants will receive push reminders before this deadline approaches:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val deadlineOptions = listOf(
                        1 to "1 Hour",
                        2 to "2 Hours",
                        3 to "3 Hours",
                        6 to "6 Hours",
                        12 to "12 Hours",
                        24 to "24 Hours (1 Day)",
                        0 to "No Deadline"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        deadlineOptions.forEach { (hours, label) ->
                            val isSelected = deadlineHours == hours
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { deadlineHours = hours }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Allow Multiple Choices", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = "Voters can select more than 1 venue/time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = allowMultiple,
                            onCheckedChange = { allowMultiple = it },
                            modifier = Modifier.testTag("allow_multiple_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Allow Participant Suggestions", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = "Group members can propose new venue/time options", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = allowCustom,
                            onCheckedChange = { allowCustom = it }
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Publish Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Please enter a poll title"
                        return@Button
                    }
                    val validOptions = optionsList.filter { it.text.isNotBlank() }
                    if (validOptions.size < 2) {
                        errorMessage = "Please provide at least 2 voting options"
                        return@Button
                    }

                    // Validate required attributes for each option (Venue, Time, Duration)
                    for (i in validOptions.indices) {
                        val opt = validOptions[i]
                        if (opt.venueAddress.isBlank()) {
                            errorMessage = "Option #${i + 1} (${opt.text}) requires a Venue Location / Address"
                            return@Button
                        }
                        if (opt.dateTimeSlot.isBlank()) {
                            errorMessage = "Option #${i + 1} (${opt.text}) requires a Date & Time Slot"
                            return@Button
                        }
                        if (opt.duration.isBlank()) {
                            errorMessage = "Option #${i + 1} (${opt.text}) requires a Duration"
                            return@Button
                        }
                    }

                    val entityOptions = validOptions.mapIndexed { idx, opt ->
                        PollOptionEntity(
                            id = opt.id,
                            pollId = "",
                            text = opt.text.trim(),
                            subtitle = opt.subtitle.trim().takeIf { it.isNotBlank() },
                            dateTimeSlot = opt.dateTimeSlot.trim(),
                            venueAddress = opt.venueAddress.trim(),
                            duration = opt.duration.trim(),
                            priceRating = opt.priceRating.trim().takeIf { it.isNotBlank() },
                            displayOrder = idx + 1
                        )
                    }

                    val targetHeadcount = targetHeadcountText.toIntOrNull()

                    viewModel.createPoll(
                        title = title,
                        category = category,
                        categoryIcon = categoryIcon,
                        description = description,
                        creatorName = creatorName,
                        options = entityOptions,
                        deadlineHours = if (deadlineHours > 0) deadlineHours else null,
                        allowMultiple = allowMultiple,
                        allowCustom = allowCustom,
                        isAnonymous = isAnonymous,
                        targetHeadcount = targetHeadcount,
                        location = generalLocation,
                        groupId = selectedGroupId,
                        groupName = selectedGroupName,
                        context = context,
                        onCreated = { pollId ->
                            onPollCreated(pollId)
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_poll_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish & Alert Group", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
