package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.model.AuthProvider
import com.example.data.model.UserAccount
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

object GoogleAuthHelper {

    private const val TAG = "GoogleAuthHelper"

    /**
     * Signs in with Google using Jetpack CredentialManager and links to Firebase Auth.
     */
    suspend fun signInWithGoogle(
        context: Context,
        webClientId: String? = null
    ): Result<UserAccount> {
        return try {
            val credentialManager = CredentialManager.create(context)

            // Web client ID can be passed from BuildConfig / Secrets or default
            val clientId = if (!webClientId.isNullOrBlank()) {
                webClientId
            } else {
                // Fallback default client ID placeholder or app identifier
                "pulsepoll-firebase-auth.apps.googleusercontent.com"
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                // Authenticate with Firebase Auth if Firebase is initialized
                try {
                    if (FirebaseApp.getApps(context).isNotEmpty()) {
                        val auth = FirebaseAuth.getInstance()
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(firebaseCredential).await()
                        val firebaseUser = authResult.user
                        Log.d(TAG, "Firebase Auth successful for: ${firebaseUser?.email}")
                    }
                } catch (fbEx: Exception) {
                    Log.w(TAG, "Firebase Auth sign-in warning: ${fbEx.message}")
                }

                val userAccount = UserAccount(
                    id = "google_" + (email.replace(Regex("[^a-zA-Z0-9]"), "_")),
                    name = displayName,
                    email = email,
                    avatarEmoji = "🌟",
                    provider = AuthProvider.GOOGLE,
                    isVerified = true,
                    linkedProviders = listOf(AuthProvider.GOOGLE),
                    loggedInTimestamp = System.currentTimeMillis()
                )

                Result.success(userAccount)
            } else {
                Result.failure(Exception("Unsupported credential type returned: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled Google Sign-In dialog")
            Result.failure(e)
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts available on device: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs out of Firebase Auth and clears cloud session.
     */
    fun signOut(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance().signOut()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error signing out of Firebase: ${e.message}")
        }
    }
}
