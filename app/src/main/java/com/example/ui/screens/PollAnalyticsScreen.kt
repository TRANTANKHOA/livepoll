package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeedbackItem
import com.example.data.model.PollWithDetails
import com.example.ui.components.AnalyticsBarOptionCard
import com.example.ui.components.DataExportDialog
import com.example.ui.components.DeadlineCountdownBadge
import com.example.ui.components.HeadcountSummaryCard
import com.example.ui.components.SharePollDialog
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.StatusActive
import com.example.ui.theme.TopWinnerGold
import com.example.ui.viewmodel.PollViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollAnalyticsScreen(
    pollId: String,
    viewModel: PollViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pollDetails by viewModel.getPollDetails(pollId).collectAsState(initial = null)

    var selectedTab by remember { mutableIntStateOf(0) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val details = pollDetails

    if (showShareDialog && details != null) {
        SharePollDialog(
            pollDetails = details,
            onDismiss = { showShareDialog = false }
        )
    }

    if (showExportDialog && details != null) {
        DataExportDialog(
            pollDetails = details,
            onDismiss = { showExportDialog = false }
        )
    }

    if (details == null) {
        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading real-time analytics...")
            }
        }
        return
    }

    val poll = details.poll
    val winningOption = details.winningOption

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Poll Results & Analytics",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${details.totalVotes} votes from ${details.uniqueVotersCount} participants",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("analytics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Export CSV/Report
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("export_data_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export Data",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Share
                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.testTag("share_poll_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary
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
        ) {
            // Tabs Row: 1. Visual Charts, 2. RSVP & Roster, 3. Feedback / Notes
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("📊 Visual Charts", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_visual_charts")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("👥 RSVP (${details.headcountSummary.totalAttendingHeadcount})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_rsvp_roster")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("💬 Notes (${details.feedbackList.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_feedback_notes")
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Poll Banner Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Text(
                                        text = poll.category.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Code: ${poll.code}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                    }
                }

                // Winner Spotlight Banner (if votes exist)
                if (winningOption != null && details.totalVotes > 0) {
                    WinnerSpotlightCard(
                        winningOptionText = winningOption.text,
                        winningVoteCount = details.getOptionVoteCount(winningOption.id),
                        winningPercent = details.getOptionVotePercent(winningOption.id),
                        timeSlot = winningOption.dateTimeSlot,
                        location = winningOption.venueAddress
                    )
                }

                // TAB 1: Visual Charts & Analytics
                if (selectedTab == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "VOTE DISTRIBUTION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        details.options.forEach { option ->
                            val voteCount = details.getOptionVoteCount(option.id)
                            val percent = details.getOptionVotePercent(option.id)
                            val isLeader = winningOption?.id == option.id && voteCount > 0
                            val voters = details.getOptionVoters(option.id)

                            AnalyticsBarOptionCard(
                                option = option,
                                voteCount = voteCount,
                                votePercent = percent,
                                totalVotes = details.totalVotes,
                                isLeader = isLeader,
                                voters = voters
                            )
                        }

                        // Average Rating Box if available
                        if (details.averageRating != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = String.format(Locale.US, "%.1f / 5.0 Stars", details.averageRating),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Average participant preference rating",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 2: RSVP Headcount & Roster
                if (selectedTab == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        HeadcountSummaryCard(
                            rsvp = details.headcountSummary,
                            targetHeadcount = poll.targetHeadcount
                        )

                        // Participant Roster List
                        Text(
                            text = "PARTICIPANT ROSTER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val votersGrouped = details.votes.groupBy { it.voterId }
                        if (votersGrouped.isEmpty()) {
                            Text(
                                text = "No votes registered yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            votersGrouped.values.forEach { voteList ->
                                val firstVote = voteList.first()
                                val selectedOptions = voteList.mapNotNull { v ->
                                    details.options.find { it.id == v.optionId }?.text
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (firstVote.voterName.isNotBlank()) firstVote.voterName.take(1).uppercase() else "👤",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (poll.isAnonymous) "Anonymous Voter" else firstVote.voterName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                if (firstVote.plusGuests > 0) {
                                                    Text(
                                                        text = "(+${firstVote.plusGuests} guests)",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "Chose: ${selectedOptions.joinToString(", ")}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // RSVP pill
                                        val (rsvpBg, rsvpFg) = when (firstVote.rsvpStatus) {
                                            "GOING" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
                                            "MAYBE" -> Color(0xFFF59E0B).copy(alpha = 0.15f) to Color(0xFFF59E0B)
                                            else -> Color(0xFFB3261E).copy(alpha = 0.15f) to Color(0xFFB3261E)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(rsvpBg)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = firstVote.rsvpStatus,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = rsvpFg
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 3: Comments & Feedback Notes
                if (selectedTab == 2) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "PARTICIPANT NOTES & SUGGESTIONS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (details.feedbackList.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No comments or suggestions submitted yet.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            details.feedbackList.forEach { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.voterName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = dateFormat.format(Date(item.timestamp)),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = "\"${item.comment}\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Poll Controls & Deadline Simulation Toolbar Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "POLL MANAGEMENT & ALERTS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Simulate Push Deadline Alert Button
                        OutlinedButton(
                            onClick = {
                                viewModel.simulateDeadlinePushAlert(details, context)
                                Toast.makeText(context, "⏰ Deadline push alert notification dispatched!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("simulate_deadline_alert_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Deadline Push Alert", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }

                        // Close / Re-open Poll Toggle Button
                        OutlinedButton(
                            onClick = {
                                val nextStatus = !poll.isClosed
                                viewModel.togglePollStatus(poll.id, nextStatus, context, poll.title)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("toggle_poll_closed_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (poll.isClosed) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (poll.isClosed) "Re-open Voting" else "Close Voting Now", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Bottom CTA: Vote or Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNavigateToVote(poll.id) },
                        modifier = Modifier.weight(1f).height(50.dp).testTag("analytics_vote_cta_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.HowToVote, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (details.hasUserVoted) "Edit My Vote" else "Cast Vote", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.weight(1f).height(50.dp).testTag("analytics_share_cta_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Results", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WinnerSpotlightCard(
    winningOptionText: String,
    winningVoteCount: Int,
    winningPercent: Float,
    timeSlot: String?,
    location: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TopWinnerGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Leader",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "🏆 Leading Consensus Choice",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = TopWinnerGold
                    )
                    Text(
                        text = "$winningVoteCount votes (${String.format(Locale.US, "%.0f%%", winningPercent)} of total)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = winningOptionText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!timeSlot.isNullOrBlank() || !location.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!timeSlot.isNullOrBlank()) {
                        Text(
                            text = "⏰ $timeSlot",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!location.isNullOrBlank()) {
                        Text(
                            text = "📍 $location",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
