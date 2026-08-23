package com.example.data.repository

import android.content.Context
import com.example.data.local.dao.GroupDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.PollDao
import com.example.data.local.dao.PollOptionDao
import com.example.data.local.dao.VoteDao
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.PollEntity
import com.example.data.local.entity.PollOptionEntity
import com.example.data.local.entity.VoteEntity
import com.example.data.model.PollWithDetails
import com.example.util.NotificationHelper
import com.example.data.remote.FirestoreService
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

class PollRepository(
    private val pollDao: PollDao,
    private val pollOptionDao: PollOptionDao,
    private val voteDao: VoteDao,
    private val notificationDao: NotificationDao,
    private val groupDao: GroupDao,
    val firestoreService: FirestoreService = FirestoreService()
) {
    val allPolls: Flow<List<PollEntity>> = pollDao.getAllPolls()
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val unreadNotificationCount: Flow<Int> = notificationDao.getUnreadCount()
    val allGroups: Flow<List<GroupEntity>> = groupDao.getAllGroups()
    val userJoinedGroups: Flow<List<GroupEntity>> = groupDao.getUserJoinedGroups()

    suspend fun getPollByCode(code: String): PollEntity? {
        val uppercaseCode = code.trim().uppercase()
        val localPoll = pollDao.getPollByCode(uppercaseCode)
        if (localPoll != null) {
            return localPoll
        }

        // Check Cloud Firestore for poll created on other devices
        val cloudPollData = firestoreService.findPollByCode(uppercaseCode)
        if (cloudPollData != null) {
            val (pollEntity, options) = cloudPollData
            pollDao.insertPoll(pollEntity)
            pollOptionDao.insertOptions(options)

            // Also fetch cloud votes
            val cloudVotes = firestoreService.fetchCloudVotes(pollEntity.id)
            if (cloudVotes.isNotEmpty()) {
                voteDao.insertVotes(cloudVotes)
            }
            return pollEntity
        }

        return null
    }

    suspend fun getGroupById(groupId: String): GroupEntity? {
        return groupDao.getGroupById(groupId)
    }

    suspend fun setGroupMembership(groupId: String, isMember: Boolean) {
        groupDao.setMembership(groupId, isMember)
    }

    suspend fun setGroupNotifications(groupId: String, enabled: Boolean) {
        groupDao.setGroupNotifications(groupId, enabled)
    }

    fun getPollWithDetails(pollId: String, currentUserId: String): Flow<PollWithDetails?> {
        val pollFlow = pollDao.getPollById(pollId)
        val optionsFlow = pollOptionDao.getOptionsForPoll(pollId)
        val votesFlow = voteDao.getVotesForPoll(pollId)

        return combine(pollFlow, optionsFlow, votesFlow) { poll, options, votes ->
            if (poll == null) null
            else {
                PollWithDetails(
                    poll = poll,
                    options = options,
                    votes = votes,
                    currentVoterId = currentUserId
                )
            }
        }
    }

    /**
     * Attaches live cloud snapshot sync to keep votes updated in real-time across devices.
     */
    fun attachLiveCloudSync(pollId: String, scope: CoroutineScope): ListenerRegistration? {
        return firestoreService.attachLiveVotesListener(pollId) { cloudVotes ->
            scope.launch(Dispatchers.IO) {
                if (cloudVotes.isNotEmpty()) {
                    voteDao.insertVotes(cloudVotes)
                }
            }
        }
    }

    suspend fun createPoll(poll: PollEntity, options: List<PollOptionEntity>) {
        pollDao.insertPoll(poll)
        pollOptionDao.insertOptions(options)

        // Sync to Cloud Firestore in the background
        firestoreService.publishPollToCloud(poll, options)

        // Insert in notification feed
        val notifTitle = if (!poll.groupName.isNullOrBlank()) {
            "📢 New Poll in ${poll.groupName}: ${poll.title}"
        } else {
            "🎉 New Poll Created: ${poll.title}"
        }

        val notif = NotificationEntity(
            id = UUID.randomUUID().toString(),
            pollId = poll.id,
            pollTitle = poll.title,
            title = notifTitle,
            message = "Join code: ${poll.code}. ${options.size} venue & time choices ready for voting.",
            type = "POLL_CREATED",
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notif)
    }

    suspend fun castVotes(
        pollId: String,
        voterId: String,
        voterName: String,
        selectedOptionIds: List<String>,
        rsvpStatus: String,
        plusGuests: Int,
        rating: Int?,
        comment: String?
    ) {
        // Clear previous votes by this user for this poll
        voteDao.clearUserVotesForPoll(pollId, voterId)

        val newVotes = selectedOptionIds.map { optId ->
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = pollId,
                optionId = optId,
                voterId = voterId,
                voterName = voterName,
                timestamp = System.currentTimeMillis(),
                rsvpStatus = rsvpStatus,
                plusGuests = plusGuests,
                ratingValue = rating,
                feedbackComment = comment
            )
        }
        voteDao.insertVotes(newVotes)

        // Sync votes to Cloud Firestore
        for (vote in newVotes) {
            firestoreService.recordVoteToCloud(vote)
        }
    }

    suspend fun addOptionToPoll(option: PollOptionEntity) {
        pollOptionDao.insertOption(option)
    }

    suspend fun togglePollStatus(pollId: String, isClosed: Boolean) {
        pollDao.setPollClosed(pollId, isClosed)
        val poll = pollDao.getPollByIdSync(pollId)
        if (poll != null && isClosed) {
            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                pollId = poll.id,
                pollTitle = poll.title,
                title = "🔒 Voting Ended: ${poll.title}",
                message = "The voting deadline has reached and final results are now locked in.",
                type = "POLL_CLOSED",
                timestamp = System.currentTimeMillis()
            )
            notificationDao.insertNotification(notif)
        }
    }

    suspend fun deletePoll(pollId: String) {
        pollDao.deletePoll(pollId)
        notificationDao.deleteNotificationsForPoll(pollId)
    }

    suspend fun markAllNotificationsAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun clearAllNotifications() {
        notificationDao.clearAllNotifications()
    }

    suspend fun insertNotification(notification: NotificationEntity) {
        notificationDao.insertNotification(notification)
    }

    suspend fun checkApproachingDeadlines(context: Context, leadTimeHours: Int): Int {
        val now = System.currentTimeMillis()
        val leadTimeMs = leadTimeHours * 3600 * 1000L
        val activePolls = pollDao.getActivePollsWithDeadline(now)

        var notifiedCount = 0
        for (poll in activePolls) {
            val deadline = poll.deadlineTimestamp ?: continue
            val remainingMs = deadline - now
            if (remainingMs in 1..leadTimeMs) {
                val remainingMins = (remainingMs / (1000 * 60)).toInt()
                val timeStr = if (remainingMins >= 60) "${remainingMins / 60}h ${remainingMins % 60}m" else "$remainingMins mins"

                val notifTitle = "⏰ Voting Deadline Approaching!"
                val notifMsg = "'${poll.title}' closes in $timeStr. Cast your vote for your preferred venue & time before time runs out!"

                val notif = NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    pollId = poll.id,
                    pollTitle = poll.title,
                    title = "⏰ Deadline Alert ($timeStr left)",
                    message = notifMsg,
                    type = "DEADLINE_WARNING",
                    timestamp = System.currentTimeMillis()
                )
                notificationDao.insertNotification(notif)

                NotificationHelper.showNotification(
                    context = context,
                    notificationId = (poll.id + "_approaching").hashCode(),
                    title = notifTitle,
                    message = notifMsg,
                    channelId = NotificationHelper.CHANNEL_DEADLINES,
                    pollId = poll.id
                )
                notifiedCount++
            }
        }
        return notifiedCount
    }

    suspend fun ensureSeeded() {
        if (groupDao.getGroupsCount() == 0) {
            seedGroups()
        }
        if (pollDao.getPollsCount() == 0) {
            seedInitialPolls()
        }
    }

    private suspend fun seedGroups() {
        val defaultGroups = listOf(
            GroupEntity(
                id = "group_soccer",
                name = "Weekend Soccer Squad ⚽",
                icon = "⚽",
                description = "Weekly 5v5 & 7v7 soccer matches, venue pitches, and time polling.",
                memberCount = 14,
                isUserMember = true,
                notificationsEnabled = true
            ),
            GroupEntity(
                id = "group_social",
                name = "Friday Social Club 🍻",
                icon = "🍻",
                description = "Post-work drinks, happy hour bar selections, and weekend hangouts.",
                memberCount = 18,
                isUserMember = true,
                notificationsEnabled = true
            ),
            GroupEntity(
                id = "group_eng",
                name = "Product & Engineering Crew 💻",
                icon = "💻",
                description = "Team lunches, sprint celebrations, and hackathon feedback polls.",
                memberCount = 12,
                isUserMember = true,
                notificationsEnabled = true
            ),
            GroupEntity(
                id = "group_gamers",
                name = "Downtown Board Gamers 🎲",
                icon = "🎲",
                description = "Sunday board game nights, RPG campaigns, and tournament RSVP.",
                memberCount = 9,
                isUserMember = true,
                notificationsEnabled = true
            ),
            GroupEntity(
                id = "group_sports",
                name = "Community Sports League 🏆",
                icon = "🏆",
                description = "Basketball, volleyball, and tennis court reservations and voting.",
                memberCount = 22,
                isUserMember = false,
                notificationsEnabled = true
            )
        )
        groupDao.insertGroups(defaultGroups)
    }

    private suspend fun seedInitialPolls() {
        val now = System.currentTimeMillis()

        // 1. Weekend Soccer Match (Group: Weekend Soccer Squad)
        val soccerPollId = "poll_soccer_001"
        val soccerPoll = PollEntity(
            id = soccerPollId,
            code = "SOC5V5",
            title = "Saturday 5v5 Soccer: Pitch Venue & Kickoff Time",
            description = "Let's lock down the venue, turf pitch, and kickoff slot for our weekend match. Needs at least 10 players!",
            category = "SOCCER",
            categoryIcon = "⚽",
            creatorName = "Coach Marcus",
            createdAt = now - (3 * 3600 * 1000L),
            deadlineTimestamp = now + (3 * 3600 * 1000L), // Closes in 3 hours
            allowMultipleChoices = true,
            allowCustomOptions = true,
            isAnonymous = false,
            isClosed = false,
            targetHeadcount = 10,
            location = "River Road Sports Complex",
            groupId = "group_soccer",
            groupName = "Weekend Soccer Squad ⚽"
        )
        val soccerOptions = listOf(
            PollOptionEntity(
                id = "opt_soc_1",
                pollId = soccerPollId,
                text = "Riverside Turf Arena (Pitch 2)",
                subtitle = "Turf grass, floodlights, ample parking",
                dateTimeSlot = "Saturday 4:00 PM - 5:30 PM",
                venueAddress = "240 River Road Field, South Park",
                duration = "90 mins",
                priceRating = "$12 / player",
                displayOrder = 1
            ),
            PollOptionEntity(
                id = "opt_soc_2",
                pollId = soccerPollId,
                text = "Central Community Pitch (Grass)",
                subtitle = "Natural grass, public access, BYO bibs",
                dateTimeSlot = "Saturday 5:30 PM - 7:00 PM",
                venueAddress = "Central Park Field 4",
                duration = "90 mins",
                priceRating = "Free",
                displayOrder = 2
            ),
            PollOptionEntity(
                id = "opt_soc_3",
                pollId = soccerPollId,
                text = "Metro Sports Dome (Indoor A/C)",
                subtitle = "Indoor air-conditioned turf, lockers available",
                dateTimeSlot = "Sunday 10:00 AM - 11:30 AM",
                venueAddress = "88 Olympic Way, Midtown",
                duration = "90 mins",
                priceRating = "$15 / player",
                displayOrder = 3
            )
        )
        val soccerVotes = listOf(
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = soccerPollId,
                optionId = "opt_soc_1",
                voterId = "user_marcus",
                voterName = "Coach Marcus",
                timestamp = now - 200000,
                rsvpStatus = "GOING",
                plusGuests = 0,
                ratingValue = 5,
                feedbackComment = "I will bring match balls and bibs"
            ),
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = soccerPollId,
                optionId = "opt_soc_1",
                voterId = "user_david",
                voterName = "David K.",
                timestamp = now - 180000,
                rsvpStatus = "GOING",
                plusGuests = 1,
                ratingValue = 5,
                feedbackComment = "Bringing a +1 goalkeeper friend"
            ),
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = soccerPollId,
                optionId = "opt_soc_1",
                voterId = "user_sam",
                voterName = "Samir R.",
                timestamp = now - 150000,
                rsvpStatus = "GOING",
                plusGuests = 0,
                ratingValue = 4,
                feedbackComment = null
            ),
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = soccerPollId,
                optionId = "opt_soc_2",
                voterId = "user_elena",
                voterName = "Elena V.",
                timestamp = now - 120000,
                rsvpStatus = "GOING",
                plusGuests = 0,
                ratingValue = 3,
                feedbackComment = "Central park is walking distance for me"
            ),
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = soccerPollId,
                optionId = "opt_soc_2",
                voterId = "user_jordan",
                voterName = "Jordan P.",
                timestamp = now - 90000,
                rsvpStatus = "MAYBE",
                plusGuests = 0,
                ratingValue = 3,
                feedbackComment = "Depends on traffic after work"
            ),
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = soccerPollId,
                optionId = "opt_soc_3",
                voterId = "user_leo",
                voterName = "Leo N.",
                timestamp = now - 60000,
                rsvpStatus = "GOING",
                plusGuests = 0,
                ratingValue = 4,
                feedbackComment = "Sunday morning works best!"
            )
        )

        // 2. Friday Social Drinks (Group: Friday Social Club)
        val drinksPollId = "poll_drinks_002"
        val drinksPoll = PollEntity(
            id = drinksPollId,
            code = "DRK777",
            title = "Friday Happy Hour: Bar & Rooftop Selection",
            description = "Sprint complete! Vote on the venue for celebration drinks and let us know your RSVP count.",
            category = "DRINKS",
            categoryIcon = "🍻",
            creatorName = "Sarah Jenkins",
            createdAt = now - (5 * 3600 * 1000L),
            deadlineTimestamp = now + (1 * 3600 * 1000L + 15 * 60 * 1000L), // Closes in 1h 15m (Urgent deadline!)
            allowMultipleChoices = true,
            allowCustomOptions = true,
            isAnonymous = false,
            isClosed = false,
            targetHeadcount = 15,
            location = "Downtown District",
            groupId = "group_social",
            groupName = "Friday Social Club 🍻"
        )
        val drinksOptions = listOf(
            PollOptionEntity(
                id = "opt_drk_1",
                pollId = drinksPollId,
                text = "The Rusty Anchor Rooftop Bar",
                subtitle = "Great skyline view, $6 drafts till 7 PM",
                dateTimeSlot = "Friday 6:00 PM onwards",
                venueAddress = "500 High St, 8th Floor Rooftop",
                duration = "2.5 hours",
                priceRating = "$$",
                displayOrder = 1
            ),
            PollOptionEntity(
                id = "opt_drk_2",
                pollId = drinksPollId,
                text = "Copper & Oak Craft Brewery",
                subtitle = "Outdoor beer garden, board games & food truck",
                dateTimeSlot = "Friday 6:30 PM onwards",
                venueAddress = "72 Industrial Ave, Arts District",
                duration = "3 hours",
                priceRating = "$",
                displayOrder = 2
            ),
            PollOptionEntity(
                id = "opt_drk_3",
                pollId = drinksPollId,
                text = "Neon Social Lounge & Arcade",
                subtitle = "2-for-1 cocktails, retro pinball & pool tables",
                dateTimeSlot = "Friday 7:00 PM onwards",
                venueAddress = "18 Market Square, Downtown",
                duration = "2 hours",
                priceRating = "$$",
                displayOrder = 3
            )
        )
        val drinksVotes = listOf(
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = drinksPollId,
                optionId = "opt_drk_1",
                voterId = "user_sarah",
                voterName = "Sarah J.",
                timestamp = now - 250000,
                rsvpStatus = "GOING",
                plusGuests = 2,
                ratingValue = 5,
                feedbackComment = "Reserved a high-top table for us"
            ),
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = drinksPollId,
                optionId = "opt_drk_1",
                voterId = "user_tom",
                voterName = "Tom B.",
                timestamp = now - 220000,
                rsvpStatus = "GOING",
                plusGuests = 0,
                ratingValue = 5,
                feedbackComment = null
            ),
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = drinksPollId,
                optionId = "opt_drk_2",
                voterId = "user_priya",
                voterName = "Priya M.",
                timestamp = now - 180000,
                rsvpStatus = "GOING",
                plusGuests = 1,
                ratingValue = 4,
                feedbackComment = "Food truck has great vegetarian options"
            ),
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = drinksPollId,
                optionId = "opt_drk_2",
                voterId = "user_alex",
                voterName = "Alex R.",
                timestamp = now - 120000,
                rsvpStatus = "GOING",
                plusGuests = 0,
                ratingValue = 4,
                feedbackComment = null
            ),
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = drinksPollId,
                optionId = "opt_drk_3",
                voterId = "user_cloe",
                voterName = "Chloe T.",
                timestamp = now - 60000,
                rsvpStatus = "MAYBE",
                plusGuests = 0,
                ratingValue = 3,
                feedbackComment = "Might join for 1 hour after gym"
            )
        )

        // 3. Team Lunch (Group: Product & Engineering Crew)
        val lunchPollId = "poll_food_003"
        val lunchPoll = PollEntity(
            id = lunchPollId,
            code = "PIZZA9",
            title = "Team Pizza & Lunch Spot",
            description = "Quick vote for today's team lunch. Dietary requirements can be noted in feedback comments!",
            category = "FOOD",
            categoryIcon = "🍕",
            creatorName = "Alex (You)",
            createdAt = now - (1 * 3600 * 1000L),
            deadlineTimestamp = now + (30 * 60 * 1000L), // Closes in 30 mins
            allowMultipleChoices = false,
            allowCustomOptions = true,
            isAnonymous = false,
            isClosed = false,
            targetHeadcount = 8,
            location = "Midtown Square",
            groupId = "group_eng",
            groupName = "Product & Engineering Crew 💻"
        )
        val lunchOptions = listOf(
            PollOptionEntity(
                id = "opt_food_1",
                pollId = lunchPollId,
                text = "Luigi's Woodfired Pizzeria",
                subtitle = "Neapolitan pizza, gluten-free crust options",
                dateTimeSlot = "Today 12:30 PM",
                venueAddress = "44 Main St",
                duration = "1 hour",
                priceRating = "$$",
                displayOrder = 1
            ),
            PollOptionEntity(
                id = "opt_food_2",
                pollId = lunchPollId,
                text = "Tokyo Ramen & Dumpling House",
                subtitle = "Tonkotsu, vegetarian broth, fast service",
                dateTimeSlot = "Today 12:30 PM",
                venueAddress = "102 Station Plaza",
                duration = "45 mins",
                priceRating = "$",
                displayOrder = 2
            ),
            PollOptionEntity(
                id = "opt_food_3",
                pollId = lunchPollId,
                text = "Green Harvest Poke Bowls",
                subtitle = "Fresh salmon, tofu bowls, healthy & quick",
                dateTimeSlot = "Today 12:30 PM",
                venueAddress = "88 Green Court",
                duration = "45 mins",
                priceRating = "$$",
                displayOrder = 3
            )
        )
        val lunchVotes = listOf(
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = lunchPollId,
                optionId = "opt_food_1",
                voterId = "user_dev1",
                voterName = "Devon W.",
                timestamp = now - 50000,
                rsvpStatus = "GOING",
                plusGuests = 0,
                ratingValue = 5,
                feedbackComment = "Craving that truffle mushroom pizza"
            ),
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = lunchPollId,
                optionId = "opt_food_1",
                voterId = "user_dev2",
                voterName = "Nina Z.",
                timestamp = now - 40000,
                rsvpStatus = "GOING",
                plusGuests = 0,
                ratingValue = 4,
                feedbackComment = null
            ),
            VoteEntity(
                id = UUID.randomUUID().toString(),
                pollId = lunchPollId,
                optionId = "opt_food_2",
                voterId = "user_dev3",
                voterName = "Kenji T.",
                timestamp = now - 30000,
                rsvpStatus = "GOING",
                plusGuests = 0,
                ratingValue = 5,
                feedbackComment = null
            )
        )

        // Insert Polls & Options
        pollDao.insertPoll(soccerPoll)
        pollOptionDao.insertOptions(soccerOptions)
        voteDao.insertVotes(soccerVotes)

        pollDao.insertPoll(drinksPoll)
        pollOptionDao.insertOptions(drinksOptions)
        voteDao.insertVotes(drinksVotes)

        pollDao.insertPoll(lunchPoll)
        pollOptionDao.insertOptions(lunchOptions)
        voteDao.insertVotes(lunchVotes)

        // Seed Notifications
        val notifs = listOf(
            NotificationEntity(
                id = "notif_001",
                pollId = soccerPollId,
                pollTitle = soccerPoll.title,
                title = "📢 New Poll in Weekend Soccer Squad ⚽",
                message = "Coach Marcus created 'Saturday 5v5 Soccer: Pitch Venue & Kickoff Time'. Duration: 90 mins.",
                type = "POLL_CREATED",
                timestamp = now - (3 * 3600 * 1000L),
                isRead = false
            ),
            NotificationEntity(
                id = "notif_002",
                pollId = drinksPollId,
                pollTitle = drinksPoll.title,
                title = "⏰ Approaching Voting Deadline: Friday Happy Hour",
                message = "Only 1 hour left to vote for bar venue & RSVP. Leading choice: The Rusty Anchor Rooftop.",
                type = "DEADLINE_WARNING",
                timestamp = now - (30 * 60 * 1000L),
                isRead = false
            ),
            NotificationEntity(
                id = "notif_003",
                pollId = lunchPollId,
                pollTitle = lunchPoll.title,
                title = "🗳️ Vote Recorded: Devon W.",
                message = "RSVP: GOING for 'Team Pizza & Lunch Spot'.",
                type = "NEW_VOTE",
                timestamp = now - (50000L),
                isRead = false
            )
        )
        notificationDao.insertNotifications(notifs)
    }
}
