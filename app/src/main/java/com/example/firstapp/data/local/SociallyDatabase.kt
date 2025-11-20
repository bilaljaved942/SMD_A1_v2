package com.example.firstapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.firstapp.data.local.dao.*
import com.example.firstapp.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        MessageEntity::class,
        PostEntity::class,
        StoryEntity::class,
        CommentEntity::class,
        FollowEntity::class,
        PendingActionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SociallyDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun commentDao(): CommentDao
    abstract fun followDao(): FollowDao
    abstract fun pendingActionDao(): PendingActionDao
    
    companion object {
        @Volatile
        private var INSTANCE: SociallyDatabase? = null
        
        fun getDatabase(context: Context): SociallyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SociallyDatabase::class.java,
                    "socially_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
