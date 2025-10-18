package com.example.firstapp

data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis() // Store timestamp for sorting
)