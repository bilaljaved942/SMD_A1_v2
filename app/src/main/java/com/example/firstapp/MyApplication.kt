package com.example.firstapp

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Enable disk persistence to cache data and work offline
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        setupPresenceSystem()
    }

    private fun setupPresenceSystem() {
        // This setup runs when the app starts and a user is logged in.
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val myConnectionsRef = FirebaseDatabase.getInstance().getReference("users/${user.uid}/online")
            val lastOnlineRef = FirebaseDatabase.getInstance().getReference("users/${user.uid}/lastOnline")
            val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")

            connectedRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        // When the device is connected to Firebase, set status to online.
                        myConnectionsRef.setValue(true)
                        // Set up a command to mark the user as offline when they disconnect.
                        myConnectionsRef.onDisconnect().setValue(false)
                        // Also, set a timestamp for the last online time on disconnect.
                        lastOnlineRef.onDisconnect().setValue(System.currentTimeMillis())
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    // Log error if listener is cancelled
                }
            })
        }
    }
}