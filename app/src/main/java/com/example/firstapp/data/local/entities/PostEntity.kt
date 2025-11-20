package com.example.firstapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val postId: String,
    val userId: String,
    val mediaBase64: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String = "image", // image, video
    val caption: String = "",
    val location: String = "",
    val timestamp: Long,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isSynced: Boolean = false,
    val lastSynced: Long? = null
)
