package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val pollId: String,
    val pollTitle: String,
    val title: String,
    val message: String,
    val type: String, // "DEADLINE_WARNING", "NEW_VOTE", "POLL_CLOSED", "RESULT_ANNOUNCEMENT"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
