package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.AuthPreferences
import com.example.data.local.NotificationPreferences
import com.example.data.repository.PollRepository
import com.example.ui.screens.CreatePollScreen
import com.example.ui.screens.PollAnalyticsScreen
import com.example.ui.screens.PollListScreen
import com.example.ui.screens.VotingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PollViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * UX review harness: renders the core screens with first-run (seeded) data
 * so they can be inspected as static images without an emulator.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class UxCatalogScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun buildDb(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
    }

    private fun buildSeededViewModel(): PollViewModel {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = buildDb()
        val repository = PollRepository(
            pollDao = db.pollDao(),
            pollOptionDao = db.pollOptionDao(),
            voteDao = db.voteDao(),
            notificationDao = db.notificationDao(),
            groupDao = db.groupDao()
        )
        runBlocking { repository.ensureSeeded() }
        return PollViewModel(
            repository,
            NotificationPreferences(context),
            AuthPreferences(context)
        )
    }

    @Test
    fun ux_pollListScreen_empty() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = buildDb()
        val repository = PollRepository(
            pollDao = db.pollDao(),
            pollOptionDao = db.pollOptionDao(),
            voteDao = db.voteDao(),
            notificationDao = db.notificationDao(),
            groupDao = db.groupDao()
        )
        val vm = PollViewModel(repository, NotificationPreferences(context), AuthPreferences(context))
        composeTestRule.setContent {
            MyApplicationTheme {
                PollListScreen(
                    viewModel = vm,
                    onCreatePollClick = {},
                    onVotePollClick = {},
                    onAnalyticsPollClick = {},
                    onNotificationsClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ux_poll_list_empty.png")
    }

    @Test
    fun ux_pollListScreen_seeded() {
        org.junit.Assume.assumeTrue(com.example.BuildConfig.ENABLE_DEMO_DATA)
        val vm = buildSeededViewModel()
        composeTestRule.setContent {
            MyApplicationTheme {
                PollListScreen(
                    viewModel = vm,
                    onCreatePollClick = {},
                    onVotePollClick = {},
                    onAnalyticsPollClick = {},
                    onNotificationsClick = {}
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 15_000) { vm.allPolls.value.isNotEmpty() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ux_poll_list_demo.png")
    }

    // Note: JoinCodeDialog can't be captured here — M3 AlertDialog windows never
    // reach Espresso idle under Robolectric (AppNotIdleException). Review it in code.

    @Test
    fun ux_analyticsScreen_seededPoll() {
        org.junit.Assume.assumeTrue(com.example.BuildConfig.ENABLE_DEMO_DATA)
        val vm = buildSeededViewModel()
        composeTestRule.setContent {
            MyApplicationTheme {
                PollAnalyticsScreen(
                    pollId = "poll_soccer_001",
                    viewModel = vm,
                    onNavigateBack = {},
                    onNavigateToVote = {}
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Vote", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ux_analytics.png")
    }

    /**
     * Sparse-data harness: polls exactly like a real first-time user creates —
     * a title, plain-text options, no venue/time/price metadata, no group,
     * optionally no deadline and no votes.
     */
    private fun buildSparseViewModel(): PollViewModel {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = buildDb()
        val repository = PollRepository(
            pollDao = db.pollDao(),
            pollOptionDao = db.pollOptionDao(),
            voteDao = db.voteDao(),
            notificationDao = db.notificationDao(),
            groupDao = db.groupDao()
        )
        val now = System.currentTimeMillis()

        // Bare poll: nothing but a title and two text options. No deadline,
        // no group, no description, zero votes.
        val barePoll = com.example.data.local.entity.PollEntity(
            id = "poll_bare",
            code = "BARE01",
            title = "Pizza on Friday?",
            description = "",
            category = "FOOD",
            categoryIcon = "🍕",
            creatorName = "Guest",
            createdAt = now,
            deadlineTimestamp = null,
            targetHeadcount = null,
            groupId = null,
            groupName = null
        )
        val bareOptions = listOf(
            com.example.data.local.entity.PollOptionEntity(id = "opt_bare_1", pollId = "poll_bare", text = "Yes", displayOrder = 1),
            com.example.data.local.entity.PollOptionEntity(id = "opt_bare_2", pollId = "poll_bare", text = "No", displayOrder = 2)
        )

        // Partial poll: has a deadline and votes, but its options carry only
        // text — one of them also has a price, to see mixed alignment.
        val partialPoll = com.example.data.local.entity.PollEntity(
            id = "poll_partial",
            code = "PART22",
            title = "Board game night this weekend",
            description = "Pick a game",
            category = "EVENT",
            categoryIcon = "🎲",
            creatorName = "Guest",
            createdAt = now,
            deadlineTimestamp = now + 5 * 3600 * 1000L,
            targetHeadcount = 6,
            groupId = null,
            groupName = null
        )
        val partialOptions = listOf(
            com.example.data.local.entity.PollOptionEntity(id = "opt_part_1", pollId = "poll_partial", text = "Catan", displayOrder = 1),
            com.example.data.local.entity.PollOptionEntity(id = "opt_part_2", pollId = "poll_partial", text = "Ticket to Ride", priceRating = "$25", displayOrder = 2),
            com.example.data.local.entity.PollOptionEntity(id = "opt_part_3", pollId = "poll_partial", text = "Codenames", displayOrder = 3)
        )
        val partialVotes = listOf(
            com.example.data.local.entity.VoteEntity(id = "v1", pollId = "poll_partial", optionId = "opt_part_1", voterId = "u1", voterName = "Guest", timestamp = now),
            com.example.data.local.entity.VoteEntity(id = "v2", pollId = "poll_partial", optionId = "opt_part_1", voterId = "u2", voterName = "Alex", timestamp = now),
            com.example.data.local.entity.VoteEntity(id = "v3", pollId = "poll_partial", optionId = "opt_part_2", voterId = "u3", voterName = "Sam", timestamp = now)
        )

        return PollViewModel(repository, NotificationPreferences(context), AuthPreferences(context)).also {
            runBlocking {
                db.pollDao().insertPoll(barePoll)
                db.pollOptionDao().insertOptions(bareOptions)
                db.pollDao().insertPoll(partialPoll)
                db.pollOptionDao().insertOptions(partialOptions)
                db.voteDao().insertVotes(partialVotes)
            }
        }
    }

    @Test
    fun ux_pollListScreen_sparse() {
        val vm = buildSparseViewModel()
        composeTestRule.setContent {
            MyApplicationTheme {
                PollListScreen(
                    viewModel = vm,
                    onCreatePollClick = {},
                    onVotePollClick = {},
                    onAnalyticsPollClick = {},
                    onNotificationsClick = {}
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 15_000) { vm.allPolls.value.isNotEmpty() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ux_poll_list_sparse.png")
    }

    @Test
    fun ux_votingScreen_sparse() {
        val vm = buildSparseViewModel()
        composeTestRule.setContent {
            MyApplicationTheme {
                VotingScreen(
                    pollId = "poll_bare",
                    viewModel = vm,
                    onNavigateBack = {},
                    onNavigateToAnalytics = {}
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Submit Vote", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Update My Response", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ux_voting_sparse.png")
    }

    @Test
    fun ux_analyticsScreen_zeroVotes() {
        val vm = buildSparseViewModel()
        composeTestRule.setContent {
            MyApplicationTheme {
                PollAnalyticsScreen(
                    pollId = "poll_bare",
                    viewModel = vm,
                    onNavigateBack = {},
                    onNavigateToVote = {}
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Vote", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ux_analytics_zero_votes.png")
    }

    @Test
    fun ux_createPollScreen_blankTap() {
        val vm = buildSeededViewModel()
        composeTestRule.setContent {
            MyApplicationTheme {
                CreatePollScreen(
                    viewModel = vm,
                    initialTemplate = null,
                    onNavigateBack = {},
                    onPollCreated = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ux_create_poll.png")
    }

    @Test
    fun ux_votingScreen_seededPoll() {
        org.junit.Assume.assumeTrue(com.example.BuildConfig.ENABLE_DEMO_DATA)
        val vm = buildSeededViewModel()
        composeTestRule.setContent {
            MyApplicationTheme {
                VotingScreen(
                    pollId = "poll_soccer_001",
                    viewModel = vm,
                    onNavigateBack = {},
                    onNavigateToAnalytics = {}
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Submit Vote", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Update My Response", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/ux_voting.png")
    }
}
