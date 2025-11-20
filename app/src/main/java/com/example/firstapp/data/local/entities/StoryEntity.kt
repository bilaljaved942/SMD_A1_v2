package com.example.firstapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val mediaUrl: String,
    val mediaBase64: String? = null,
    val timestamp: Long,
    val expiresAt: Long,
    val viewed: Boolean = false,
    val isVideo: Boolean = false,
    val isSynced: Boolean = false,
    val lastSynced: Long? = null
)
