package com.example

import com.example.data.local.entity.PollEntity
import com.example.data.local.entity.PollOptionEntity
import com.example.data.local.entity.VoteEntity
import com.example.data.model.AuthProvider
import com.example.data.model.PollTemplates
import com.example.data.model.PollWithDetails
import com.example.data.model.UserAccount
import com.example.util.DataExportHelper
import org.junit.Assert.*
import org.junit.Test

class DomainAndCalculationsUnitTest {

    @Test
    fun testPollWithDetailsCalculationsAndTies() {
        val now = System.currentTimeMillis()
        val poll = PollEntity(
            id = "poll_tie_test",
            code = "TIE123",
            title = "Lunch Spot Decider",
            description = "Where should the team eat?",
            category = "FOOD",
            categoryIcon = "🍕",
            creatorName = "Host",
            createdAt = now,
            deadlineTimestamp = now + 3600_000L,
            allowMultipleChoices = false,
            allowCustomOptions = false,
            isAnonymous = false,
            isClosed = false,
            targetHeadcount = 5
        )

        val opt1 = PollOptionEntity(
            id = "opt_taco",
            pollId = "poll_tie_test",
            text = "Taco Fiesta",
            venueAddress = "12 Main St"
        )
        val opt2 = PollOptionEntity(
            id = "opt_sushi",
            pollId = "poll_tie_test",
            text = "Tokyo Sushi",
            venueAddress = "88 Ocean Ave"
        )

        // Equal votes for a tie
        val votes = listOf(
            VoteEntity(id = "v1", pollId = "poll_tie_test", optionId = "opt_taco", voterId = "u1", voterName = "Alex", rsvpStatus = "GOING", plusGuests = 0, timestamp = now),
            VoteEntity(id = "v2", pollId = "poll_tie_test", optionId = "opt_sushi", voterId = "u2", voterName = "Sarah", rsvpStatus = "GOING", plusGuests = 0, timestamp = now)
        )

        val details = PollWithDetails(
            poll = poll,
            options = listOf(opt1, opt2),
            votes = votes,
            currentVoterId = "u1"
        )

        assertEquals(2, details.totalVotes)
        assertEquals(2, details.uniqueVotersCount)
        assertEquals(50.0f, details.getOptionVotePercent("opt_taco"), 0.01f)
        assertEquals(50.0f, details.getOptionVotePercent("opt_sushi"), 0.01f)
        assertNotNull(details.winningOption)
        assertEquals(2, details.headcountSummary.goingCount)
        assertEquals(2, details.headcountSummary.totalAttendingHeadcount)
    }

    @Test
    fun testAnonymousVotingMasksNames() {
        val now = System.currentTimeMillis()
        val poll = PollEntity(
            id = "anon_poll",
            code = "ANON01",
            title = "Anonymous Team Retro",
            description = "Honest feedback",
            category = "DECISION",
            categoryIcon = "⭐",
            creatorName = "Manager",
            createdAt = now,
            isAnonymous = true
        )

        val opt = PollOptionEntity(
            id = "opt_action",
            pollId = "anon_poll",
            text = "Improve unit testing"
        )

        val votes = listOf(
            VoteEntity(
                id = "v_anon",
                pollId = "anon_poll",
                optionId = "opt_action",
                voterId = "u99",
                voterName = "Real Secret Name",
                feedbackComment = "Needs CI pipeline",
                timestamp = now
            )
        )

        val details = PollWithDetails(
            poll = poll,
            options = listOf(opt),
            votes = votes
        )

        // For anonymous poll, voter names are not exposed in getOptionVoters
        val voters = details.getOptionVoters("opt_action")
        assertTrue(voters.isEmpty())

        // Feedback masks voter names to "Anonymous"
        val feedback = details.feedbackList
        assertEquals(1, feedback.size)
        assertEquals("Anonymous", feedback.first().voterName)
        assertEquals("Needs CI pipeline", feedback.first().comment)
    }

    @Test
    fun testPollExpirationAndDeadlineCalculation() {
        val past = System.currentTimeMillis() - 100_000L
        val expiredPoll = PollEntity(
            id = "past_poll",
            code = "PAST01",
            title = "Past Event",
            description = "Closed test",
            category = "EVENT",
            categoryIcon = "📅",
            creatorName = "Host",
            createdAt = past - 200_000L,
            deadlineTimestamp = past
        )

        val details = PollWithDetails(
            poll = expiredPoll,
            options = emptyList(),
            votes = emptyList()
        )

        assertTrue(details.isExpired)
        assertFalse(details.isActive)
    }

    @Test
    fun testTemplatesCatalogIntegrity() {
        val templates = PollTemplates.templates
        assertTrue("Templates catalog should not be empty", templates.isNotEmpty())

        for (template in templates) {
            assertNotNull(template.id)
            assertNotNull(template.title)
            assertNotNull(template.category)
            assertTrue("Template must have an icon", template.categoryIcon.isNotBlank())
            assertTrue("Template must have at least 2 options", template.defaultOptions.size >= 2)
        }
    }

    @Test
    fun testAuthModelsAndPresets() {
        val user = UserAccount(
            id = "test_user_id",
            name = "Alex Test",
            email = "alex@test.com",
            provider = AuthProvider.GOOGLE,
            avatarEmoji = "🧑‍💻"
        )

        assertEquals("Google", user.provider.displayName)
        assertTrue(user.isVerified)

        val guestUser = UserAccount(
            id = "guest_1",
            name = "Guest Visitor",
            email = null,
            provider = AuthProvider.GUEST,
            avatarEmoji = "👤"
        )

        assertEquals("Guest", guestUser.provider.displayName)
    }

    @Test
    fun testDataExportFormattedText() {
        val now = System.currentTimeMillis()
        val poll = PollEntity(
            id = "share_poll",
            code = "PULSE1",
            title = "Friday Team Social",
            description = "Drinks and food",
            category = "DRINKS",
            categoryIcon = "🍻",
            creatorName = "Captain",
            createdAt = now
        )

        val opt1 = PollOptionEntity(
            id = "opt_bar",
            pollId = "share_poll",
            text = "Skyline Lounge",
            venueAddress = "Penthouse Level"
        )

        val details = PollWithDetails(
            poll = poll,
            options = listOf(opt1),
            votes = listOf(
                VoteEntity(id = "v1", pollId = "share_poll", optionId = "opt_bar", voterId = "u1", voterName = "Alex", rsvpStatus = "GOING", plusGuests = 2, timestamp = now)
            )
        )

        val text = DataExportHelper.generateShareableSummary(details)
        assertTrue(text.contains("Friday Team Social"))
        assertTrue(text.contains("PULSE1"))
        assertTrue(text.contains("Skyline Lounge"))

        val csv = DataExportHelper.generateCsv(details)
        assertTrue(csv.contains("Friday Team Social"))
        assertTrue(csv.contains("GOING"))
    }
}
