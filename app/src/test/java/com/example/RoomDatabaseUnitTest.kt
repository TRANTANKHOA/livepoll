package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.PollDao
import com.example.data.local.dao.PollOptionDao
import com.example.data.local.dao.VoteDao
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.PollEntity
import com.example.data.local.entity.PollOptionEntity
import com.example.data.local.entity.VoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDatabaseUnitTest {

    private lateinit var db: AppDatabase
    private lateinit var pollDao: PollDao
    private lateinit var optionDao: PollOptionDao
    private lateinit var voteDao: VoteDao
    private lateinit var notificationDao: NotificationDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pollDao = db.pollDao()
        optionDao = db.pollOptionDao()
        voteDao = db.voteDao()
        notificationDao = db.notificationDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrievePoll() = runBlocking {
        val poll = PollEntity(
            id = "db_poll_1",
            code = "CODE99",
            title = "Database Test Poll",
            description = "Testing in-memory Room",
            category = "SOCCER",
            categoryIcon = "⚽",
            creatorName = "Tester",
            createdAt = System.currentTimeMillis()
        )

        pollDao.insertPoll(poll)
        val retrieved = pollDao.getPollById("db_poll_1").first()
        assertNotNull(retrieved)
        assertEquals("CODE99", retrieved?.code)
        assertEquals("Database Test Poll", retrieved?.title)

        val retrievedByCode = pollDao.getPollByCode("CODE99")
        assertNotNull(retrievedByCode)
        assertEquals("db_poll_1", retrievedByCode?.id)
    }

    @Test
    fun testInsertOptionsAndVoteCascade() = runBlocking {
        val poll = PollEntity(
            id = "poll_cascade",
            code = "CASC01",
            title = "Cascade Test",
            description = "Testing cascade delete",
            category = "FOOD",
            categoryIcon = "🍔",
            creatorName = "Host",
            createdAt = System.currentTimeMillis()
        )
        pollDao.insertPoll(poll)

        val opt1 = PollOptionEntity(id = "opt_c1", pollId = "poll_cascade", text = "Burger")
        val opt2 = PollOptionEntity(id = "opt_c2", pollId = "poll_cascade", text = "Pizza")
        optionDao.insertOptions(listOf(opt1, opt2))

        val options = optionDao.getOptionsForPoll("poll_cascade").first()
        assertEquals(2, options.size)

        // Insert votes
        val vote = VoteEntity(
            id = "v_casc_1",
            pollId = "poll_cascade",
            optionId = "opt_c1",
            voterId = "user_100",
            voterName = "Alice",
            rsvpStatus = "GOING",
            plusGuests = 1,
            timestamp = System.currentTimeMillis()
        )
        voteDao.insertVote(vote)

        val pollVotes = voteDao.getVotesForPoll("poll_cascade").first()
        assertEquals(1, pollVotes.size)
        assertEquals("Alice", pollVotes.first().voterName)

        // Test clear user votes for poll
        voteDao.clearUserVotesForPoll("poll_cascade", "user_100")
        val votesAfterDelete = voteDao.getVotesForPoll("poll_cascade").first()
        assertTrue(votesAfterDelete.isEmpty())
    }

    @Test
    fun testNotificationsFlowAndMarkAsRead() = runBlocking {
        val notif = NotificationEntity(
            id = "notif_1",
            pollId = "poll_cascade",
            pollTitle = "Soccer Match",
            title = "Quorum Reached!",
            message = "8 voters have joined the soccer match",
            type = "QUORUM_REACHED",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        notificationDao.insertNotification(notif)
        val unread = notificationDao.getUnreadCount().first()
        assertEquals(1, unread)

        notificationDao.markAllAsRead()
        val unreadAfter = notificationDao.getUnreadCount().first()
        assertEquals(0, unreadAfter)
    }
}
