package com.example.firstapp

import com.google.firebase.database.PropertyName
data class Story(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val viewed: Boolean = false,
    val isVideo: Boolean = false // CRITICAL FIX: This field must be added
)
// UPDATED User data class
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    @get:PropertyName("profilePicture") @set:PropertyName("profilePicture")
    var profilePictureBase64: String? = null,
    var isFollowing: Boolean = false,
    // --- ADDED FOR PRESENCE ---
    val online: Boolean = false,
    val lastOnline: Long = 0L
)

data class DisplayStory(
    val story: Story,
    val user: User
)