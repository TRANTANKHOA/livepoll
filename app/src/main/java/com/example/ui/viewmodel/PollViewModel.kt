package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AuthPreferences
import com.example.data.local.NotificationPreferences
import com.example.data.local.NotificationSettings
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.PollEntity
import com.example.data.local.entity.PollOptionEntity
import com.example.data.model.AuthProvider
import com.example.data.model.PollWithDetails
import com.example.data.model.UserAccount
import com.example.data.repository.PollRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class PollViewModel(
    private val repository: PollRepository,
    private val notificationPreferences: NotificationPreferences,
    private val authPreferences: AuthPreferences
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    val currentUser = MutableStateFlow(authPreferences.getCurrentUser())
    val currentVoterId = MutableStateFlow(currentUser.value.id)
    val currentVoterName = MutableStateFlow(currentUser.value.name)

    fun setCurrentUser(user: UserAccount) {
        currentUser.value = user
        currentVoterId.value = user.id
        currentVoterName.value = user.name
        authPreferences.saveUser(user)
    }

    fun switchUser(id: String, name: String, avatarEmoji: String = "😎") {
        currentVoterId.value = id
        currentVoterName.value = name
        val updatedUser = currentUser.value.copy(
            id = id,
            name = name,
            avatarEmoji = avatarEmoji
        )
        currentUser.value = updatedUser
        authPreferences.saveUser(updatedUser)
    }

    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow("ALL") // "ALL", "ACTIVE", "SOCCER", "DRINKS", "FOOD", "EVENT", "CLOSED"

    val allPolls: StateFlow<List<PollEntity>> = repository.allPolls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allGroups: StateFlow<List<GroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userJoinedGroups: StateFlow<List<GroupEntity>> = repository.userJoinedGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationSettings: StateFlow<NotificationSettings> = notificationPreferences.settings

    val filteredPolls: StateFlow<List<PollEntity>> = combine(
        allPolls,
        searchQuery,
        activeFilter
    ) { polls, query, filter ->
        val now = System.currentTimeMillis()
        polls.filter { poll ->
            val matchesQuery = query.isBlank() ||
                    poll.title.contains(query, ignoreCase = true) ||
                    poll.description.contains(query, ignoreCase = true) ||
                    poll.creatorName.contains(query, ignoreCase = true) ||
                    poll.code.contains(query, ignoreCase = true) ||
                    (poll.groupName?.contains(query, ignoreCase = true) == true) ||
                    (poll.location?.contains(query, ignoreCase = true) == true)

            val isExpired = poll.deadlineTimestamp != null && now > poll.deadlineTimestamp
            val isActive = !poll.isClosed && !isExpired

            val matchesFilter = when (filter) {
                "ALL" -> true
                "ACTIVE" -> isActive
                "CLOSED" -> poll.isClosed || isExpired
                "SOCCER" -> poll.category == "SOCCER"
                "DRINKS" -> poll.category == "DRINKS"
                "FOOD" -> poll.category == "FOOD"
                "EVENT" -> poll.category == "EVENT" || poll.category == "FEEDBACK"
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getPollDetails(pollId: String): Flow<PollWithDetails?> {
        return repository.getPollWithDetails(pollId, currentVoterId.value)
    }

    fun createPoll(
        title: String,
        category: String,
        categoryIcon: String,
        description: String,
        creatorName: String,
        options: List<PollOptionEntity>,
        deadlineHours: Int?,
        allowMultiple: Boolean,
        allowCustom: Boolean,
        isAnonymous: Boolean,
        targetHeadcount: Int?,
        location: String?,
        groupId: String?,
        groupName: String?,
        context: Context?,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val pollId = UUID.randomUUID().toString()
            val code = generatePollCode()
            val now = System.currentTimeMillis()
            val deadline = deadlineHours?.takeIf { it > 0 }?.let { now + (it * 3600 * 1000L) }

            val poll = PollEntity(
                id = pollId,
                code = code,
                title = title.trim(),
                description = description.trim(),
                category = category,
                categoryIcon = categoryIcon,
                creatorName = creatorName.trim().ifBlank { "Poll Host" },
                createdAt = now,
                deadlineTimestamp = deadline,
                allowMultipleChoices = allowMultiple,
                allowCustomOptions = allowCustom,
                isAnonymous = isAnonymous,
                isClosed = false,
                targetHeadcount = targetHeadcount,
                location = location?.trim()?.takeIf { it.isNotBlank() },
                groupId = groupId,
                groupName = groupName
            )

            val finalOptions = options.mapIndexed { idx, opt ->
                opt.copy(
                    id = opt.id.ifBlank { UUID.randomUUID().toString() },
                    pollId = pollId,
                    displayOrder = idx + 1
                )
            }

            repository.createPoll(poll, finalOptions)

            context?.let { ctx ->
                val notifTitle = if (!groupName.isNullOrBlank()) {
                    "📢 New Poll in $groupName"
                } else {
                    "🎉 New Poll Created: ${poll.title}"
                }

                val optionsSummary = finalOptions.firstOrNull()?.let {
                    "Option: ${it.text} (${it.duration ?: "Standard"})${if (it.venueAddress != null) " at ${it.venueAddress}" else ""}"
                } ?: "Voting is now open!"

                NotificationHelper.showNotification(
                    context = ctx,
                    notificationId = pollId.hashCode(),
                    title = notifTitle,
                    message = "${poll.title}. $optionsSummary. Code: ${poll.code}",
                    channelId = if (!groupName.isNullOrBlank()) NotificationHelper.CHANNEL_GROUP_POLLS else NotificationHelper.CHANNEL_DEADLINES,
                    pollId = pollId,
                    groupId = groupId
                )
            }

            onCreated(pollId)
        }
    }

    fun castVote(
        pollId: String,
        selectedOptionIds: List<String>,
        rsvpStatus: String,
        plusGuests: Int,
        rating: Int?,
        comment: String?,
        context: Context?,
        pollTitle: String? = null
    ) {
        viewModelScope.launch {
            repository.castVotes(
                pollId = pollId,
                voterId = currentVoterId.value,
                voterName = currentVoterName.value,
                selectedOptionIds = selectedOptionIds,
                rsvpStatus = rsvpStatus,
                plusGuests = plusGuests,
                rating = rating,
                comment = comment?.trim()?.takeIf { it.isNotBlank() }
            )

            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                pollId = pollId,
                pollTitle = pollTitle ?: "Poll Activity",
                title = "🗳️ Vote Registered (${currentVoterName.value})",
                message = "RSVP: $rsvpStatus ${if (plusGuests > 0) "(+$plusGuests)" else ""} for '${pollTitle ?: "poll"}'",
                type = "NEW_VOTE",
                timestamp = System.currentTimeMillis()
            )
            repository.insertNotification(notif)

            context?.let { ctx ->
                NotificationHelper.showNotification(
                    context = ctx,
                    notificationId = (pollId + currentVoterId.value).hashCode(),
                    title = "🗳️ Vote Recorded: ${currentVoterName.value}",
                    message = "Your response ($rsvpStatus) was successfully recorded for '${pollTitle ?: "poll"}'!",
                    channelId = NotificationHelper.CHANNEL_VOTES,
                    pollId = pollId
                )
            }
        }
    }

    fun addCustomOption(
        pollId: String,
        text: String,
        subtitle: String?,
        dateTime: String?,
        venueAddress: String?,
        duration: String?,
        priceRating: String?
    ) {
        viewModelScope.launch {
            val option = PollOptionEntity(
                id = UUID.randomUUID().toString(),
                pollId = pollId,
                text = text.trim(),
                subtitle = subtitle?.trim()?.takeIf { it.isNotBlank() },
                dateTimeSlot = dateTime?.trim()?.takeIf { it.isNotBlank() },
                venueAddress = venueAddress?.trim()?.takeIf { it.isNotBlank() },
                duration = duration?.trim()?.takeIf { it.isNotBlank() },
                priceRating = priceRating?.trim()?.takeIf { it.isNotBlank() },
                addedBy = currentVoterName.value,
                displayOrder = 99
            )
            repository.addOptionToPoll(option)
        }
    }

    fun togglePollStatus(pollId: String, isClosed: Boolean, context: Context?, pollTitle: String? = null) {
        viewModelScope.launch {
            repository.togglePollStatus(pollId, isClosed)
            if (isClosed && context != null) {
                NotificationHelper.showNotification(
                    context = context,
                    notificationId = pollId.hashCode(),
                    title = "🔒 Voting Ended",
                    message = "Voting has officially concluded for '${pollTitle ?: "this poll"}'. Final results are locked.",
                    channelId = NotificationHelper.CHANNEL_DEADLINES,
                    pollId = pollId
                )
            }
        }
    }

    fun deletePoll(pollId: String) {
        viewModelScope.launch {
            repository.deletePoll(pollId)
        }
    }

    fun findPollByCode(code: String, onResult: (PollEntity?) -> Unit) {
        viewModelScope.launch {
            val poll = repository.getPollByCode(code)
            onResult(poll)
        }
    }

    fun toggleGroupMembership(groupId: String, isMember: Boolean) {
        viewModelScope.launch {
            repository.setGroupMembership(groupId, isMember)
        }
    }

    fun toggleGroupNotifications(groupId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setGroupNotifications(groupId, enabled)
            notificationPreferences.toggleGroupMute(groupId, !enabled)
        }
    }

    fun updateMasterPush(enabled: Boolean) {
        notificationPreferences.updateMasterPush(enabled)
    }

    fun updateGroupPollsAlerts(enabled: Boolean) {
        notificationPreferences.updateGroupPollsAlerts(enabled)
    }

    fun updateApproachingDeadlines(enabled: Boolean) {
        notificationPreferences.updateApproachingDeadlines(enabled)
    }

    fun updateDeadlineLeadTime(hours: Int) {
        notificationPreferences.updateDeadlineLeadTime(hours)
    }

    fun updateVibration(enabled: Boolean) {
        notificationPreferences.updateVibration(enabled)
    }

    fun updateVoteActivityAlerts(enabled: Boolean) {
        notificationPreferences.updateVoteActivityAlerts(enabled)
    }

    fun checkApproachingDeadlines(context: Context, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val leadTime = notificationSettings.value.deadlineLeadTimeHours
            val count = repository.checkApproachingDeadlines(context, leadTime)
            onComplete(count)
        }
    }

    fun testGroupPollNotification(context: Context) {
        viewModelScope.launch {
            val sampleGroup = userJoinedGroups.value.firstOrNull() ?: allGroups.value.firstOrNull()
            val groupName = sampleGroup?.name ?: "Weekend Soccer Squad ⚽"
            val groupId = sampleGroup?.id ?: "group_soccer"

            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                pollId = "poll_soccer_001",
                pollTitle = "Saturday 5v5 Soccer",
                title = "📢 New Poll in $groupName",
                message = "Saturday 5v5 Match: 90 mins slot at Riverside Turf. Voting is now open!",
                type = "POLL_CREATED",
                timestamp = System.currentTimeMillis()
            )
            repository.insertNotification(notif)

            NotificationHelper.showNotification(
                context = context,
                notificationId = UUID.randomUUID().hashCode(),
                title = "📢 New Poll in $groupName",
                message = "Saturday 5v5 Soccer (90 mins). Kickoff time & pitch venue choices open for voting!",
                channelId = NotificationHelper.CHANNEL_GROUP_POLLS,
                pollId = "poll_soccer_001",
                groupId = groupId,
                bypassUserPrefs = false
            )
        }
    }

    fun testDeadlineNotification(context: Context) {
        viewModelScope.launch {
            val samplePoll = allPolls.value.firstOrNull { !it.isClosed } ?: allPolls.value.firstOrNull()
            val title = samplePoll?.title ?: "Friday Happy Hour & Social Drinks"
            val leadHours = notificationSettings.value.deadlineLeadTimeHours

            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                pollId = samplePoll?.id ?: "poll_drinks_002",
                pollTitle = title,
                title = "⏰ Approaching Voting Deadline ($leadHours hrs left)",
                message = "'$title' voting closes soon. Cast your vote for venue & time before the deadline!",
                type = "DEADLINE_WARNING",
                timestamp = System.currentTimeMillis()
            )
            repository.insertNotification(notif)

            NotificationHelper.showNotification(
                context = context,
                notificationId = UUID.randomUUID().hashCode(),
                title = "⏰ Approaching Voting Deadline ($leadHours hrs left)",
                message = "Only $leadHours hours remaining to cast your vote on '$title'. Tap to pick your venue & time!",
                channelId = NotificationHelper.CHANNEL_DEADLINES,
                pollId = samplePoll?.id ?: "poll_drinks_002",
                bypassUserPrefs = false
            )
        }
    }

    fun simulateDeadlinePushAlert(details: PollWithDetails, context: Context) {
        viewModelScope.launch {
            val poll = details.poll
            val leadHours = notificationSettings.value.deadlineLeadTimeHours
            val notifTitle = "⏰ Voting Deadline Alert: ${poll.title}"
            val leadingOption = details.winningOption?.text ?: "your favorite venue"
            val notifMsg = "Voting closes soon! Current leading venue: '$leadingOption'. Cast your vote before time runs out."

            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                pollId = poll.id,
                pollTitle = poll.title,
                title = "⏰ Approaching Deadline ($leadHours hrs)",
                message = notifMsg,
                type = "DEADLINE_WARNING",
                timestamp = System.currentTimeMillis()
            )
            repository.insertNotification(notif)

            NotificationHelper.showNotification(
                context = context,
                notificationId = (poll.id + "_simulated_deadline").hashCode(),
                title = notifTitle,
                message = notifMsg,
                channelId = NotificationHelper.CHANNEL_DEADLINES,
                pollId = poll.id,
                bypassUserPrefs = false
            )
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    private fun generatePollCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}
