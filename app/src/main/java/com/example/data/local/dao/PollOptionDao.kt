package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PollOptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PollOptionDao {
    @Query("SELECT * FROM poll_options WHERE pollId = :pollId ORDER BY displayOrder ASC")
    fun getOptionsForPoll(pollId: String): Flow<List<PollOptionEntity>>

    @Query("SELECT * FROM poll_options WHERE pollId = :pollId ORDER BY displayOrder ASC")
    suspend fun getOptionsForPollList(pollId: String): List<PollOptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: PollOptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<PollOptionEntity>)

    @Query("DELETE FROM poll_options WHERE id = :optionId")
    suspend fun deleteOption(optionId: String)
}
