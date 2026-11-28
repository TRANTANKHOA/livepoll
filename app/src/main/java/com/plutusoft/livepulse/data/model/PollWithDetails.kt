package com.plutusoft.livepulse.data.model

import com.plutusoft.livepulse.data.local.entity.PollEntity
import com.plutusoft.livepulse.data.local.entity.PollOptionEntity
import com.plutusoft.livepulse.data.local.entity.VoteEntity

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
    // Derived values are cached properties, not computed get()s: a
    // PollWithDetails is constructed once per flow emission, while the
    // results screen recomposes many times per second (tab switches, ticker,
    // listener snapshots). Computing tallies per access made every frame
    // re-run filters/groupBy/sorts — the KB-14 jank (81 ms median frames).

    val totalVotes: Int = votes.size

    val uniqueVotersCount: Int = votes.map { it.voterId }.distinct().size

    val isExpired: Boolean
        get() = poll.deadlineTimestamp != null && System.currentTimeMillis() > poll.deadlineTimestamp

    val isActive: Boolean
        get() = !poll.isClosed && !isExpired

    val timeLeftMillis: Long?
        get() = poll.deadlineTimestamp?.let { it - System.currentTimeMillis() }

    private val votesByOption: Map<String, List<VoteEntity>> =
        votes.groupBy { it.optionId }

    private val votesByVoter: Map<String, List<VoteEntity>> =
        votes.groupBy { it.voterId }

    val userVotes: List<VoteEntity> =
        votesByVoter[currentVoterId] ?: emptyList()

    val hasUserVoted: Boolean = userVotes.isNotEmpty()

    fun getOptionVoteCount(optionId: String): Int =
        votesByOption[optionId]?.size ?: 0

    fun getOptionVotePercent(optionId: String): Float {
        if (totalVotes == 0) return 0f
        return (getOptionVoteCount(optionId).toFloat() / totalVotes.toFloat()) * 100f
    }

    fun getOptionVoters(optionId: String): List<String> {
        if (poll.isAnonymous) return emptyList()
        return votesByOption[optionId]?.map { it.voterName } ?: emptyList()
    }

    val winningOption: PollOptionEntity? =
        if (options.isEmpty() || totalVotes == 0) null
        else options.maxByOrNull { getOptionVoteCount(it.id) }

    val headcountSummary: RsvpHeadcount = run {
        // First vote per voter carries the ballot's RSVP state — multi-select
        // ballots must count their owner once.
        val uniqueVoterVotes = votesByVoter.values.map { it.first() }
        val going = uniqueVoterVotes.count { it.rsvpStatus == "GOING" }
        val maybe = uniqueVoterVotes.count { it.rsvpStatus == "MAYBE" }
        val cantGo = uniqueVoterVotes.count { it.rsvpStatus == "CANT_GO" }
        val guests = uniqueVoterVotes.filter { it.rsvpStatus == "GOING" }.sumOf { it.plusGuests }
        RsvpHeadcount(
            goingCount = going,
            maybeCount = maybe,
            cantGoCount = cantGo,
            totalGuests = guests,
            totalAttendingHeadcount = going + guests
        )
    }

    val averageRating: Float? = run {
        val ratings = votes.mapNotNull { it.ratingValue }
        if (ratings.isEmpty()) null else ratings.average().toFloat()
    }

    val feedbackList: List<FeedbackItem> =
        votes
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
