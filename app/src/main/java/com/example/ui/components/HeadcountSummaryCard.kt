package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RsvpHeadcount
import com.example.ui.theme.GeometricAccent
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusEndingSoon

@Composable
fun HeadcountSummaryCard(
    rsvp: RsvpHeadcount,
    targetHeadcount: Int?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Geometric Title & Quorum Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ATTENDANCE SNAPSHOT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "${rsvp.totalAttendingHeadcount} Confirmed Headcount (${rsvp.totalGuests} +1s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (targetHeadcount != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (rsvp.totalAttendingHeadcount >= targetHeadcount)
                                    StatusActive.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Target: ${rsvp.totalAttendingHeadcount}/$targetHeadcount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rsvp.totalAttendingHeadcount >= targetHeadcount) StatusActive else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quorum Progress Bar if target exists
            if (targetHeadcount != null && targetHeadcount > 0) {
                val progress = (rsvp.totalAttendingHeadcount.toFloat() / targetHeadcount.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (progress >= 1f) StatusActive else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                    Text(
                        text = if (progress >= 1f) "🎉 Target reached for match/outing!" else "${targetHeadcount - rsvp.totalAttendingHeadcount} more attendees needed for full headcount",
                        fontSize = 11.sp,
                        color = if (progress >= 1f) StatusActive else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3 Geometric Badges: Going, Maybe, Can't Go
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RsvpBadgeItem(
                    title = "Going",
                    count = rsvp.goingCount,
                    guests = rsvp.totalGuests,
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.primary,
                    bgColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                )
                RsvpBadgeItem(
                    title = "Maybe",
                    count = rsvp.maybeCount,
                    guests = 0,
                    icon = Icons.Default.Help,
                    color = StatusEndingSoon,
                    bgColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                )
                RsvpBadgeItem(
                    title = "Can't Go",
                    count = rsvp.cantGoCount,
                    guests = 0,
                    icon = Icons.Default.Cancel,
                    color = Color(0xFFB3261E),
                    bgColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                )
            }

            // Bottom Avatar Stack & Count
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Overlapping Avatar Stack
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    val sampleInitials = listOf("JD", "SK", "AK")
                    sampleInitials.forEachIndexed { index, init ->
                        Box(
                            modifier = Modifier
                                .offset(x = (-8 * index).dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when (index) {
                                        0 -> MaterialTheme.colorScheme.primary
                                        1 -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.tertiary
                                    }
                                )
                                .border(2.dp, MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = init,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    val extraCount = rsvp.totalAttendingHeadcount + rsvp.maybeCount - 3
                    if (extraCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = (-24).dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$extraCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "${rsvp.totalAttendingHeadcount + rsvp.maybeCount + rsvp.cantGoCount} Total Responses",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun RsvpBadgeItem(
    title: String,
    count: Int,
    guests: Int,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Text(
                text = "$count",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (guests > 0) {
                Text(
                    text = "+$guests guests",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
