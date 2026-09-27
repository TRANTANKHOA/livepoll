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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthProvider
import com.example.data.model.UserAccount
import com.example.ui.viewmodel.PollViewModel

@Composable
fun UserSwitcherDialog(
    viewModel: PollViewModel,
    onOpenAuthDialog: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    var customName by remember { mutableStateOf("") }
    var isAddingCustom by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProviderBadge(provider = currentUser.provider, size = 26.dp)
                Column {
                    Text(
                        text = "Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Google Sign-In or join as a guest",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Current account
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = currentUser.avatarEmoji, fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = currentUser.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (currentUser.isVerified) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentUser.email?.ifBlank { null }
                                    ?: "Guest — votes stay on this device",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (currentUser.provider != AuthProvider.GUEST) {
                            TextButton(
                                onClick = {
                                    viewModel.logoutUser(context)
                                    onDismiss()
                                },
                                modifier = Modifier.testTag("switcher_sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Sign in with Google (the only configured provider)
                Surface(
                    onClick = {
                        onDismiss()
                        onOpenAuthDialog()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().testTag("social_sso_card_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GoogleLogoIcon(size = 18.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sign in with Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Sync votes across devices",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "›",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Join with a display name (local guest identity, no fabricated email)
                if (isAddingCustom) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Display name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_voter_name_input")
                    )
                    Button(
                        onClick = {
                            if (customName.isNotBlank()) {
                                val name = customName.trim()
                                viewModel.setCurrentUser(
                                    UserAccount(
                                        id = "guest_" + name.lowercase().replace(Regex("[^a-z0-9]"), "_"),
                                        name = name,
                                        email = "",
                                        avatarEmoji = "👤",
                                        provider = AuthProvider.GUEST,
                                        isVerified = false
                                    )
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("add_custom_voter_confirm_button")
                    ) {
                        Text("Join as \"$customName\"")
                    }
                } else {
                    TextButton(
                        onClick = { isAddingCustom = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Join with a display name", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
