package com.example.firstapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: Int,  // Changed from String to Int to match API responses
    val name: String,
    val email: String,
    val profilePictureBase64: String? = null,
    val coverPhotoBase64: String? = null,
    val bio: String? = null,
    val fcmToken: String? = null,
    val online: Boolean = false,
    val lastOnline: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSynced: Long = System.currentTimeMillis()
)
