package com.plutusoft.livepulse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.plutusoft.livepulse.data.local.AuthPreferences
import com.plutusoft.livepulse.data.local.NotificationPreferences
import com.plutusoft.livepulse.data.repository.PollRepository

class PollViewModelFactory(
    private val repository: PollRepository,
    private val notificationPreferences: NotificationPreferences,
    private val authPreferences: AuthPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PollViewModel::class.java)) {
            return PollViewModel(repository, notificationPreferences, authPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
