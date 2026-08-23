package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthProvider
import com.example.data.model.UserAccount
import com.example.ui.viewmodel.PollViewModel

data class DemoProfile(
    val id: String,
    val name: String,
    val email: String,
    val avatarEmoji: String,
    val provider: AuthProvider
)

@Composable
fun UserSwitcherDialog(
    viewModel: PollViewModel,
    onOpenAuthDialog: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    val profiles = listOf(
        DemoProfile("user_google_alex", "Alex Rivera (You)", "alex.rivera@gmail.com", "😎", AuthProvider.GOOGLE),
        DemoProfile("user_fb_sarah", "Sarah Jenkins", "sarah.j@facebook.com", "🍹", AuthProvider.FACEBOOK),
        DemoProfile("user_apple_marcus", "Marcus Chen", "marcus@appleid.com", "⚽", AuthProvider.APPLE),
        DemoProfile("voter_leo", "Leo Hernandez", "leo.h@gmail.com", "🎯", AuthProvider.GOOGLE),
        DemoProfile("voter_emma", "Emma Watson", "emma.w@icloud.com", "✨", AuthProvider.APPLE),
        DemoProfile("voter_david", "David Miller", "david.m@facebook.com", "🚀", AuthProvider.FACEBOOK)
    )

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
                        text = "Participant Account & Identity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Google • Facebook • Apple SSO",
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
                // Quick Social Sign In CTA Card
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
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            GoogleLogoIcon(size = 18.dp)
                            FacebookLogoIcon(size = 18.dp)
                            AppleLogoIcon(size = 18.dp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sign in / Manage SSO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Google, Facebook or Apple account",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Manage ›",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "Or switch attendee profile to test quorum, multi-user consensus & live votes:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                profiles.forEach { profile ->
                    val isSelected = currentUser.id == profile.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .clickable {
                                viewModel.setCurrentUser(
                                    UserAccount(
                                        id = profile.id,
                                        name = profile.name,
                                        email = profile.email,
                                        avatarEmoji = profile.avatarEmoji,
                                        provider = profile.provider,
                                        isVerified = true
                                    )
                                )
                                onDismiss()
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = profile.avatarEmoji, fontSize = 18.sp)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = profile.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    ProviderBadge(provider = profile.provider, size = 12.dp)
                                }
                                Text(
                                    text = profile.email,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (isAddingCustom) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Enter Attendee Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_voter_name_input")
                    )
                    Button(
                        onClick = {
                            if (customName.isNotBlank()) {
                                val newId = "user_" + customName.trim().lowercase().replace(" ", "_")
                                viewModel.setCurrentUser(
                                    UserAccount(
                                        id = newId,
                                        name = customName.trim(),
                                        email = "${customName.trim().lowercase().replace(" ", "")}@domain.com",
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
                        Text("Switch to ${customName.ifBlank { "Custom Attendee" }}")
                    }
                } else {
                    TextButton(
                        onClick = { isAddingCustom = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Custom Friend / Voter Name", fontSize = 12.sp)
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
