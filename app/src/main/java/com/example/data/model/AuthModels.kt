package com.example.data.model

enum class AuthProvider(val displayName: String, val iconEmoji: String) {
    GOOGLE("Google", "🌐"),
    FACEBOOK("Facebook", "📘"),
    APPLE("Apple", "🍎"),
    GUEST("Guest", "👤")
}

data class UserAccount(
    val id: String,
    val name: String,
    val email: String? = null,
    val avatarEmoji: String = "😎",
    val photoUrl: String? = null,
    val provider: AuthProvider = AuthProvider.GUEST,
    val isVerified: Boolean = true,
    val linkedProviders: List<AuthProvider> = listOf(provider),
    val loggedInTimestamp: Long = System.currentTimeMillis()
)
