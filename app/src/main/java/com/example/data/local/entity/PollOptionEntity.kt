package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "poll_options",
    foreignKeys = [
        ForeignKey(
            entity = PollEntity::class,
            parentColumns = ["id"],
            childColumns = ["pollId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pollId")]
)
data class PollOptionEntity(
    @PrimaryKey val id: String,
    val pollId: String,
    val text: String,
    val subtitle: String? = null,
    val dateTimeSlot: String? = null,
    val venueAddress: String? = null,
    val duration: String? = null, // e.g. "90 mins", "2 hours", "1 hour", "All Day"
    val priceRating: String? = null, // e.g. "$$", "Free", "$12 / player"
    val addedBy: String? = null, // null if creator, or participant name
    val displayOrder: Int = 0
)
