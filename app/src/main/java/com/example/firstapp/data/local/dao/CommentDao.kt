package com.example.firstapp.data.local.dao

import androidx.room.*
import com.example.firstapp.data.local.entities.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)
    
    @Update
    suspend fun updateComment(comment: CommentEntity)
    
    @Query("SELECT * FROM comments WHERE commentId = :commentId")
    suspend fun getCommentById(commentId: String): CommentEntity?
    
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp DESC")
    suspend fun getCommentsByPost(postId: String): List<CommentEntity>
    
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp DESC")
    fun getCommentsByPostFlow(postId: String): Flow<List<CommentEntity>>
    
    @Query("SELECT * FROM comments WHERE isSynced = 0")
    suspend fun getUnsyncedComments(): List<CommentEntity>
    
    @Query("UPDATE comments SET isSynced = 1, lastSynced = :timestamp WHERE commentId = :commentId")
    suspend fun markCommentAsSynced(commentId: String, timestamp: Long)
    
    @Delete
    suspend fun deleteComment(comment: CommentEntity)
    
    @Query("DELETE FROM comments WHERE commentId = :commentId")
    suspend fun deleteCommentById(commentId: String)
    
    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteCommentsByPost(postId: String)
}
