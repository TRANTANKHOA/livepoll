package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.PollEntity
import com.example.data.model.AuthProvider
import com.example.data.model.PollTemplate
import com.example.data.model.PollTemplates
import com.example.data.model.PollWithDetails
import com.example.ui.components.AuthDialog
import com.example.ui.components.GoogleLogoIcon
import com.example.ui.components.FacebookLogoIcon
import com.example.ui.components.AppleLogoIcon
import com.example.ui.components.JoinCodeDialog
import com.example.ui.components.PollCard
import com.example.ui.components.ProviderBadge
import com.example.ui.components.SharePollDialog
import com.example.ui.components.UserSwitcherDialog
import com.example.ui.components.WebAccessDialog
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.StatusActive
import com.example.ui.theme.TertiaryLight
import com.example.ui.viewmodel.PollViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollListScreen(
    viewModel: PollViewModel,
    onCreatePollClick: (PollTemplate?) -> Unit,
    onVotePollClick: (String) -> Unit,
    onAnalyticsPollClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredPolls by viewModel.filteredPolls.collectAsState()
    val allPolls by viewModel.allPolls.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentVoterName by viewModel.currentVoterName.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showUserSwitcher by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showWebAccessDialog by remember { mutableStateOf(false) }
    var sharingPollDetails by remember { mutableStateOf<PollWithDetails?>(null) }

    // Dialogs
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

    if (showJoinDialog) {
        JoinCodeDialog(
            viewModel = viewModel,
            onPollFound = { pollId ->
                onVotePollClick(pollId)
            },
            onDismiss = { showJoinDialog = false }
        )
    }

    if (showWebAccessDialog) {
        WebAccessDialog(
            onDismiss = { showWebAccessDialog = false }
        )
    }

    sharingPollDetails?.let { details ->
        SharePollDialog(
            pollDetails = details,
            onDismiss = { sharingPollDetails = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(PrimaryLight, SecondaryLight)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⚡", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                text = "PulsePoll",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Social Polls & Real-Time Analytics",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Web Browser Access Info Button
                    IconButton(
                        onClick = { showWebAccessDialog = true },
                        modifier = Modifier.testTag("web_access_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Chrome Web Access Info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Profile / Social SSO Switcher Pill
                    Surface(
                        onClick = { showUserSwitcher = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("user_switcher_pill")
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

                    // Join by code icon
                    IconButton(
                        onClick = { showJoinDialog = true },
                        modifier = Modifier.testTag("join_code_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Join Poll with Code",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Notifications bell with badge
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier.testTag("notifications_top_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(containerColor = SecondaryLight) {
                                        Text("$unreadCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onCreatePollClick(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_poll_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Text("New Poll", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Stats Banner
            item {
                StatsOverviewHeader(allPolls = allPolls)
            }

            // Multi-Device & Browser Access Guide Banner
            item {
                Surface(
                    onClick = { showWebAccessDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("web_access_banner_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📱 Multi-User Testing & Join Code Guide",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "How to test multi-person votes, share codes & sync results",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Guide ›",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Social Sign-in (Google / Facebook / Apple) SSO Banner
            item {
                Surface(
                    onClick = { showAuthDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("social_auth_home_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            GoogleLogoIcon(size = 18.dp)
                            FacebookLogoIcon(size = 18.dp)
                            AppleLogoIcon(size = 18.dp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentUser.provider == AuthProvider.GUEST) "Sign in with Google, Facebook or Apple" else "Account: ${currentUser.provider.displayName} (${currentUser.name})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentUser.provider == AuthProvider.GUEST) "Link your identity to sync votes & host polls" else "${currentUser.email ?: "Connected"} • Tap to switch or manage SSO",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (currentUser.provider == AuthProvider.GUEST) "Sign In ›" else "Manage ›",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 2. 1-Tap Ready Templates Carousel
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🚀 Quick Templates (1-Tap Setup)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(PollTemplates.templates) { template ->
                            TemplateStarterCard(
                                template = template,
                                onClick = { onCreatePollClick(template) }
                            )
                        }
                    }
                }
            }

            // 3. Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search polls, venues, codes, creators...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("poll_search_input")
                )
            }

            // 4. Category Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All Polls",
                        "ACTIVE" to "⚡ Active",
                        "SOCCER" to "⚽ Soccer Games",
                        "DRINKS" to "🍻 Drinks & Bars",
                        "FOOD" to "🍕 Food & Lunch",
                        "EVENT" to "🎉 Events & RSVP",
                        "CLOSED" to "🔒 Closed"
                    )

                    filters.forEach { (key, label) ->
                        FilterChip(
                            selected = activeFilter == key,
                            onClick = { viewModel.activeFilter.value = key },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (activeFilter == key) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_chip_$key")
                        )
                    }
                }
            }

            // 5. Polls Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Group Polls & Surveys (${filteredPolls.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // 6. Poll Cards or Empty State
            if (filteredPolls.isEmpty()) {
                item {
                    EmptyPollState(
                        searchQuery = searchQuery,
                        onCreateClick = { onCreatePollClick(null) }
                    )
                }
            } else {
                items(filteredPolls, key = { it.id }) { poll ->
                    PollCard(
                        poll = poll,
                        viewModel = viewModel,
                        onVoteClick = { onVotePollClick(poll.id) },
                        onAnalyticsClick = { onAnalyticsPollClick(poll.id) },
                        onShareClick = {
                            // Fetch details for sharing
                            val flow = viewModel.getPollDetails(poll.id)
                            coroutineScope.launch {
                                flow.firstOrNull()?.let { details ->
                                    sharingPollDetails = details
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsOverviewHeader(allPolls: List<PollEntity>) {
    val now = System.currentTimeMillis()
    val activeCount = allPolls.count { !it.isClosed && (it.deadlineTimestamp == null || now <= it.deadlineTimestamp) }
    val deadlinesCount = allPolls.count { !it.isClosed && it.deadlineTimestamp != null && now <= it.deadlineTimestamp && (it.deadlineTimestamp - now) <= 12 * 3600 * 1000L }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatMetricItem(
                label = "Active Polls",
                value = "$activeCount",
                icon = "⚡",
                color = StatusActive
            )
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            StatMetricItem(
                label = "Closing Today",
                value = "$deadlinesCount",
                icon = "⏰",
                color = SecondaryLight
            )
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            StatMetricItem(
                label = "Total Polls",
                value = "${allPolls.size}",
                icon = "📊",
                color = TertiaryLight
            )
        }
    }
}

@Composable
private fun StatMetricItem(
    label: String,
    value: String,
    icon: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 13.sp)
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TemplateStarterCard(
    template: PollTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() }
            .testTag("template_card_${template.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = template.categoryIcon, fontSize = 20.sp)
                Text(
                    text = template.title.substringAfter(" "),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = template.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${template.defaultOptions.size} options ready",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Use ➔",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyPollState(
    searchQuery: String,
    onCreateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🗳️",
                fontSize = 44.sp
            )
            Text(
                text = if (searchQuery.isNotBlank()) "No polls found matching '$searchQuery'" else "No polls in this category yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create a new poll for your group soccer match, social drinks, or team lunch decisions!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            androidx.compose.material3.Button(
                onClick = onCreateClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create New Poll")
            }
        }
    }
}
