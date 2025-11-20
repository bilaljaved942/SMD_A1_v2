package com.example.firstapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents offline actions that need to be synced when internet is available
 */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String, // send_message, upload_post, upload_story, follow_request, like_post, etc.
    val entityId: String, // ID of the related entity (message ID, post ID, etc.)
    val payload: String, // JSON payload with action details
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null,
    val errorMessage: String? = null
)
