package com.plutusoft.livepulse.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.plutusoft.livepulse.data.local.dao.GroupDao
import com.plutusoft.livepulse.data.local.dao.NotificationDao
import com.plutusoft.livepulse.data.local.dao.PollDao
import com.plutusoft.livepulse.data.local.dao.PollOptionDao
import com.plutusoft.livepulse.data.local.dao.VoteDao
import com.plutusoft.livepulse.data.local.entity.GroupEntity
import com.plutusoft.livepulse.data.local.entity.NotificationEntity
import com.plutusoft.livepulse.data.local.entity.PollEntity
import com.plutusoft.livepulse.data.local.entity.PollOptionEntity
import com.plutusoft.livepulse.data.local.entity.VoteEntity

@Database(
    entities = [
        PollEntity::class,
        PollOptionEntity::class,
        VoteEntity::class,
        NotificationEntity::class,
        GroupEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pollDao(): PollDao
    abstract fun pollOptionDao(): PollOptionDao
    abstract fun voteDao(): VoteDao
    abstract fun notificationDao(): NotificationDao
    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "livepulse_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
