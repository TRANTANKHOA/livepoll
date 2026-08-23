package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AuthPreferences
import com.example.data.local.NotificationPreferences
import com.example.data.repository.PollRepository

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
