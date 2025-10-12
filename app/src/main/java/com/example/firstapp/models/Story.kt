package com.example.firstapp.models

data class Story(
    val storyId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "", "", "", 0L, 0L)
}
