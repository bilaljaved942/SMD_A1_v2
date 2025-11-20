package com.example.firstapp.data.local.dao

import androidx.room.*
import com.example.firstapp.data.local.entities.FollowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollows(follows: List<FollowEntity>)
    
    @Update
    suspend fun updateFollow(follow: FollowEntity)
    
    @Query("SELECT * FROM follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun getFollow(followerId: String, followingId: String): FollowEntity?
    
    @Query("SELECT * FROM follows WHERE followerId = :userId AND status = 'accepted'")
    suspend fun getFollowing(userId: String): List<FollowEntity>
    
    @Query("SELECT * FROM follows WHERE followerId = :userId AND status = 'accepted'")
    fun getFollowingFlow(userId: String): Flow<List<FollowEntity>>
    
    @Query("SELECT * FROM follows WHERE followingId = :userId AND status = 'accepted'")
    suspend fun getFollowers(userId: String): List<FollowEntity>
    
    @Query("SELECT * FROM follows WHERE followingId = :userId AND status = 'pending'")
    suspend fun getPendingFollowRequests(userId: String): List<FollowEntity>
    
    @Query("SELECT * FROM follows WHERE isSynced = 0")
    suspend fun getUnsyncedFollows(): List<FollowEntity>
    
    @Query("UPDATE follows SET status = :status WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun updateFollowStatus(followerId: String, followingId: String, status: String)
    
    @Query("UPDATE follows SET isSynced = 1 WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun markFollowAsSynced(followerId: String, followingId: String)
    
    @Delete
    suspend fun deleteFollow(follow: FollowEntity)
    
    @Query("DELETE FROM follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollowRelation(followerId: String, followingId: String)
}
