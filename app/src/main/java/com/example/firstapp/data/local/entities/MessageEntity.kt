package com.example.firstapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    val mediaBase64: String? = null,
    val mediaUrl: String? = null,
    val type: String = "text", // text, image, video, document, call_invite
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val isSeen: Boolean = false,
    val seenAt: Long? = null,
    val callType: String? = null, // video, audio
    val channelName: String? = null,
    val vanishMode: Boolean = false,
    val isSynced: Boolean = false,
    val lastSynced: Long? = null
)
