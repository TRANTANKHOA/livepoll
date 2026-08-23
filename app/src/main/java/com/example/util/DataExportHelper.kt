package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.PollWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExportHelper {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    fun generateCsv(pollDetails: PollWithDetails): String {
        val sb = StringBuilder()
        val poll = pollDetails.poll

        // Header Section
        sb.append("Poll Title,Category,Created By,Created At,Deadline,Status,Total Votes,Unique Voters\n")
        val deadlineStr = poll.deadlineTimestamp?.let { dateFormat.format(Date(it)) } ?: "No Deadline"
        val statusStr = if (pollDetails.isActive) "Active" else "Closed"
        sb.append("\"${escapeCsv(poll.title)}\",\"${poll.category}\",\"${escapeCsv(poll.creatorName)}\",\"${dateFormat.format(Date(poll.createdAt))}\",\"$deadlineStr\",\"$statusStr\",${pollDetails.totalVotes},${pollDetails.uniqueVotersCount}\n\n")

        // Options Breakdown
        sb.append("Option #,Option Text,Time Slot,Location,Price/Info,Votes Count,Percentage,Added By\n")
        pollDetails.options.forEachIndexed { index, opt ->
            val voteCount = pollDetails.getOptionVoteCount(opt.id)
            val percent = String.format(Locale.US, "%.1f%%", pollDetails.getOptionVotePercent(opt.id))
            sb.append("${index + 1},\"${escapeCsv(opt.text)}\",\"${escapeCsv(opt.dateTimeSlot ?: "")}\",\"${escapeCsv(opt.venueAddress ?: "")}\",\"${escapeCsv(opt.priceRating ?: "")}\",$voteCount,$percent,\"${escapeCsv(opt.addedBy ?: "Creator")}\"\n")
        }
        sb.append("\n")

        // RSVP Summary
        val rsvp = pollDetails.headcountSummary
        sb.append("RSVP Breakdown\n")
        sb.append("Going,Maybe,Cannot Go,Guests (+1s),Total Headcount\n")
        sb.append("${rsvp.goingCount},${rsvp.maybeCount},${rsvp.cantGoCount},${rsvp.totalGuests},${rsvp.totalAttendingHeadcount}\n\n")

        // Detailed Voter Log
        sb.append("Voter Log\n")
        sb.append("Voter Name,Selected Option,RSVP Status,Guests (+),Rating (1-5),Feedback / Note,Timestamp\n")
        pollDetails.votes.forEach { vote ->
            val optionText = pollDetails.options.find { it.id == vote.optionId }?.text ?: "Unknown Option"
            val voterName = if (poll.isAnonymous) "Anonymous" else vote.voterName
            val ratingStr = vote.ratingValue?.toString() ?: "-"
            val commentStr = vote.feedbackComment ?: ""
            val timeStr = dateFormat.format(Date(vote.timestamp))
            sb.append("\"${escapeCsv(voterName)}\",\"${escapeCsv(optionText)}\",\"${vote.rsvpStatus}\",${vote.plusGuests},\"$ratingStr\",\"${escapeCsv(commentStr)}\",\"$timeStr\"\n")
        }

        return sb.toString()
    }

    fun generateShareableSummary(pollDetails: PollWithDetails): String {
        val poll = pollDetails.poll
        val sb = StringBuilder()
        sb.append("📊 *${poll.title}*\n")
        if (poll.description.isNotBlank()) {
            sb.append("_${poll.description}_\n\n")
        }

        val deadlineStr = poll.deadlineTimestamp?.let {
            val date = Date(it)
            "⏳ *Deadline:* ${dateFormat.format(date)}\n"
        } ?: ""
        if (deadlineStr.isNotEmpty()) sb.append(deadlineStr)

        sb.append("🗳️ *Total Votes:* ${pollDetails.totalVotes} | 👥 *Voters:* ${pollDetails.uniqueVotersCount}\n\n")

        val winning = pollDetails.winningOption
        if (winning != null && pollDetails.totalVotes > 0) {
            sb.append("🏆 *Current Leader:* ${winning.text} (${String.format(Locale.US, "%.0f%%", pollDetails.getOptionVotePercent(winning.id))})\n\n")
        }

        sb.append("*Options & Standings:*\n")
        pollDetails.options.forEach { opt ->
            val count = pollDetails.getOptionVoteCount(opt.id)
            val percent = String.format(Locale.US, "%.0f%%", pollDetails.getOptionVotePercent(opt.id))
            val slot = if (!opt.dateTimeSlot.isNullOrBlank()) " (${opt.dateTimeSlot})" else ""
            sb.append("• ${opt.text}$slot — *$count votes* ($percent)\n")
        }

        val rsvp = pollDetails.headcountSummary
        if (rsvp.totalAttendingHeadcount > 0) {
            sb.append("\n👥 *RSVP Attendance:* ${rsvp.totalAttendingHeadcount} Confirmed Headcount (${rsvp.goingCount} Going, ${rsvp.totalGuests} Guests)\n")
        }

        sb.append("\n🔑 *Join & Vote with Code:* *${poll.code}*\n")
        sb.append("📲 *How to Vote:* Open PulsePoll and tap *'Join with Code'* → enter *${poll.code}*\n")
        sb.append("_(Instant multi-device voting, real-time live results & RSVP attendance)_")

        return sb.toString()
    }

    fun generateJsonBackup(pollDetails: PollWithDetails): String {
        val poll = pollDetails.poll
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"id\": \"${poll.id}\",\n")
        sb.append("  \"code\": \"${poll.code}\",\n")
        sb.append("  \"title\": \"${escapeJson(poll.title)}\",\n")
        sb.append("  \"description\": \"${escapeJson(poll.description)}\",\n")
        sb.append("  \"category\": \"${poll.category}\",\n")
        sb.append("  \"creatorName\": \"${escapeJson(poll.creatorName)}\",\n")
        sb.append("  \"totalVotes\": ${pollDetails.totalVotes},\n")
        sb.append("  \"options\": [\n")
        pollDetails.options.forEachIndexed { idx, opt ->
            sb.append("    {\"id\": \"${opt.id}\", \"text\": \"${escapeJson(opt.text)}\", \"votes\": ${pollDetails.getOptionVoteCount(opt.id)}}")
            if (idx < pollDetails.options.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    fun shareText(context: Context, text: String, title: String = "Share Poll") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
