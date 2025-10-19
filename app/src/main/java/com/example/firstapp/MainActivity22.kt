package com.example.firstapp

import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

// Note: Assumes User data class and UserAdapter are defined elsewhere in the 'com.example.firstapp' package.
// Also assumes requestAndSaveFCMToken() is an external utility function.

class MainActivity22 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var searchView: SearchView
    private lateinit var followingFilterCheckbox: CheckBox

    // Master list of all users fetched from the database
    private val allUserList = ArrayList<User>()
    // The list currently shown in the RecyclerView (filtered/searched)
    private val displayedUserList = ArrayList<User>()

    // Set of UIDs the current user is following for quick lookups
    private val followingUids = HashSet<String>()

    // Store the current user's name (existing code for notification)
    private var currentUserName: String = "A Follower"

    // Current user ID for convenience
    private val currentUserId: String by lazy { auth.currentUser?.uid ?: "" }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main22)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // --- NEW: Initialize Search and Filter UI ---
        searchView = findViewById(R.id.user_search_view)
        followingFilterCheckbox = findViewById(R.id.checkbox_following_filter)
        // -------------------------------------------

        usersRecyclerView = findViewById(R.id.users_recycler_view)

        // Set up the RecyclerView
        usersRecyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(displayedUserList, currentUserId, ::onFollowButtonClicked)
        usersRecyclerView.adapter = userAdapter

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_find_people)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.back_arrow)?.setOnClickListener {
            finish()
        }

        // Start data fetching and setup listeners
        setupSearchAndFilterListeners()
        fetchUsers()
        fetchCurrentUserName(currentUserId)

        // Retaining your original line for FCM token handling
        // requestAndSaveFCMToken()
    }

    // ----------------------------------------------------------------------------------
    // SEARCH AND FILTER IMPLEMENTATION
    // ----------------------------------------------------------------------------------

    private fun setupSearchAndFilterListeners() {
        // 1. Search View Listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                applyFiltersAndSearch(newText.orEmpty())
                return true
            }
        })

        // 2. Checkbox Listener
        followingFilterCheckbox.setOnCheckedChangeListener { _, _ ->
            applyFiltersAndSearch(searchView.query.toString())
        }
    }

    /**
     * Applies the current search query and checkbox filter to the master list.
     */
    private fun applyFiltersAndSearch(query: String) {
        val isFollowingFilterActive = followingFilterCheckbox.isChecked
        val searchLower = query.trim().lowercase()

        // Filter the master list
        val filteredList = allUserList.filter { user ->
            // 1. Name/Username Search Filter
            val nameMatch = user.name.lowercase().contains(searchLower)

            // 2. Following Filter
            val filterMatch = if (isFollowingFilterActive) {
                // If checkbox is checked, only show users I am following
                user.isFollowing
            } else {
                // If checkbox is not checked, show all users
                true
            }

            // Must match BOTH conditions
            nameMatch && filterMatch
        }.sortedBy { it.name.lowercase() } // Keep list sorted by name

        // Update the displayed list and notify the adapter
        displayedUserList.clear()
        displayedUserList.addAll(filteredList)
        userAdapter.notifyDataSetChanged()
    }

    // ----------------------------------------------------------------------------------
    // DATA FETCHING AND PROCESSING
    // ----------------------------------------------------------------------------------

    /**
     * Fetches the current user's name to use in the notification payload. (Original logic)
     */
    private fun fetchCurrentUserName(currentUserId: String) {
        database.getReference("users").child(currentUserId).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUserName = snapshot.getValue(String::class.java) ?: auth.currentUser?.email ?: "A Follower"
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FindPeople", "Failed to fetch current user name: ${error.message}")
                }
            })
    }


    /**
     * Fetches all user profiles from the /users node, excluding the current user.
     */
    private fun fetchUsers() {
        val usersRef = database.getReference("users")

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Authentication error. Please log in.", Toast.LENGTH_LONG).show()
            return
        }

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUserList.clear() // Use the master list
                val processedUids = HashSet<String>()

                for (userSnapshot in snapshot.children) {
                    val uid = userSnapshot.key

                    if (uid != null && uid != currentUserId && !processedUids.contains(uid)) {
                        val name = userSnapshot.child("name").getValue(String::class.java)

                        if (name != null && name.isNotBlank()) {
                            val email = userSnapshot.child("email").getValue(String::class.java) ?: ""
                            val profilePic = userSnapshot.child("profilePicture").getValue(String::class.java)
                            val fcmToken = userSnapshot.child("fcmToken").getValue(String::class.java)

                            // Add to the master list
                            allUserList.add(User(
                                uid = uid,
                                name = name,
                                email = email,
                                profilePictureBase64 = profilePic,
                                isFollowing = false,
                                fcmToken = fcmToken
                            ))
                            processedUids.add(uid)
                        } else {
                            Log.w("FindPeople", "Skipped user with UID $uid due to missing name.")
                        }
                    }
                }

                // Next: Determine which of these users the current user is following
                fetchFollowingStatus()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FindPeople", "Failed to fetch users: ${error.message}")
                Toast.makeText(this@MainActivity22, "Failed to load users.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Checks which users the current user is already following and updates the list state.
     */
    private fun fetchFollowingStatus() {
        val followingRef = database.getReference("following").child(currentUserId)

        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followingUids.clear()
                // Collect UIDs of people the current user follows
                snapshot.children.mapNotNullTo(followingUids) { it.key }

                // Update the 'isFollowing' flag on the master list
                allUserList.forEach { user ->
                    user.isFollowing = followingUids.contains(user.uid)
                }

                // All data ready: apply the initial filter and display the data
                applyFiltersAndSearch(searchView.query.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FindPeople", "Failed to fetch following status: ${error.message}")
                // Display the user list even if follow status failed
                applyFiltersAndSearch(searchView.query.toString())
            }
        })
    }

    // ----------------------------------------------------------------------------------
    // FOLLOW BUTTON AND NOTIFICATION LOGIC (MODIFIED FOR UI UPDATE)
    // ----------------------------------------------------------------------------------

    /**
     * Handles the follow/unfollow Firebase transaction.
     */
    private fun onFollowButtonClicked(user: User, position: Int) {
        val targetUserId = user.uid
        val followRef = database.getReference("following").child(currentUserId).child(targetUserId)

        if (user.isFollowing) {
            // UNFOLLOW
            followRef.removeValue()
                .addOnSuccessListener {
                    user.isFollowing = false
                    followingUids.remove(targetUserId) // Update local set
                    userAdapter.notifyItemChanged(position)
                    Toast.makeText(this, "Unfollowed ${user.name}", Toast.LENGTH_SHORT).show()
                    applyFiltersAndSearch(searchView.query.toString()) // Re-apply filter
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Unfollow failed.", Toast.LENGTH_SHORT).show()
                }
        } else {
            // FOLLOW
            val followData = mapOf("timestamp" to System.currentTimeMillis())
            followRef.setValue(followData)
                .addOnSuccessListener {
                    user.isFollowing = true
                    followingUids.add(targetUserId) // Update local set
                    userAdapter.notifyItemChanged(position)
                    Toast.makeText(this, "Following ${user.name}", Toast.LENGTH_SHORT).show()
                    applyFiltersAndSearch(searchView.query.toString()) // Re-apply filter

                    // CRITICAL: Trigger the push notification payload log (existing code)
                    triggerNewFollowerNotification(user, currentUserId)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Follow failed.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Simulates the secure backend call by logging the FCM payload. (Original logic)
     */
    private fun triggerNewFollowerNotification(followedUser: User, followerUid: String) {
        val targetToken = followedUser.fcmToken

        if (targetToken.isNullOrEmpty()) {
            Log.w("FCM_FOLLOW", "User ${followedUser.name} has no FCM token. Notification skipped.")
            return
        }

        val notificationData = """
            {
                "to": "$targetToken",
                "notification": {
                    "title": "New Follower!",
                    "body": "$currentUserName started following you.",
                    "sound": "default"
                },
                "data": {
                    "type": "NEW_FOLLOWER",
                    "follower_uid": "$followerUid",
                    "follower_name": "$currentUserName"
                }
            }
        """.trimIndent()

        Log.i("FCM_FOLLOW", "--------------------------------------------------------")
        Log.i("FCM_FOLLOW", "BACKEND SIMULATION: New Follower Notification Triggered!")
        Log.i("FCM_FOLLOW", "Payload SENT TO SECURE BACKEND:\n$notificationData")
        Log.i("FCM_FOLLOW", "--------------------------------------------------------")
    }
}