package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PollOptionEntity
import com.example.ui.components.AddCustomOptionDialog
import com.example.ui.components.AuthDialog
import com.example.ui.components.DeadlineCountdownBadge
import com.example.ui.components.ProviderBadge
import com.example.ui.components.UserSwitcherDialog
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusEndingSoon
import com.example.ui.theme.TopWinnerGold
import com.example.ui.viewmodel.PollViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(
    pollId: String,
    viewModel: PollViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAnalytics: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pollDetails by viewModel.getPollDetails(pollId).collectAsState(initial = null)
    val currentUser by viewModel.currentUser.collectAsState()
    val currentVoterName by viewModel.currentVoterName.collectAsState()
    val cloudStatus by viewModel.cloudSyncStatus.collectAsState()

    androidx.compose.runtime.LaunchedEffect(pollId) {
        viewModel.attachPollCloudListener(pollId)
    }

    var showUserSwitcher by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showAddCustomOption by remember { mutableStateOf(false) }

    if (showUserSwitcher) {
        UserSwitcherDialog(
            viewModel = viewModel,
            onOpenAuthDialog = { showAuthDialog = true },
            onDismiss = { showUserSwitcher = false }
        )
    }

    if (showAuthDialog) {
        AuthDialog(
            viewModel = viewModel,
            onDismiss = { showAuthDialog = false }
        )
    }

    if (showAddCustomOption) {
        AddCustomOptionDialog(
            pollId = pollId,
            viewModel = viewModel,
            onDismiss = { showAddCustomOption = false }
        )
    }

    val details = pollDetails
    if (details == null) {
        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading questionnaire details...")
            }
        }
        return
    }

    val poll = details.poll
    val options = details.options
    val userExistingVotes = details.userVotes

    val selectedOptionIds = remember(userExistingVotes) {
        mutableStateListOf<String>().apply {
            addAll(userExistingVotes.map { it.optionId })
        }
    }

    var rsvpStatus by remember(userExistingVotes) {
        mutableStateOf(userExistingVotes.firstOrNull()?.rsvpStatus ?: "GOING")
    }

    var plusGuests by remember(userExistingVotes) {
        mutableIntStateOf(userExistingVotes.firstOrNull()?.plusGuests ?: 0)
    }

    var starRating by remember(userExistingVotes) {
        mutableStateOf<Int?>(userExistingVotes.firstOrNull()?.ratingValue)
    }

    var feedbackComment by remember(userExistingVotes) {
        mutableStateOf(userExistingVotes.firstOrNull()?.feedbackComment ?: "")
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cast Your Vote",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("voting_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Profile Switcher Pill
                    Surface(
                        onClick = { showUserSwitcher = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("voting_user_profile_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(text = currentUser.avatarEmoji, fontSize = 14.sp)
                            ProviderBadge(provider = currentUser.provider, size = 14.dp)
                            Text(
                                text = currentUser.name.split(" ").firstOrNull() ?: currentVoterName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
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
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = poll.categoryIcon, fontSize = 22.sp)
                            }
                            Column {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = poll.code,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                                if (!poll.groupName.isNullOrBlank()) {
                                    Text(
                                        text = poll.groupName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        DeadlineCountdownBadge(
                            deadlineTimestamp = poll.deadlineTimestamp,
                            isClosed = poll.isClosed
                        )
                    }

                    Text(
                        text = poll.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (poll.description.isNotBlank()) {
                        Text(
                            text = poll.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Created by ${poll.creatorName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = "•", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = if (poll.allowMultipleChoices) "Multi-selection allowed" else "Single choice only",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Options Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PREFERRED CHOICE(S)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (poll.allowCustomOptions && !poll.isClosed && !details.isExpired) {
                        OutlinedButton(
                            onClick = { showAddCustomOption = true },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("suggest_venue_option_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suggest Option", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                options.forEach { option ->
                    val isSelected = selectedOptionIds.contains(option.id)
                    VotingOptionCard(
                        option = option,
                        isSelected = isSelected,
                        isMultiChoice = poll.allowMultipleChoices,
                        onSelect = {
                            if (poll.allowMultipleChoices) {
                                if (isSelected) {
                                    selectedOptionIds.remove(option.id)
                                } else {
                                    selectedOptionIds.add(option.id)
                                }
                            } else {
                                selectedOptionIds.clear()
                                selectedOptionIds.add(option.id)
                            }
                            validationError = null
                        }
                    )
                }
            }

            // Attendance & RSVP Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "RSVP & HEADCOUNT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RsvpChoiceChip(
                            label = "Going",
                            icon = Icons.Default.CheckCircle,
                            color = MaterialTheme.colorScheme.primary,
                            isSelected = rsvpStatus == "GOING",
                            onClick = { rsvpStatus = "GOING" },
                            modifier = Modifier.weight(1f)
                        )
                        RsvpChoiceChip(
                            label = "Maybe",
                            icon = Icons.Default.Help,
                            color = StatusEndingSoon,
                            isSelected = rsvpStatus == "MAYBE",
                            onClick = { rsvpStatus = "MAYBE" },
                            modifier = Modifier.weight(1f)
                        )
                        RsvpChoiceChip(
                            label = "Can't Go",
                            icon = Icons.Default.Cancel,
                            color = Color(0xFFB3261E),
                            isSelected = rsvpStatus == "CANT_GO",
                            onClick = { rsvpStatus = "CANT_GO" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // +1 Guests Stepper (Only if going or maybe)
                    if (rsvpStatus == "GOING" || rsvpStatus == "MAYBE") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Bringing Guests / +1s?",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Adds to group headcount",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0, 1, 2, 3).forEach { count ->
                                    val isCountSelected = plusGuests == count
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCountSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .clickable { plusGuests = count },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (count == 0) "0" else "+$count",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCountSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Star Rating (Optional)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PREFERENCE RATING (OPTIONAL)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            val isFilled = starRating != null && star <= starRating!!
                            IconButton(onClick = { starRating = star }) {
                                Icon(
                                    imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star Stars",
                                    tint = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Comments / Suggestions TextField
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "NOTES / DIETARY / SUGGESTIONS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = feedbackComment,
                        onValueChange = { feedbackComment = it },
                        placeholder = { Text("e.g. Bringing match ball / gluten-free / running 10m late") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("vote_feedback_input"),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Action Buttons: Submit Vote & View Analytics
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (selectedOptionIds.isEmpty() && rsvpStatus == "GOING") {
                            validationError = "Please select at least one preferred option before voting"
                            return@Button
                        }

                        viewModel.castVote(
                            pollId = poll.id,
                            selectedOptionIds = selectedOptionIds.ifEmpty {
                                options.take(1).map { it.id }
                            },
                            rsvpStatus = rsvpStatus,
                            plusGuests = plusGuests,
                            rating = starRating,
                            comment = feedbackComment,
                            context = context,
                            pollTitle = poll.title
                        )

                        onNavigateToAnalytics(poll.id)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_vote_button"),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (details.hasUserVoted) "Update My Response" else "Submit Vote & View Results",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                OutlinedButton(
                    onClick = { onNavigateToAnalytics(poll.id) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("view_results_button"),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(imageVector = Icons.Default.BarChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Skip to Visual Analytics & Results", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun VotingOptionCard(
    option: PollOptionEntity,
    isSelected: Boolean,
    isMultiChoice: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(18.dp)
                    )
                } else {
                    Modifier
                }
            )
            .testTag("option_card_${option.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isMultiChoice) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() }
                )
            } else {
                RadioButton(
                    selected = isSelected,
                    onClick = { onSelect() }
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = option.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!option.subtitle.isNullOrBlank()) {
                    Text(
                        text = option.subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!option.dateTimeSlot.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = option.dateTimeSlot,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (!option.duration.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⏱️ ${option.duration}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!option.venueAddress.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SecondaryLight,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = option.venueAddress,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (option.priceRating != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = option.priceRating,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RsvpChoiceChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                if (isSelected) 1.5.dp else 0.dp,
                if (isSelected) color else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
