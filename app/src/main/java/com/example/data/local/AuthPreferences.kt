package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AuthProvider
import com.example.data.model.UserAccount

class AuthPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pulsepoll_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USER_NAME = "auth_user_name"
        private const val KEY_USER_EMAIL = "auth_user_email"
        private const val KEY_USER_AVATAR = "auth_user_avatar"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_IS_VERIFIED = "auth_is_verified"
        private const val KEY_LOGGED_IN_TIME = "auth_logged_in_time"
    }

    fun getCurrentUser(): UserAccount {
        val id = prefs.getString(KEY_USER_ID, "user_google_alex") ?: "user_google_alex"
        val name = prefs.getString(KEY_USER_NAME, "Alex Rivera") ?: "Alex Rivera"
        val email = prefs.getString(KEY_USER_EMAIL, "alex.rivera@gmail.com")
        val avatar = prefs.getString(KEY_USER_AVATAR, "😎") ?: "😎"
        val providerStr = prefs.getString(KEY_AUTH_PROVIDER, AuthProvider.GOOGLE.name) ?: AuthProvider.GOOGLE.name
        val provider = try {
            AuthProvider.valueOf(providerStr)
        } catch (e: Exception) {
            AuthProvider.GOOGLE
        }
        val isVerified = prefs.getBoolean(KEY_IS_VERIFIED, true)
        val loggedInTime = prefs.getLong(KEY_LOGGED_IN_TIME, System.currentTimeMillis())

        return UserAccount(
            id = id,
            name = name,
            email = email,
            avatarEmoji = avatar,
            provider = provider,
            isVerified = isVerified,
            linkedProviders = listOf(provider),
            loggedInTimestamp = loggedInTime
        )
    }

    fun saveUser(user: UserAccount) {
        prefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_AVATAR, user.avatarEmoji)
            .putString(KEY_AUTH_PROVIDER, user.provider.name)
            .putBoolean(KEY_IS_VERIFIED, user.isVerified)
            .putLong(KEY_LOGGED_IN_TIME, user.loggedInTimestamp)
            .apply()
    }

    fun clearUser() {
        prefs.edit().clear().apply()
    }
}
