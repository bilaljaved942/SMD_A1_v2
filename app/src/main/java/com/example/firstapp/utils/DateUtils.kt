package com.example.firstapp.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    
    fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
    
    fun formatMessageTime(timestamp: Long): String {
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        
        return when {
            now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) &&
                    now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            now.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1 &&
                    now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                "Yesterday"
            }
            else -> {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
    
    fun isWithin5Minutes(timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return diff < 300000 // 5 minutes in milliseconds
    }
    
    fun isStoryExpired(timestamp: Long, expirationHours: Int = 24): Boolean {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return diff > (expirationHours * 3600000L)
    }
}
