package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.local.NotificationPreferences

object NotificationHelper {
    const val CHANNEL_GROUP_POLLS = "pulsepoll_group_polls_channel"
    const val CHANNEL_DEADLINES = "pulsepoll_deadlines_channel"
    const val CHANNEL_VOTES = "pulsepoll_votes_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val groupPollChannel = NotificationChannel(
                CHANNEL_GROUP_POLLS,
                "New Group Polls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when new polls are posted in your groups"
                enableVibration(true)
            }

            val deadlineChannel = NotificationChannel(
                CHANNEL_DEADLINES,
                "Approaching Voting Deadlines",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders when active poll voting deadlines are approaching"
                enableVibration(true)
            }

            val votesChannel = NotificationChannel(
                CHANNEL_VOTES,
                "Poll Activity & Votes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates on new votes and RSVPs"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(groupPollChannel)
            manager?.createNotificationChannel(deadlineChannel)
            manager?.createNotificationChannel(votesChannel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        channelId: String = CHANNEL_DEADLINES,
        pollId: String? = null,
        groupId: String? = null,
        bypassUserPrefs: Boolean = false
    ): Boolean {
        createNotificationChannels(context)

        // Check user preferences
        if (!bypassUserPrefs) {
            val prefs = NotificationPreferences(context)
            val settings = prefs.settings.value

            if (!settings.masterPushEnabled) {
                return false
            }

            when (channelId) {
                CHANNEL_GROUP_POLLS -> {
                    if (!settings.groupPollsAlertsEnabled) return false
                    if (groupId != null && settings.mutedGroupIds.contains(groupId)) return false
                }
                CHANNEL_DEADLINES -> {
                    if (!settings.approachingDeadlinesEnabled) return false
                }
                CHANNEL_VOTES -> {
                    if (!settings.voteActivityAlertsEnabled) return false
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            pollId?.let { putExtra("EXTRA_POLL_ID", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val prefs = NotificationPreferences(context)
        if (prefs.settings.value.vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 250, 150, 250))
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            return true
        } catch (e: SecurityException) {
            return false
        }
    }
}
