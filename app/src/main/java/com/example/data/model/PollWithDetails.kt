package com.example.data.model

import com.example.data.local.entity.PollEntity
import com.example.data.local.entity.PollOptionEntity
import com.example.data.local.entity.VoteEntity

data class RsvpHeadcount(
    val goingCount: Int = 0,
    val maybeCount: Int = 0,
    val cantGoCount: Int = 0,
    val totalGuests: Int = 0,
    val totalAttendingHeadcount: Int = 0 // goingCount + totalGuests
)

data class FeedbackItem(
    val voterName: String,
    val rsvpStatus: String,
    val rating: Int?,
    val comment: String,
    val timestamp: Long
)

data class PollWithDetails(
    val poll: PollEntity,
    val options: List<PollOptionEntity> = emptyList(),
    val votes: List<VoteEntity> = emptyList(),
    val currentVoterId: String = "current_user"
) {
    val totalVotes: Int = votes.size

    val uniqueVotersCount: Int = votes.map { it.voterId }.distinct().size

    val isExpired: Boolean
        get() = poll.deadlineTimestamp != null && System.currentTimeMillis() > poll.deadlineTimestamp

    val isActive: Boolean
        get() = !poll.isClosed && !isExpired

    val timeLeftMillis: Long?
        get() = poll.deadlineTimestamp?.let { it - System.currentTimeMillis() }

    val userVotes: List<VoteEntity>
        get() = votes.filter { it.voterId == currentVoterId }

    val hasUserVoted: Boolean
        get() = userVotes.isNotEmpty()

    fun getOptionVoteCount(optionId: String): Int {
        return votes.count { it.optionId == optionId }
    }

    fun getOptionVotePercent(optionId: String): Float {
        if (totalVotes == 0) return 0f
        return (getOptionVoteCount(optionId).toFloat() / totalVotes.toFloat()) * 100f
    }

    fun getOptionVoters(optionId: String): List<String> {
        if (poll.isAnonymous) return emptyList()
        return votes.filter { it.optionId == optionId }.map { it.voterName }
    }

    val winningOption: PollOptionEntity?
        get() {
            if (options.isEmpty() || totalVotes == 0) return null
            return options.maxByOrNull { getOptionVoteCount(it.id) }
        }

    val headcountSummary: RsvpHeadcount
        get() {
            // Group votes by voterId to avoid counting duplicate entries from multi-select
            val uniqueVoterVotes = votes.groupBy { it.voterId }.values.map { it.first() }
            val going = uniqueVoterVotes.count { it.rsvpStatus == "GOING" }
            val maybe = uniqueVoterVotes.count { it.rsvpStatus == "MAYBE" }
            val cantGo = uniqueVoterVotes.count { it.rsvpStatus == "CANT_GO" }
            val guests = uniqueVoterVotes.filter { it.rsvpStatus == "GOING" }.sumOf { it.plusGuests }
            return RsvpHeadcount(
                goingCount = going,
                maybeCount = maybe,
                cantGoCount = cantGo,
                totalGuests = guests,
                totalAttendingHeadcount = going + guests
            )
        }

    val averageRating: Float?
        get() {
            val ratings = votes.mapNotNull { it.ratingValue }
            if (ratings.isEmpty()) return null
            return ratings.average().toFloat()
        }

    val feedbackList: List<FeedbackItem>
        get() {
            return votes
                .filter { !it.feedbackComment.isNullOrBlank() }
                .groupBy { it.voterId }
                .values
                .map { list ->
                    val v = list.first()
                    FeedbackItem(
                        voterName = if (poll.isAnonymous) "Anonymous" else v.voterName,
                        rsvpStatus = v.rsvpStatus,
                        rating = v.ratingValue,
                        comment = v.feedbackComment ?: "",
                        timestamp = v.timestamp
                    )
                }
                .sortedByDescending { it.timestamp }
        }
}
