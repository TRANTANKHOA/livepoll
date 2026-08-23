package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String, // emoji or icon code
    val description: String,
    val memberCount: Int = 1,
    val isUserMember: Boolean = true,
    val notificationsEnabled: Boolean = true
)
