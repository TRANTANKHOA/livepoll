package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.local.AuthPreferences
import com.example.data.local.NotificationPreferences
import com.example.data.model.PollTemplates
import com.example.data.repository.PollRepository
import com.example.ui.screens.CreatePollScreen
import com.example.ui.screens.NotificationSettingsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PollAnalyticsScreen
import com.example.ui.screens.PollListScreen
import com.example.ui.screens.VotingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PollViewModel
import com.example.ui.viewmodel.PollViewModelFactory
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val viewModel: PollViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PollRepository(
            pollDao = database.pollDao(),
            pollOptionDao = database.pollOptionDao(),
            voteDao = database.voteDao(),
            notificationDao = database.notificationDao(),
            groupDao = database.groupDao()
        )
        val notifPrefs = NotificationPreferences(applicationContext)
        val authPrefs = AuthPreferences(applicationContext)
        PollViewModelFactory(repository, notifPrefs, authPrefs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannels(this)
        com.example.util.SecurityDefenseHelper.initDeviceAttestation(this)

        val pollIdFromIntent = intent?.getStringExtra("EXTRA_POLL_ID")

        setContent {
            MyApplicationTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PulsePollApp(
                        viewModel = viewModel,
                        initialPollId = pollIdFromIntent
                    )
                }
            }
        }
    }
}

@Composable
fun PulsePollApp(
    viewModel: PollViewModel,
    initialPollId: String? = null
) {
    val navController = rememberNavController()

    LaunchedEffect(initialPollId) {
        if (!initialPollId.isNullOrBlank()) {
            navController.navigate("analytics_poll/$initialPollId")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "poll_list"
    ) {
        composable("poll_list") {
            PollListScreen(
                viewModel = viewModel,
                onCreatePollClick = { template ->
                    if (template != null) {
                        navController.navigate("create_poll?templateId=${template.id}")
                    } else {
                        navController.navigate("create_poll")
                    }
                },
                onVotePollClick = { pollId ->
                    navController.navigate("vote_poll/$pollId")
                },
                onAnalyticsPollClick = { pollId ->
                    navController.navigate("analytics_poll/$pollId")
                },
                onNotificationsClick = {
                    navController.navigate("notifications")
                }
            )
        }

        composable(
            route = "create_poll?templateId={templateId}",
            arguments = listOf(
                navArgument("templateId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId")
            val template = PollTemplates.templates.find { it.id == templateId }

            CreatePollScreen(
                viewModel = viewModel,
                initialTemplate = template,
                onNavigateBack = { navController.popBackStack() },
                onPollCreated = { newPollId ->
                    navController.navigate("analytics_poll/$newPollId") {
                        popUpTo("poll_list")
                    }
                }
            )
        }

        composable(
            route = "vote_poll/{pollId}",
            arguments = listOf(
                navArgument("pollId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pollId = backStackEntry.arguments?.getString("pollId") ?: ""
            VotingScreen(
                pollId = pollId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAnalytics = { targetPollId ->
                    navController.navigate("analytics_poll/$targetPollId") {
                        popUpTo("poll_list")
                    }
                }
            )
        }

        composable(
            route = "analytics_poll/{pollId}",
            arguments = listOf(
                navArgument("pollId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pollId = backStackEntry.arguments?.getString("pollId") ?: ""
            PollAnalyticsScreen(
                pollId = pollId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVote = { targetPollId ->
                    navController.navigate("vote_poll/$targetPollId")
                }
            )
        }

        composable("notifications") {
            NotificationsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = {
                    navController.navigate("notification_settings")
                },
                onPollClick = { pollId ->
                    navController.navigate("analytics_poll/$pollId")
                }
            )
        }

        composable("notification_settings") {
            NotificationSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
