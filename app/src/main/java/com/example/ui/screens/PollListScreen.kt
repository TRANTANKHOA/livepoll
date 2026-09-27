package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showUserSwitcher by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showSearchInput by remember { mutableStateOf(false) }
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
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                    .clip(MaterialTheme.shapes.small)
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
                                    text = "LivePulse",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Real-Time Group Polls",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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

                        // Profile / Social SSO Avatar Pill (compact: emoji + provider badge)
                        Surface(
                            onClick = { showUserSwitcher = true },
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .testTag("user_switcher_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = currentUser.avatarEmoji, fontSize = 14.sp)
                                ProviderBadge(provider = currentUser.provider, size = 14.dp)
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
                                    text = { Text("Ready Templates") },
                                    leadingIcon = { Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showTemplatesSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Join with Code") },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showJoinDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Account (${currentUser.provider.displayName})") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showAuthDialog = true
                                    }
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
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
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
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
            ExtendedFloatingActionButton(
                onClick = { showTemplatesSheet = true },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("New Poll", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 6.dp
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.testTag("create_poll_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Quick Actions (counts live on the filter chips below)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    AssistChip(
                        onClick = { showJoinDialog = true },
                        label = { Text("Join", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("quick_join_code_button")
                    )
                }
            }

            // 3. Category Filter Chips (derived from actual poll categories)
            if (allPolls.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val now = System.currentTimeMillis()
                        val activeCount = allPolls.count { !it.isClosed && (it.deadlineTimestamp == null || now <= it.deadlineTimestamp) }
                        val closedCount = allPolls.count { it.isClosed || (it.deadlineTimestamp != null && now > it.deadlineTimestamp) }
                        val filters = buildList<Pair<String, String>> {
                            add("ALL" to "All (${allPolls.size})")
                            add("ACTIVE" to "⚡ Active ($activeCount)")
                            allPolls.distinctBy { it.category }.forEach { poll ->
                                val count = allPolls.count { it.category == poll.category }
                                add(poll.category to "${poll.categoryIcon} ${categoryLabel(poll.category)} ($count)")
                            }
                            if (closedCount > 0) add("CLOSED" to "🔒 Closed ($closedCount)")
                        }

                        filters.forEach { (key, label) ->
                            FilterChip(
                                selected = activeFilter == key,
                                onClick = { viewModel.activeFilter.value = key },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (activeFilter == key) FontWeight.Bold else FontWeight.Normal) },
                                shape = MaterialTheme.shapes.large,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("filter_chip_$key")
                            )
                        }
                    }
                }
            }

            // 4. Poll Cards, Template Gallery (first run), or Search Empty State
            if (filteredPolls.isEmpty()) {
                if (searchQuery.isNotBlank()) {
                    item {
                        EmptyPollState(searchQuery = searchQuery)
                    }
                } else {
                    item {
                        TemplateGalleryEmptyState(
                            onTemplateClick = { template ->
                                onCreatePollClick(template)
                            },
                            onBlankClick = { onCreatePollClick(null) }
                        )
                    }
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
private fun EmptyPollState(searchQuery: String) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🔍",
                fontSize = 28.sp
            )
            Text(
                text = "No polls found matching '$searchQuery'",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Try a different search, or clear it to see all polls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun TemplateGalleryEmptyState(
    onTemplateClick: (PollTemplate) -> Unit,
    onBlankClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "🗳️", fontSize = 34.sp)
            Text(
                text = "What are you deciding?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Pick a template, share the code, and watch votes land live.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        PollTemplates.templates.forEach { template ->
            Card(
                onClick = { onTemplateClick(template) },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_template_item_${template.id}")
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
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = template.categoryIcon, fontSize = 22.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = template.title.removePrefix("${template.categoryIcon} "),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildString {
                                append("${template.defaultOptions.size} options ready")
                                template.defaultDeadlineHours?.let { append(" • ${it}h deadline") }
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        TextButton(
            onClick = onBlankClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("empty_state_blank_button")
        ) {
            Text("Or start from a blank poll", fontSize = 13.sp)
        }
    }
}

private fun categoryLabel(category: String): String = when (category) {
    "SOCCER" -> "Soccer"
    "DRINKS" -> "Drinks"
    "FOOD" -> "Food"
    "EVENT" -> "Events"
    "FEEDBACK" -> "Feedback"
    "DECISION" -> "Votes"
    else -> category.lowercase().replaceFirstChar { it.uppercase() }
}
