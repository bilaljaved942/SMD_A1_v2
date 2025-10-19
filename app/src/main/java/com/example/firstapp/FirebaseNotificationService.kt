package com.example.firstapp

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService

class FirebaseNotificationService : FirebaseMessagingService() {

    private val TAG = "FCM_SERVICE"

    /**
     * Called if the FCM registration token is updated (e.g., if the previous token expires).
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Always save the latest token to the user's profile
        sendRegistrationToServer(token)
    }

    /**
     * Persists the FCM registration token to the user's profile in the Firebase Realtime Database.
     * This token is used by the backend to send targeted notifications.
     */
    fun sendRegistrationToServer(token: String?) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null || token.isNullOrEmpty()) {
            Log.w(TAG, "Cannot save token: User not authenticated or token is empty.")
            return
        }

        // Path: /users/{userId}/fcmToken
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId)
        userRef.child("fcmToken").setValue(token)
            .addOnSuccessListener {
                Log.i(TAG, "FCM Token successfully saved for user $currentUserId.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save FCM token: ${e.message}")
            }
    }

    // NOTE: You would also implement onMessageReceived here to handle incoming
    // push notifications when your app is in the foreground.
}

/**
 * Utility function to initially get and save the FCM token.
 * This should be called once from your main/entry activity.
 */
fun requestAndSaveFCMToken() {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            Log.w("FCM_UTIL", "Fetching FCM registration token failed", task.exception)
            return@addOnCompleteListener
        }
        val token = task.result
        // Use the service logic to save the token
        FirebaseNotificationService().sendRegistrationToServer(token)
    }
}
