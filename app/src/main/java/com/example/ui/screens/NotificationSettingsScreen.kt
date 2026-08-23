package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.core.content.ContextCompat
import com.example.data.local.entity.GroupEntity
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusEndingSoon
import com.example.ui.viewmodel.PollViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: PollViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.notificationSettings.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notifications are restricted in system settings", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notification Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("notification_settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Permission Status Banner (if on Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasNotificationPermission)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (hasNotificationPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (hasNotificationPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Column {
                                    Text(
                                        text = if (hasNotificationPermission) "System Permission Granted" else "Push Permission Required",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (hasNotificationPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = if (hasNotificationPermission) "App can deliver instant deadline & group alerts" else "Tap to allow Android push notifications",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!hasNotificationPermission) {
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("request_permission_button")
                                ) {
                                    Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Master Push Notification Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (settings.masterPushEnabled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (settings.masterPushEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (settings.masterPushEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column {
                                Text(
                                    text = "Allow Push Notifications",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (settings.masterPushEnabled) "Receiving alerts on lock screen & tray" else "All push notifications are paused",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = settings.masterPushEnabled,
                            onCheckedChange = { viewModel.updateMasterPush(it) },
                            modifier = Modifier.testTag("master_push_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // 3. Alert Types & Rules (Visible when master push is on)
            item {
                AnimatedVisibility(visible = settings.masterPushEnabled) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "ALERT CHANNELS & PREFERENCES",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Group Polls Alert
                            SettingToggleRow(
                                title = "New Polls in My Groups",
                                description = "Alert me when group members propose new venue & time slots",
                                icon = "📢",
                                isChecked = settings.groupPollsAlertsEnabled,
                                onCheckedChange = { viewModel.updateGroupPollsAlerts(it) },
                                testTag = "group_polls_switch"
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Approaching Deadlines Alert
                            SettingToggleRow(
                                title = "Approaching Voting Deadlines",
                                description = "Remind me before voting closes on active surveys",
                                icon = "⏰",
                                isChecked = settings.approachingDeadlinesEnabled,
                                onCheckedChange = { viewModel.updateApproachingDeadlines(it) },
                                testTag = "deadlines_switch"
                            )

                            // Deadline Lead Time Selector (if deadlines enabled)
                            if (settings.approachingDeadlinesEnabled) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Remind me before deadline:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val options = listOf(
                                            1 to "1 Hour",
                                            2 to "2 Hours",
                                            4 to "4 Hours",
                                            12 to "12 Hours",
                                            24 to "24 Hours"
                                        )
                                        options.forEach { (hours, label) ->
                                            val isSelected = settings.deadlineLeadTimeHours == hours
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.updateDeadlineLeadTime(hours) },
                                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                modifier = Modifier.testTag("lead_time_chip_$hours")
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Vote Activity Alerts
                            SettingToggleRow(
                                title = "Vote & RSVP Activity",
                                description = "Notify when someone votes or updates attendance (+1s)",
                                icon = "🗳️",
                                isChecked = settings.voteActivityAlertsEnabled,
                                onCheckedChange = { viewModel.updateVoteActivityAlerts(it) },
                                testTag = "votes_activity_switch"
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Vibration & Haptics
                            SettingToggleRow(
                                title = "Vibration & Haptic Alert",
                                description = "Vibrate device when receiving survey and deadline alerts",
                                icon = "📳",
                                isChecked = settings.vibrationEnabled,
                                onCheckedChange = { viewModel.updateVibration(it) },
                                testTag = "vibration_switch"
                            )
                        }
                    }
                }
            }

            // 4. Group-Specific Notification Toggles
            item {
                AnimatedVisibility(visible = settings.masterPushEnabled && settings.groupPollsAlertsEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "GROUP NOTIFICATION CONTROLS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                allGroups.forEachIndexed { index, group ->
                                    GroupNotificationItem(
                                        group = group,
                                        isMuted = settings.mutedGroupIds.contains(group.id),
                                        onToggleMute = { isMuted ->
                                            viewModel.toggleGroupNotifications(group.id, !isMuted)
                                        },
                                        onToggleMembership = { isMember ->
                                            viewModel.toggleGroupMembership(group.id, isMember)
                                        }
                                    )
                                    if (index < allGroups.size - 1) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Test & Trigger Live Push Notifications
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🧪", fontSize = 18.sp)
                            Text(
                                text = "Test & Simulate Push Alerts",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Text(
                            text = "Send sample notifications to verify system sound, vibration, venue/time attributes, and tray display:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.testGroupPollNotification(context)
                                    Toast.makeText(context, "Group poll notification triggered!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_group_notification_button")
                            ) {
                                Icon(imageVector = Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Group Poll", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.testDeadlineNotification(context)
                                    Toast.makeText(context, "Deadline notification triggered!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryLight),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_deadline_notification_button")
                            ) {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Deadline Alert", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.checkApproachingDeadlines(context) { count ->
                                    val msg = if (count > 0) "Found $count active surveys nearing deadline. Alerts sent!"
                                    else "All active surveys have plenty of time remaining."
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_approaching_deadlines_button")
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan & Alert Approaching Deadlines Now", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    icon: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun GroupNotificationItem(
    group: GroupEntity,
    isMuted: Boolean,
    onToggleMute: (Boolean) -> Unit,
    onToggleMembership: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = group.icon, fontSize = 18.sp)
            }

            Column {
                Text(
                    text = group.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${group.memberCount} members • ${if (group.isUserMember) "Joined" else "Not Joined"}",
                    fontSize = 11.sp,
                    color = if (group.isUserMember) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (group.isUserMember) {
                Surface(
                    onClick = { onToggleMute(!isMuted) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isMuted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.testTag("mute_group_${group.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isMuted) "Muted" else "Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { onToggleMembership(true) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Join Group", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
