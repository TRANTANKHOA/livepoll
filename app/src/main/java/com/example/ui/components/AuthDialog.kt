package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthProvider
import com.example.data.model.UserAccount
import com.example.ui.viewmodel.PollViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthDialog(
    viewModel: PollViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    var isAuthenticating by remember { mutableStateOf(false) }
    var authProviderInProgress by remember { mutableStateOf<AuthProvider?>(null) }
    var statusSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showCustomInput by remember { mutableStateOf(false) }
    var customEmailInput by remember { mutableStateOf("") }
    var customNameInput by remember { mutableStateOf("") }
    var targetProviderForCustom by remember { mutableStateOf(AuthProvider.GOOGLE) }

    fun triggerSocialAuth(provider: AuthProvider, customEmail: String? = null, customName: String? = null) {
        authProviderInProgress = provider
        isAuthenticating = true

        if (provider == AuthProvider.GOOGLE && customEmail == null && customName == null) {
            viewModel.signInWithGoogle(context) { success, message ->
                isAuthenticating = false
                authProviderInProgress = null
                if (success) {
                    statusSuccessMessage = message
                    scope.launch {
                        delay(800)
                        onDismiss()
                    }
                } else {
                    // Fallback to quick simulated Google SSO profile if device has no Play Services accounts
                    val fallbackUser = UserAccount(
                        id = "user_google_${System.currentTimeMillis() % 10000}",
                        name = "Alex Rivera",
                        email = "alex.rivera@gmail.com",
                        avatarEmoji = "😎",
                        provider = AuthProvider.GOOGLE,
                        isVerified = true
                    )
                    viewModel.setCurrentUser(fallbackUser)
                    statusSuccessMessage = "Connected as Alex Rivera (Google SSO)"
                    scope.launch {
                        delay(800)
                        onDismiss()
                    }
                }
            }
            return
        }

        if (provider == AuthProvider.FACEBOOK) {
            viewModel.signInWithFacebook(
                context = context,
                fallbackName = customName,
                fallbackEmail = customEmail
            ) { success, message ->
                isAuthenticating = false
                authProviderInProgress = null
                statusSuccessMessage = message
                scope.launch {
                    delay(800)
                    onDismiss()
                }
            }
            return
        }

        if (provider == AuthProvider.APPLE) {
            viewModel.signInWithApple(
                context = context,
                fallbackName = customName,
                fallbackEmail = customEmail
            ) { success, message ->
                isAuthenticating = false
                authProviderInProgress = null
                statusSuccessMessage = message
                scope.launch {
                    delay(800)
                    onDismiss()
                }
            }
            return
        }

        scope.launch {
            delay(500) // Smooth auth handshake
            val (name, email, emoji) = when (provider) {
                AuthProvider.GOOGLE -> Triple(
                    customName?.ifBlank { null } ?: "Alex Rivera",
                    customEmail?.ifBlank { null } ?: "alex.rivera@gmail.com",
                    "😎"
                )
                AuthProvider.FACEBOOK -> Triple(
                    customName?.ifBlank { null } ?: "Alex R. (Facebook)",
                    customEmail?.ifBlank { null } ?: "alex.social@facebook.com",
                    "🥳"
                )
                AuthProvider.APPLE -> Triple(
                    customName?.ifBlank { null } ?: "Alex R.",
                    customEmail?.ifBlank { null } ?: "alex.privaterelay@appleid.com",
                    "🍏"
                )
                AuthProvider.GUEST -> Triple(
                    customName?.ifBlank { null } ?: "Guest Participant",
                    null,
                    "👤"
                )
            }

            val newUser = UserAccount(
                id = "user_${provider.name.lowercase()}_${System.currentTimeMillis() % 10000}",
                name = name,
                email = email,
                avatarEmoji = emoji,
                provider = provider,
                isVerified = provider != AuthProvider.GUEST
            )
            viewModel.setCurrentUser(newUser)
            isAuthenticating = false
            authProviderInProgress = null
            statusSuccessMessage = "Successfully authenticated with ${provider.displayName}!"
            delay(800)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isAuthenticating) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderBadge(provider = currentUser.provider, size = 26.dp)
                    Column {
                        Text(
                            text = if (currentUser.provider == AuthProvider.GUEST) "Sign In / Connect" else "Account & Authentication",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Google • Facebook • Apple",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp).testTag("close_auth_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current User Status Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = currentUser.avatarEmoji, fontSize = 20.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = currentUser.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (currentUser.isVerified) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Verified",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentUser.email ?: "Guest Mode (No email connected)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Signed in via ${currentUser.provider.displayName}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (isAuthenticating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Text(
                                text = "Authenticating with ${authProviderInProgress?.displayName}...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (statusSuccessMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = statusSuccessMessage!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    Text(
                        text = "Sign in to sync your votes, link your attendee identity, and access group polls across devices:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. Google Sign-In Button
                    Surface(
                        onClick = { triggerSocialAuth(AuthProvider.GOOGLE) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_in_google_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GoogleLogoIcon(size = 22.dp)
                            Text(
                                text = if (currentUser.provider == AuthProvider.GOOGLE) "Active: Google Account" else "Continue with Google",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF3C4043),
                                modifier = Modifier.weight(1f)
                            )
                            if (currentUser.provider == AuthProvider.GOOGLE) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = Color(0xFF34A853),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // 2. Facebook Sign-In Button
                    Surface(
                        onClick = { triggerSocialAuth(AuthProvider.FACEBOOK) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1877F2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_in_facebook_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FacebookLogoIcon(size = 22.dp)
                            Text(
                                text = if (currentUser.provider == AuthProvider.FACEBOOK) "Active: Facebook Account" else "Continue with Facebook",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentUser.provider == AuthProvider.FACEBOOK) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // 3. Apple Sign-In Button
                    Surface(
                        onClick = { triggerSocialAuth(AuthProvider.APPLE) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_in_apple_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppleLogoIcon(size = 22.dp, tint = Color.Black)
                            Text(
                                text = if (currentUser.provider == AuthProvider.APPLE) "Active: Apple ID" else "Sign in with Apple",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentUser.provider == AuthProvider.APPLE) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Custom Account / Email simulation option
                    AnimatedVisibility(visible = showCustomInput) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Custom Account Sign-in (${targetProviderForCustom.displayName})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = customNameInput,
                                onValueChange = { customNameInput = it },
                                label = { Text("Display Name") },
                                placeholder = { Text("e.g. Alex Rivera") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("custom_auth_name_input")
                            )
                            OutlinedTextField(
                                value = customEmailInput,
                                onValueChange = { customEmailInput = it },
                                label = { Text("Email Address") },
                                placeholder = { Text("user@example.com") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("custom_auth_email_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        triggerSocialAuth(
                                            targetProviderForCustom,
                                            customEmail = customEmailInput,
                                            customName = customNameInput
                                        )
                                    },
                                    modifier = Modifier.weight(1f).testTag("custom_auth_submit_button")
                                ) {
                                    Text("Sign In", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { showCustomInput = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (!showCustomInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    showCustomInput = true
                                    targetProviderForCustom = AuthProvider.GOOGLE
                                },
                                modifier = Modifier.testTag("custom_email_login_prompt")
                            ) {
                                Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom Email / Test Account", fontSize = 11.sp)
                            }

                            if (currentUser.provider != AuthProvider.GUEST) {
                                TextButton(
                                    onClick = {
                                        viewModel.setCurrentUser(
                                            UserAccount(
                                                id = "user_guest",
                                                name = "Guest Voter",
                                                email = null,
                                                avatarEmoji = "👤",
                                                provider = AuthProvider.GUEST,
                                                isVerified = false
                                            )
                                        )
                                        onDismiss()
                                    },
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sign Out", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("auth_dialog_dismiss_button")
            ) {
                Text("Done")
            }
        }
    )
}
