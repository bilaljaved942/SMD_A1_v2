package com.example.firstapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey
    val commentId: String,
    val postId: String,
    val userId: String,
    val content: String,
    val timestamp: Long,
    val isSynced: Boolean = false,
    val lastSynced: Long? = null
)
