package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE isUserMember = 1")
    fun getUserJoinedGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Query("UPDATE groups SET isUserMember = :isMember WHERE id = :groupId")
    suspend fun setMembership(groupId: String, isMember: Boolean)

    @Query("UPDATE groups SET notificationsEnabled = :enabled WHERE id = :groupId")
    suspend fun setGroupNotifications(groupId: String, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM groups")
    suspend fun getGroupsCount(): Int
}
