package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PollEntity
import com.example.data.model.AuthProvider
import com.example.data.model.PollTemplate
import com.example.data.model.PollTemplates
import com.example.data.model.PollWithDetails
import com.example.ui.components.AuthDialog
import com.example.ui.components.JoinCodeDialog
import com.example.ui.components.PollCard
import com.example.ui.components.ProviderBadge
import com.example.ui.components.SharePollDialog
import com.example.ui.components.TemplatesBottomSheet
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
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showSearchInput by remember { mutableStateOf(false) }
    var showInsightsExpanded by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var sharingPollDetails by remember { mutableStateOf<PollWithDetails?>(null) }

    // Dialogs & Sheets
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

    if (showTemplatesSheet) {
        TemplatesBottomSheet(
            onTemplateSelect = { template ->
                onCreatePollClick(template)
            },
            onCustomPollClick = {
                onCreatePollClick(null)
            },
            onDismiss = { showTemplatesSheet = false }
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
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
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
                                    text = "Social Polls & Real-Time Voting",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Search Toggle Button
                        IconButton(
                            onClick = {
                                showSearchInput = !showSearchInput
                                if (!showSearchInput) {
                                    viewModel.searchQuery.value = ""
                                }
                            },
                            modifier = Modifier.testTag("search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (showSearchInput) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = "Search Polls",
                                tint = if (showSearchInput || searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

                        // Profile / Social SSO Avatar Pill
                        Surface(
                            onClick = { showUserSwitcher = true },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .testTag("user_switcher_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = currentUser.avatarEmoji, fontSize = 13.sp)
                                ProviderBadge(provider = currentUser.provider, size = 13.dp)
                                Text(
                                    text = currentUser.name.split(" ").firstOrNull() ?: currentVoterName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Overflow Menu (More Actions)
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("home_overflow_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options"
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🚀 Ready Templates") },
                                    leadingIcon = { Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showTemplatesSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🔑 Join with Code") },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showJoinDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📱 Multi-User Guide") },
                                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showWebAccessDialog = true
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("👤 Account & SSO (${currentUser.provider.displayName})") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showAuthDialog = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Expandable Search Bar
                AnimatedVisibility(
                    visible = showSearchInput || searchQuery.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Search polls, venues, codes...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
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
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("poll_search_input")
                        )
                    }
                }
            }
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
            contentPadding = PaddingValues(bottom = 90.dp, top = 6.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Compact Action Bar (Summary Pill & Quick Action Buttons)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Insights Toggle Pill
                    val now = System.currentTimeMillis()
                    val activeCount = allPolls.count { !it.isClosed && (it.deadlineTimestamp == null || now <= it.deadlineTimestamp) }

                    Surface(
                        onClick = { showInsightsExpanded = !showInsightsExpanded },
                        shape = RoundedCornerShape(12.dp),
                        color = if (showInsightsExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (showInsightsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.testTag("insights_toggle_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "⚡", fontSize = 12.sp)
                            Text(
                                text = "$activeCount Active • ${allPolls.size} Polls",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (showInsightsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Quick Action Pills
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Templates Pill
                        Surface(
                            onClick = { showTemplatesSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("quick_templates_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "🚀", fontSize = 12.sp)
                                Text(
                                    text = "Templates",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Join Code Pill
                        Surface(
                            onClick = { showJoinDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("quick_join_code_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Join",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Collapsible Live Stats & Insights Card
            item {
                AnimatedVisibility(
                    visible = showInsightsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    StatsOverviewHeader(
                        allPolls = allPolls,
                        onClose = { showInsightsExpanded = false }
                    )
                }
            }

            // 3. Category Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val now = System.currentTimeMillis()
                    val filters = listOf(
                        "ALL" to ("All (" + allPolls.size + ")"),
                        "ACTIVE" to ("⚡ Active (" + allPolls.count { !it.isClosed && (it.deadlineTimestamp == null || now <= it.deadlineTimestamp) } + ")"),
                        "SOCCER" to "⚽ Soccer",
                        "DRINKS" to "🍻 Drinks",
                        "FOOD" to "🍕 Food",
                        "EVENT" to "🎉 Events",
                        "CLOSED" to "🔒 Closed"
                    )

                    filters.forEach { (key, label) ->
                        FilterChip(
                            selected = activeFilter == key,
                            onClick = { viewModel.activeFilter.value = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (activeFilter == key) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_chip_$key")
                        )
                    }
                }
            }

            // 4. Poll Cards or Empty State (Directly focused and accessible)
            if (filteredPolls.isEmpty()) {
                item {
                    EmptyPollState(
                        searchQuery = searchQuery,
                        onCreateClick = { onCreatePollClick(null) },
                        onOpenTemplates = { showTemplatesSheet = true }
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
private fun StatsOverviewHeader(
    allPolls: List<PollEntity>,
    onClose: () -> Unit
) {
    val now = System.currentTimeMillis()
    val activeCount = allPolls.count { !it.isClosed && (it.deadlineTimestamp == null || now <= it.deadlineTimestamp) }
    val deadlinesCount = allPolls.count { !it.isClosed && it.deadlineTimestamp != null && now <= it.deadlineTimestamp && (it.deadlineTimestamp - now) <= 12 * 3600 * 1000L }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Live Voting Summary",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.ExpandLess, contentDescription = "Collapse", modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        .height(28.dp)
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
                        .height(28.dp)
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
            Text(text = icon, fontSize = 12.sp)
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyPollState(
    searchQuery: String,
    onCreateClick: () -> Unit,
    onOpenTemplates: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "🗳️",
                fontSize = 36.sp
            )
            Text(
                text = if (searchQuery.isNotBlank()) "No polls found matching '$searchQuery'" else "No polls in this category yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create a new poll or pick a pre-configured template to get started instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Button(
                    onClick = onOpenTemplates,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🚀 Use Template", fontSize = 12.sp)
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = onCreateClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom Poll", fontSize = 12.sp)
                }
            }
        }
    }
}
