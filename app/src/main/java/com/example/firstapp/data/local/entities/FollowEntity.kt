package com.example.firstapp.data.local.entities

import androidx.room.Entity

@Entity(tableName = "follows", primaryKeys = ["followerId", "followingId"])
data class FollowEntity(
    val followerId: String,
    val followingId: String,
    val status: String = "accepted", // pending, accepted, rejected
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
