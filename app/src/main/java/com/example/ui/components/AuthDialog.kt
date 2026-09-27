package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthProvider
import com.example.ui.viewmodel.PollViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthDialog(
    viewModel: PollViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    var isAuthenticating by remember { mutableStateOf(false) }
    var statusSuccessMessage by remember { mutableStateOf<String?>(null) }
    var statusErrorMessage by remember { mutableStateOf<String?>(null) }

    fun signInWithGoogle() {
        isAuthenticating = true
        statusErrorMessage = null
        viewModel.signInWithGoogle(context) { success, message ->
            isAuthenticating = false
            if (success) {
                statusSuccessMessage = message
                scope.launch {
                    delay(800)
                    onDismiss()
                }
            } else {
                // Real failure: surface it. Never fabricate an account.
                statusErrorMessage = message.ifBlank {
                    "Google Sign-In failed. Check that WEB_CLIENT_ID (.env) and the " +
                        "app's SHA-1 fingerprint are configured in Firebase."
                }
            }
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
                            text = if (currentUser.provider == AuthProvider.GUEST) "Sign In" else "Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Google Sign-In",
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
                // Current account summary
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
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentUser.email?.ifBlank { null } ?: "Guest mode (no account)",
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

                when {
                    isAuthenticating -> {
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
                                    text = "Opening Google Sign-In…",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    statusSuccessMessage != null -> {
                        StatusBanner(
                            message = statusSuccessMessage!!,
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    statusErrorMessage != null -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatusBanner(
                                message = statusErrorMessage!!,
                                container = MaterialTheme.colorScheme.errorContainer,
                                content = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(
                                onClick = { signInWithGoogle() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Try Again", fontSize = 12.sp)
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = "Sign in to sync your votes and access group polls across devices:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Google Sign-In (the only configured provider)
                        Surface(
                            onClick = { signInWithGoogle() },
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
                                    text = if (currentUser.provider == AuthProvider.GOOGLE) "Switch Google Account" else "Continue with Google",
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

                        if (currentUser.provider != AuthProvider.GUEST) {
                            TextButton(
                                onClick = {
                                    viewModel.logoutUser(context)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sign Out", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
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

@Composable
private fun StatusBanner(message: String, container: Color, content: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = message,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = content
        )
    }
}
