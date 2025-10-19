package com.example.firstapp

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MyApplication : Application() {

    private lateinit var auth: FirebaseAuth
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private val appLifecycleObserver = AppLifecycleObserver()

    // Store the last known user ID to correctly mark them as offline on logout.
    private var lastLoggedInUserId: String? = null

    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        auth = FirebaseAuth.getInstance()

        // This observer handles the app going to the background or foreground.
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        // This new listener handles login and logout events instantly.
        setupAuthStateListener()
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // --- USER HAS LOGGED IN ---
                lastLoggedInUserId = user.uid
                // Set up disconnection hooks for this specific user (for crashes, lost internet).
                setupOnDisconnect(user.uid)
                // Explicitly set the user to 'online' because they just logged in.
                updateUserStatus(user.uid, true)
            } else {
                // --- USER HAS LOGGED OUT ---
                lastLoggedInUserId?.let { uid ->
                    // Explicitly set the last known user to 'offline' because they just logged out.
                    updateUserStatus(uid, false)
                }
                lastLoggedInUserId = null
            }
        }
        // Attach the listener to FirebaseAuth.
        auth.addAuthStateListener(authStateListener!!)
    }

    /**
     * A robust function to update a user's status.
     */
    private fun updateUserStatus(userId: String, isOnline: Boolean) {
        val userStatusRef = FirebaseDatabase.getInstance().getReference("users/$userId")
        val statusUpdate = mapOf<String, Any>(
            "online" to isOnline,
            "lastOnline" to System.currentTimeMillis()
        )
        userStatusRef.updateChildren(statusUpdate)
    }

    /**
     * Tells Firebase to mark the user as offline if the app connection is lost unexpectedly.
     */
    private fun setupOnDisconnect(userId: String) {
        val userStatusRef = FirebaseDatabase.getInstance().getReference("users/$userId")
        val onDisconnectUpdate = mapOf<String, Any>(
            "online" to false,
            "lastOnline" to System.currentTimeMillis()
        )
        userStatusRef.onDisconnect().updateChildren(onDisconnectUpdate)
    }

    /**
     * This inner class handles the app moving between foreground and background.
     */
    inner class AppLifecycleObserver : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            // Only update status if a user is currently logged in.
            val currentUser = auth.currentUser ?: return

            when (event) {
                Lifecycle.Event.ON_START -> {
                    // App came to the foreground.
                    updateUserStatus(currentUser.uid, true)
                }
                Lifecycle.Event.ON_STOP -> {
                    // App went to the background.
                    updateUserStatus(currentUser.uid, false)
                }
                else -> {
                    // Other lifecycle events are not needed.
                }
            }
        }
    }
}