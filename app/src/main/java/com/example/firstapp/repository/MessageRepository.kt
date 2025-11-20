package com.example.firstapp.repository

import android.content.Context
import android.util.Log
import com.example.firstapp.data.local.SociallyDatabase
import com.example.firstapp.data.local.entities.MessageEntity
import com.example.firstapp.data.local.entities.PendingActionEntity
import com.example.firstapp.data.remote.RetrofitClient
import com.example.firstapp.data.remote.models.*
import com.example.firstapp.utils.SecurePreferences
import com.example.firstapp.utils.NetworkUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MessageRepository(private val context: Context) {

    private val messageApi = RetrofitClient.messageApi
    private val messageDao = SociallyDatabase.getDatabase(context).messageDao()
    private val pendingActionDao = SociallyDatabase.getDatabase(context).pendingActionDao()
    private val prefs = SecurePreferences(context)
    private val gson = Gson()

    /**
     * Send a message
     */
    suspend fun sendMessage(
        receiverId: String,
        content: String?,
        mediaBase64: String? = null,
        type: String = "text",
        vanishMode: Boolean = false,
        callType: String? = null,
        channelName: String? = null
    ): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val senderId = prefs.getUserId()
                if (senderId == null) {
                    return@withContext Result.failure(Exception("Not logged in"))
                }

                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken() ?: return@withContext Result.failure(Exception("Not logged in"))
                    val request = SendMessageRequest(senderId = senderId, receiverId = receiverId,
                        content = content ?: "",
                        mediaBase64 = mediaBase64,
                        type = type,
                        vanishMode = vanishMode,
                        callType = callType,
                        channelName = channelName
                    )
                    val response = messageApi.sendMessage(request)

                    if (response.isSuccessful) {
                        val messageResponse = response.body()!!
                        
                        // Save to local database
                        val messageEntity = MessageEntity(
                            id = messageResponse.id, senderId = messageResponse.senderId, receiverId = messageResponse.receiverId,
                            content = messageResponse.content,
                            timestamp = System.currentTimeMillis(),
                            mediaBase64 = mediaBase64,
                            mediaUrl = messageResponse.mediaUrl,
                            type = messageResponse.type,
                            isEdited = messageResponse.isEdited,
                            editedAt = messageResponse.editedAt,
                            isDeleted = messageResponse.isDeleted,
                            deletedAt = null,
                            isSeen = messageResponse.isSeen,
                            seenAt = messageResponse.seenAt,
                            callType = messageResponse.callType,
                            channelName = messageResponse.channelName,
                            vanishMode = vanishMode,
                            isSynced = true
                        )
                        messageDao.insertMessages(listOf(messageEntity))

                        Log.d("MessageRepository", "Message sent: ${messageResponse.id}")
                        Result.success(messageResponse)
                    } else {
                        Result.failure(Exception("Failed to send message"))
                    }
                } else {
                    // Queue for later sync
                    val payload = gson.toJson(mapOf(
                        "receiverId" to receiverId,
                        "content" to content,
                        "mediaBase64" to mediaBase64,
                        "type" to type,
                        "vanishMode" to vanishMode
                    ))
                    
                    val pendingAction = PendingActionEntity(
                        actionType = "send_message",
                        entityId = "temp_${System.currentTimeMillis()}",
                        payload = payload,
                        retryCount = 0,
                        maxRetries = 5
                    )
                    pendingActionDao.insertAction(pendingAction)

                    // Save locally with isSynced = false
                    val messageEntity = MessageEntity(
                        id = pendingAction.entityId,
                        senderId = senderId,
                        receiverId = receiverId,
                        content = content ?: "",
                        timestamp = System.currentTimeMillis(),
                        mediaBase64 = mediaBase64,
                        mediaUrl = null,
                        type = type,
                        isEdited = false,
                        editedAt = null,
                        isDeleted = false,
                        deletedAt = null,
                        isSeen = false,
                        seenAt = null,
                        callType = callType,
                        channelName = channelName,
                        vanishMode = vanishMode,
                        isSynced = false
                    )
                    messageDao.insertMessages(listOf(messageEntity))

                    Log.d("MessageRepository", "Message queued for sync")
                    Result.failure(Exception("No internet - message will sync later"))
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Send message error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Fetch messages in a conversation
     */
    suspend fun fetchMessages(otherUserId: String, since: Long? = null): Result<List<MessageEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = prefs.getUserId()
                if (currentUserId == null) {
                    return@withContext Result.failure(Exception("Not logged in"))
                }

                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken() ?: return@withContext Result.failure(Exception("Not logged in"))
                    val response = messageApi.fetchMessages(currentUserId, otherUserId, since)

                    if (response.isSuccessful && response.body()?.success == true) {
                        val messages = response.body()!!.messages.map { msgResponse ->
                            MessageEntity(
                                id = msgResponse.id,
                                senderId = msgResponse.senderId,
                                receiverId = msgResponse.receiverId,
                                content = msgResponse.content,
                                timestamp = System.currentTimeMillis(),
                                mediaBase64 = null, // Not sent from server
                                mediaUrl = msgResponse.mediaUrl,
                                type = msgResponse.type,
                                isEdited = msgResponse.isEdited,
                                editedAt = msgResponse.editedAt,
                                isDeleted = msgResponse.isDeleted,
                                deletedAt = null,
                                isSeen = msgResponse.isSeen,
                                seenAt = msgResponse.seenAt,
                                callType = msgResponse.callType,
                                channelName = msgResponse.channelName,
                                vanishMode = false,
                                isSynced = true
                            )
                        }

                        // Cache in local database
                        messageDao.insertMessages(messages)

                        Log.d("MessageRepository", "Fetched ${messages.size} messages")
                        Result.success(messages)
                    } else {
                        Result.failure(Exception("Failed to fetch messages"))
                    }
                } else {
                    // Use cached data
                    Log.d("MessageRepository", "No internet - loading cached messages")
                    Result.failure(Exception("No internet - using cache"))
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Fetch messages error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Edit a message (within 5 minutes)
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken() ?: return@withContext Result.failure(Exception("Not logged in"))
                    val request = EditMessageRequest(messageId, newContent)
                    val response = messageApi.editMessage(request)

                    if (response.isSuccessful && response.body()?.success == true) {
                        // Update local database
                        messageDao.updateMessageContent(messageId, newContent, System.currentTimeMillis())
                        
                        Log.d("MessageRepository", "Message edited: $messageId")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(response.body()?.message ?: "Failed to edit message"))
                    }
                } else {
                    Result.failure(Exception("No internet connection"))
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Edit message error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken() ?: return@withContext Result.failure(Exception("Not logged in"))
                    val request = DeleteMessageRequest(messageId)
                    val response = messageApi.deleteMessage(request)

                    if (response.isSuccessful && response.body()?.success == true) {
                        // Mark as deleted locally
                        messageDao.markMessageAsDeleted(messageId, System.currentTimeMillis())
                        
                        Log.d("MessageRepository", "Message deleted: $messageId")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(response.body()?.message ?: "Failed to delete message"))
                    }
                } else {
                    Result.failure(Exception("No internet connection"))
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Delete message error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Mark messages as seen
     */
    suspend fun markAsSeen(messageIds: List<String>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Update locally immediately for better UX
                messageIds.forEach { messageId ->
                    messageDao.markMessageAsSeen(messageId, System.currentTimeMillis())
                }

                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken() ?: return@withContext Result.failure(Exception("Not logged in"))
                    // Assuming API accepts array of message IDs
                    messageIds.forEach { messageId ->
                        messageApi.markMessageAsSeen(messageId)
                    }
                    
                    Log.d("MessageRepository", "Messages marked as seen")
                    Result.success(Unit)
                } else {
                    // Already updated locally
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Mark as seen error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get conversation as Flow for reactive UI
     */
    fun getConversationFlow(userId1: String, userId2: String): Flow<List<MessageEntity>> {
        return messageDao.getConversation(userId1, userId2)
    }

    /**
     * Delete conversation
     */
    suspend fun deleteConversation(otherUserId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = prefs.getUserId() ?: "" ?: return@withContext Result.failure(Exception("Not logged in"))
                messageDao.deleteConversation(currentUserId, otherUserId)
                Log.d("MessageRepository", "Conversation deleted")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("MessageRepository", "Delete conversation error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}
