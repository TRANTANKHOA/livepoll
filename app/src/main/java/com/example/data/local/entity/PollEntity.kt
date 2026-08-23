package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "polls")
data class PollEntity(
    @PrimaryKey val id: String,
    val code: String, // 6-digit join code (e.g. "SOC420")
    val title: String,
    val description: String,
    val category: String, // "SOCCER", "DRINKS", "FOOD", "DECISION", "FEEDBACK", "EVENT"
    val categoryIcon: String, // emoji or icon tag
    val creatorName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val deadlineTimestamp: Long? = null,
    val allowMultipleChoices: Boolean = false,
    val allowCustomOptions: Boolean = true,
    val isAnonymous: Boolean = false,
    val isClosed: Boolean = false,
    val targetHeadcount: Int? = null,
    val location: String? = null,
    val bannerDrawableRes: String? = null,
    val groupId: String? = null,
    val groupName: String? = null
)
