package com.example.firstapp.data.local.dao

import androidx.room.*
import com.example.firstapp.data.local.entities.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)
    
    @Update
    suspend fun updateStory(story: StoryEntity)
    
    @Query("SELECT * FROM stories WHERE id = :storyId")
    suspend fun getStoryById(storyId: String): StoryEntity?
    
    @Query("SELECT * FROM stories WHERE userId = :userId AND expiresAt > :currentTime ORDER BY timestamp DESC")
    suspend fun getActiveStoriesByUser(userId: String, currentTime: Long): List<StoryEntity>
    
    @Query("SELECT * FROM stories WHERE expiresAt > :currentTime ORDER BY timestamp DESC")
    fun getActiveStories(currentTime: Long): Flow<List<StoryEntity>>
    
    @Query("SELECT * FROM stories WHERE expiresAt > :currentTime ORDER BY timestamp DESC")
    suspend fun getActiveStoriesList(currentTime: Long): List<StoryEntity>
    
    @Query("DELETE FROM stories WHERE expiresAt <= :currentTime")
    suspend fun deleteExpiredStories(currentTime: Long)
    
    @Query("UPDATE stories SET viewed = 1 WHERE id = :storyId")
    suspend fun markStoryAsViewed(storyId: String)
    
    @Query("SELECT * FROM stories WHERE isSynced = 0")
    suspend fun getUnsyncedStories(): List<StoryEntity>
    
    @Query("UPDATE stories SET isSynced = 1, lastSynced = :timestamp WHERE id = :storyId")
    suspend fun markStoryAsSynced(storyId: String, timestamp: Long)
    
    @Delete
    suspend fun deleteStory(story: StoryEntity)
    
    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteStoryById(storyId: String)
}
