package com.example.firstapp.data.local.dao

import androidx.room.*
import com.example.firstapp.data.local.entities.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)
    
    @Update
    suspend fun updatePost(post: PostEntity)
    
    @Query("SELECT * FROM posts WHERE postId = :postId")
    suspend fun getPostById(postId: String): PostEntity?
    
    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getPostsByUser(userId: String): List<PostEntity>
    
    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPostsByUserFlow(userId: String): Flow<List<PostEntity>>
    
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    suspend fun getAllPosts(): List<PostEntity>
    
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>
    
    @Query("SELECT * FROM posts WHERE isSynced = 0")
    suspend fun getUnsyncedPosts(): List<PostEntity>
    
    @Query("UPDATE posts SET isSynced = 1, lastSynced = :timestamp WHERE postId = :postId")
    suspend fun markPostAsSynced(postId: String, timestamp: Long)
    
    @Query("UPDATE posts SET likesCount = :count WHERE postId = :postId")
    suspend fun updateLikesCount(postId: String, count: Int)
    
    @Query("UPDATE posts SET commentsCount = :count WHERE postId = :postId")
    suspend fun updateCommentsCount(postId: String, count: Int)
    
    @Delete
    suspend fun deletePost(post: PostEntity)
    
    @Query("DELETE FROM posts WHERE postId = :postId")
    suspend fun deletePostById(postId: String)
}
