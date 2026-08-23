package com.example

import com.example.data.local.entity.PollEntity
import com.example.data.local.entity.PollOptionEntity
import com.example.data.local.entity.VoteEntity
import com.example.data.model.PollWithDetails
import com.example.util.DataExportHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPollWithDetailsCalculations() {
        val now = System.currentTimeMillis()
        val poll = PollEntity(
            id = "test_poll_1",
            code = "TEST01",
            title = "Weekend Soccer Match",
            description = "Venue preference",
            category = "SOCCER",
            categoryIcon = "⚽",
            creatorName = "Host",
            createdAt = now,
            deadlineTimestamp = now + 3600_000L,
            allowMultipleChoices = true,
            allowCustomOptions = true,
            isAnonymous = false,
            isClosed = false,
            targetHeadcount = 10
        )

        val opt1 = PollOptionEntity(
            id = "opt_1",
            pollId = "test_poll_1",
            text = "Riverside Turf",
            venueAddress = "123 River Rd",
            dateTimeSlot = "Sat 4pm",
            duration = "90 mins"
        )
        val opt2 = PollOptionEntity(
            id = "opt_2",
            pollId = "test_poll_1",
            text = "Central Park",
            venueAddress = "Central Field 4",
            dateTimeSlot = "Sat 5:30pm",
            duration = "90 mins"
        )

        val votes = listOf(
            VoteEntity(id = "v1", pollId = "test_poll_1", optionId = "opt_1", voterId = "user_1", voterName = "Alex", rsvpStatus = "GOING", plusGuests = 1, timestamp = now),
            VoteEntity(id = "v2", pollId = "test_poll_1", optionId = "opt_1", voterId = "user_2", voterName = "Sarah", rsvpStatus = "GOING", plusGuests = 0, timestamp = now),
            VoteEntity(id = "v3", pollId = "test_poll_1", optionId = "opt_2", voterId = "user_3", voterName = "Marcus", rsvpStatus = "MAYBE", plusGuests = 0, timestamp = now)
        )

        val details = PollWithDetails(
            poll = poll,
            options = listOf(opt1, opt2),
            votes = votes,
            currentVoterId = "user_1"
        )

        // Verify votes count and unique voter calculations
        assertEquals(3, details.totalVotes)
        assertEquals(3, details.uniqueVotersCount)
        assertEquals(true, details.hasUserVoted)
        assertEquals(false, details.isExpired)
        assertEquals(true, details.isActive)

        // Verify option vote counts & percentages
        assertEquals(2, details.getOptionVoteCount("opt_1"))
        assertEquals(1, details.getOptionVoteCount("opt_2"))
        assertEquals(66.66667f, details.getOptionVotePercent("opt_1"), 0.1f)
        assertEquals(33.33333f, details.getOptionVotePercent("opt_2"), 0.1f)
        assertEquals("opt_1", details.winningOption?.id)

        // Verify Headcount & RSVP calculation (+1s)
        val headcount = details.headcountSummary
        assertEquals(2, headcount.goingCount)
        assertEquals(1, headcount.maybeCount)
        assertEquals(0, headcount.cantGoCount)
        assertEquals(1, headcount.totalGuests)
        assertEquals(3, headcount.totalAttendingHeadcount) // 2 going + 1 guest = 3
    }

    @Test
    fun testDataExportCsvGeneration() {
        val now = 1700000000000L
        val poll = PollEntity(
            id = "test_poll_2",
            code = "EXP123",
            title = "Friday Drinks",
            description = "After work",
            category = "DRINKS",
            categoryIcon = "🍻",
            creatorName = "Alex",
            createdAt = now
        )

        val opt = PollOptionEntity(
            id = "opt_a",
            pollId = "test_poll_2",
            text = "Rooftop Bar",
            venueAddress = "Downtown",
            dateTimeSlot = "Fri 6pm",
            duration = "2 hours"
        )

        val details = PollWithDetails(
            poll = poll,
            options = listOf(opt),
            votes = emptyList()
        )

        val csv = DataExportHelper.generateCsv(details)
        assertTrue(csv.contains("Poll Title,Category,Created By"))
        assertTrue(csv.contains("Friday Drinks"))
        assertTrue(csv.contains("Rooftop Bar"))

        val shareText = DataExportHelper.generateShareableSummary(details)
        assertTrue(shareText.contains("Friday Drinks"))
        assertTrue(shareText.contains("EXP123"))
    }
}
