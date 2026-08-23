package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.AuthPreferences
import com.example.data.local.NotificationPreferences
import com.example.data.local.entity.PollEntity
import com.example.data.local.entity.PollOptionEntity
import com.example.data.model.AuthProvider
import com.example.data.model.UserAccount
import com.example.data.repository.PollRepository
import com.example.ui.viewmodel.PollViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PollViewModelUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var repository: PollRepository
    private lateinit var authPreferences: AuthPreferences
    private lateinit var notificationPreferences: NotificationPreferences
    private lateinit var viewModel: PollViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        authPreferences = AuthPreferences(context)
        notificationPreferences = NotificationPreferences(context)
        repository = PollRepository(
            pollDao = db.pollDao(),
            pollOptionDao = db.pollOptionDao(),
            voteDao = db.voteDao(),
            notificationDao = db.notificationDao(),
            groupDao = db.groupDao()
        )
        viewModel = PollViewModel(repository, notificationPreferences, authPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testUserSwitching() = runTest(testDispatcher) {
        viewModel.switchUser(
            id = "user_sarah",
            name = "Sarah Connor",
            avatarEmoji = "👩‍🎨"
        )

        assertEquals("user_sarah", viewModel.currentVoterId.value)
        assertEquals("Sarah Connor", viewModel.currentVoterName.value)
        assertEquals("👩‍🎨", viewModel.currentUser.value.avatarEmoji)
    }

    @Test
    fun testSetCurrentUser() = runTest(testDispatcher) {
        val customUser = UserAccount(
            id = "user_marcus",
            name = "Marcus Aurelius",
            email = "marcus@apple.com",
            provider = AuthProvider.APPLE,
            avatarEmoji = "👨‍💼"
        )

        viewModel.setCurrentUser(customUser)
        assertEquals("user_marcus", viewModel.currentVoterId.value)
        assertEquals("Marcus Aurelius", viewModel.currentVoterName.value)
        assertEquals(AuthProvider.APPLE, viewModel.currentUser.value.provider)
    }

    @Test
    fun testRepositoryPollCreationAndDetails() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val poll = PollEntity(
            id = "test_vm_poll",
            code = "VMT101",
            title = "ViewModel Test Poll",
            description = "Direct repo test",
            category = "SOCCER",
            categoryIcon = "⚽",
            creatorName = "Host",
            createdAt = now
        )
        val options = listOf(
            PollOptionEntity(id = "opt_1", pollId = "test_vm_poll", text = "Option 1"),
            PollOptionEntity(id = "opt_2", pollId = "test_vm_poll", text = "Option 2")
        )

        repository.createPoll(poll, options)

        val retrievedByCode = repository.getPollByCode("VMT101")
        assertNotNull(retrievedByCode)
        assertEquals("ViewModel Test Poll", retrievedByCode?.title)

        val details = repository.getPollWithDetails("test_vm_poll", "user_test").first()
        assertNotNull(details)
        assertEquals(2, details?.options?.size)
        assertEquals(0, details?.totalVotes)
    }

    @Test
    fun testNotificationPreferencesToggle() = runTest(testDispatcher) {
        notificationPreferences.updateVoteActivityAlerts(false)
        assertFalse(notificationPreferences.settings.value.voteActivityAlertsEnabled)

        notificationPreferences.updateApproachingDeadlines(true)
        assertTrue(notificationPreferences.settings.value.approachingDeadlinesEnabled)
    }
}
