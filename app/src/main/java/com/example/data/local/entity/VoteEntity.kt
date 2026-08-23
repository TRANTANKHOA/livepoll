package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "votes",
    foreignKeys = [
        ForeignKey(
            entity = PollEntity::class,
            parentColumns = ["id"],
            childColumns = ["pollId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PollOptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["optionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("pollId"),
        Index("optionId"),
        Index(value = ["pollId", "voterId", "optionId"], unique = true)
    ]
)
data class VoteEntity(
    @PrimaryKey val id: String,
    val pollId: String,
    val optionId: String,
    val voterId: String,
    val voterName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val rsvpStatus: String = "GOING", // "GOING", "MAYBE", "CANT_GO"
    val plusGuests: Int = 0, // e.g. +1, +2
    val ratingValue: Int? = null, // e.g. 1..5 stars
    val feedbackComment: String? = null
)
