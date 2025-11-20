package com.example.firstapp.data.local.dao

import androidx.room.*
import com.example.firstapp.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    @Update
    suspend fun updateMessage(message: MessageEntity)
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?
    
    @Query("""
        SELECT * FROM messages 
        WHERE (senderId = :userId1 AND receiverId = :userId2) 
           OR (senderId = :userId2 AND receiverId = :userId1)
        ORDER BY timestamp ASC
    """)
    fun getConversation(userId1: String, userId2: String): Flow<List<MessageEntity>>
    
    @Query("""
        SELECT * FROM messages 
        WHERE (senderId = :userId1 AND receiverId = :userId2) 
           OR (senderId = :userId2 AND receiverId = :userId1)
        AND isDeleted = 0
        ORDER BY timestamp ASC
    """)
    suspend fun getConversationList(userId1: String, userId2: String): List<MessageEntity>
    
    @Query("""
        SELECT * FROM messages 
        WHERE senderId = :currentUserId OR receiverId = :currentUserId
        AND isDeleted = 0
        ORDER BY timestamp DESC
    """)
    fun getAllConversations(currentUserId: String): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE isSynced = 0")
    suspend fun getUnsyncedMessages(): List<MessageEntity>
    
    @Query("UPDATE messages SET isSynced = 1, lastSynced = :timestamp WHERE id = :messageId")
    suspend fun markMessageAsSynced(messageId: String, timestamp: Long)
    
    @Query("UPDATE messages SET isSeen = 1, seenAt = :timestamp WHERE id = :messageId")
    suspend fun markMessageAsSeen(messageId: String, timestamp: Long)
    
    @Query("UPDATE messages SET isDeleted = 1, deletedAt = :timestamp WHERE id = :messageId")
    suspend fun markMessageAsDeleted(messageId: String, timestamp: Long)
    
    @Query("UPDATE messages SET content = :content, isEdited = 1, editedAt = :timestamp WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, content: String, timestamp: Long)
    
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
    
    @Query("DELETE FROM messages WHERE vanishMode = 1 AND isSeen = 1")
    suspend fun deleteVanishedMessages()
    
    @Query("""
        DELETE FROM messages 
        WHERE (senderId = :userId1 AND receiverId = :userId2) 
           OR (senderId = :userId2 AND receiverId = :userId1)
    """)
    suspend fun deleteConversation(userId1: String, userId2: String)
}
