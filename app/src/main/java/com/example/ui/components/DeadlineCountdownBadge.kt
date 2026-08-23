package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusClosed
import com.example.ui.theme.StatusEndingSoon
import kotlinx.coroutines.delay

@Composable
fun DeadlineCountdownBadge(
    deadlineTimestamp: Long?,
    isClosed: Boolean,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(deadlineTimestamp, isClosed) {
        if (!isClosed && deadlineTimestamp != null) {
            while (true) {
                delay(30000) // Update every 30 seconds
                currentTime = System.currentTimeMillis()
            }
        }
    }

    val isExpired = deadlineTimestamp != null && currentTime > deadlineTimestamp

    val (bg, fg, label, icon) = when {
        isClosed -> {
            Tuple4(
                StatusClosed.copy(alpha = 0.15f),
                StatusClosed,
                "Closed",
                Icons.Default.Lock
            )
        }
        isExpired -> {
            Tuple4(
                StatusClosed.copy(alpha = 0.15f),
                StatusClosed,
                "Expired",
                Icons.Default.Lock
            )
        }
        deadlineTimestamp == null -> {
            Tuple4(
                StatusActive.copy(alpha = 0.15f),
                StatusActive,
                "Open (No Deadline)",
                Icons.Default.Schedule
            )
        }
        else -> {
            val diff = deadlineTimestamp - currentTime
            val hours = diff / (3600 * 1000)
            val minutes = (diff % (3600 * 1000)) / (60 * 1000)

            when {
                diff <= 3600 * 1000 -> {
                    Tuple4(
                        StatusEndingSoon.copy(alpha = 0.2f),
                        StatusEndingSoon,
                        if (minutes > 0) "Ends in ${minutes}m" else "Ending now!",
                        Icons.Default.HourglassBottom
                    )
                }
                hours < 24 -> {
                    Tuple4(
                        StatusActive.copy(alpha = 0.15f),
                        StatusActive,
                        "Ends in ${hours}h ${minutes}m",
                        Icons.Default.Schedule
                    )
                }
                else -> {
                    val days = hours / 24
                    Tuple4(
                        StatusActive.copy(alpha = 0.15f),
                        StatusActive,
                        "Ends in ${days}d ${hours % 24}h",
                        Icons.Default.Schedule
                    )
                }
            }
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
