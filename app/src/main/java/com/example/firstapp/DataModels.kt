package com.example.firstapp

import com.google.firebase.database.PropertyName

/**
 * Represents a user's uploaded story content.
 */
data class Story(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val viewed: Boolean = false,
    val isVideo: Boolean = false
)

/**
 * Represents the core user profile, including the necessary FCM token for notifications.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    @get:PropertyName("profilePicture") @set:PropertyName("profilePicture")
    var profilePictureBase64: String? = null,
    /**
     * CRITICAL: FCM Token is needed to address the user's device for notifications.
     */
    var fcmToken: String? = null,
    var isFollowing: Boolean = false,
    // --- ADDED FOR PRESENCE ---
    val online: Boolean = false,
    val lastOnline: Long = 0L
)

/**
 * A composite class used to display a story along with the user who posted it.
 */
data class DisplayStory(
    val story: Story,
    val user: User
)
