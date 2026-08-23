package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationSettings(
    val masterPushEnabled: Boolean = true,
    val groupPollsAlertsEnabled: Boolean = true,
    val approachingDeadlinesEnabled: Boolean = true,
    val deadlineLeadTimeHours: Int = 2, // 1, 2, 4, 12, 24 hours
    val vibrationEnabled: Boolean = true,
    val voteActivityAlertsEnabled: Boolean = true,
    val mutedGroupIds: Set<String> = emptySet()
)

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pulsepoll_notif_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()

    private fun loadSettings(): NotificationSettings {
        return NotificationSettings(
            masterPushEnabled = prefs.getBoolean("master_push_enabled", true),
            groupPollsAlertsEnabled = prefs.getBoolean("group_polls_alerts_enabled", true),
            approachingDeadlinesEnabled = prefs.getBoolean("approaching_deadlines_enabled", true),
            deadlineLeadTimeHours = prefs.getInt("deadline_lead_time_hours", 2),
            vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
            voteActivityAlertsEnabled = prefs.getBoolean("vote_activity_alerts_enabled", true),
            mutedGroupIds = prefs.getStringSet("muted_group_ids", emptySet()) ?: emptySet()
        )
    }

    fun updateMasterPush(enabled: Boolean) {
        prefs.edit().putBoolean("master_push_enabled", enabled).apply()
        _settings.value = _settings.value.copy(masterPushEnabled = enabled)
    }

    fun updateGroupPollsAlerts(enabled: Boolean) {
        prefs.edit().putBoolean("group_polls_alerts_enabled", enabled).apply()
        _settings.value = _settings.value.copy(groupPollsAlertsEnabled = enabled)
    }

    fun updateApproachingDeadlines(enabled: Boolean) {
        prefs.edit().putBoolean("approaching_deadlines_enabled", enabled).apply()
        _settings.value = _settings.value.copy(approachingDeadlinesEnabled = enabled)
    }

    fun updateDeadlineLeadTime(hours: Int) {
        prefs.edit().putInt("deadline_lead_time_hours", hours).apply()
        _settings.value = _settings.value.copy(deadlineLeadTimeHours = hours)
    }

    fun updateVibration(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
        _settings.value = _settings.value.copy(vibrationEnabled = enabled)
    }

    fun updateVoteActivityAlerts(enabled: Boolean) {
        prefs.edit().putBoolean("vote_activity_alerts_enabled", enabled).apply()
        _settings.value = _settings.value.copy(voteActivityAlertsEnabled = enabled)
    }

    fun toggleGroupMute(groupId: String, mute: Boolean) {
        val current = _settings.value.mutedGroupIds.toMutableSet()
        if (mute) {
            current.add(groupId)
        } else {
            current.remove(groupId)
        }
        prefs.edit().putStringSet("muted_group_ids", current).apply()
        _settings.value = _settings.value.copy(mutedGroupIds = current)
    }

    fun isGroupMuted(groupId: String): Boolean {
        return _settings.value.mutedGroupIds.contains(groupId)
    }
}
