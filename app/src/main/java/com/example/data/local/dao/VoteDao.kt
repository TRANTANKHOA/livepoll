package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.VoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoteDao {
    @Query("SELECT * FROM votes WHERE pollId = :pollId ORDER BY timestamp DESC")
    fun getVotesForPoll(pollId: String): Flow<List<VoteEntity>>

    @Query("SELECT * FROM votes WHERE pollId = :pollId ORDER BY timestamp DESC")
    suspend fun getVotesForPollList(pollId: String): List<VoteEntity>

    @Query("SELECT * FROM votes WHERE pollId = :pollId AND voterId = :voterId")
    fun getVotesForUser(pollId: String, voterId: String): Flow<List<VoteEntity>>

    @Query("SELECT * FROM votes WHERE pollId = :pollId AND voterId = :voterId")
    suspend fun getVotesForUserList(pollId: String, voterId: String): List<VoteEntity>

    @Query("SELECT * FROM votes ORDER BY timestamp DESC")
    fun getAllVotes(): Flow<List<VoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: VoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVotes(votes: List<VoteEntity>)

    @Query("DELETE FROM votes WHERE pollId = :pollId AND voterId = :voterId")
    suspend fun clearUserVotesForPoll(pollId: String, voterId: String)

    @Query("DELETE FROM votes WHERE id = :voteId")
    suspend fun deleteVote(voteId: String)
}
