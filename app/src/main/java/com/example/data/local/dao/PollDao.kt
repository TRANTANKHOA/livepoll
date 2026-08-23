package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PollEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PollDao {
    @Query("SELECT * FROM polls ORDER BY createdAt DESC")
    fun getAllPolls(): Flow<List<PollEntity>>

    @Query("SELECT * FROM polls WHERE id = :pollId LIMIT 1")
    fun getPollById(pollId: String): Flow<PollEntity?>

    @Query("SELECT * FROM polls WHERE id = :pollId LIMIT 1")
    suspend fun getPollByIdSync(pollId: String): PollEntity?

    @Query("SELECT * FROM polls WHERE UPPER(code) = UPPER(:code) LIMIT 1")
    suspend fun getPollByCode(code: String): PollEntity?

    @Query("SELECT * FROM polls WHERE isClosed = 0 AND deadlineTimestamp IS NOT NULL AND deadlineTimestamp > :now ORDER BY deadlineTimestamp ASC")
    suspend fun getActivePollsWithDeadline(now: Long): List<PollEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoll(poll: PollEntity)

    @Update
    suspend fun updatePoll(poll: PollEntity)

    @Query("UPDATE polls SET isClosed = :isClosed WHERE id = :pollId")
    suspend fun setPollClosed(pollId: String, isClosed: Boolean)

    @Query("DELETE FROM polls WHERE id = :pollId")
    suspend fun deletePoll(pollId: String)

    @Query("SELECT COUNT(*) FROM polls")
    suspend fun getPollsCount(): Int
}
