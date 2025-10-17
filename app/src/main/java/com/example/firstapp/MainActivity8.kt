package com.example.firstapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
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
import User
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity8 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatListAdapter
    private val mutualFollowList = ArrayList<User>()

    // Use a temporary list to collect users who follow back before fetching their profiles
    private val mutuallyFollowedUids = ArrayList<String>()
    private var profileFetchCounter = 0 // Tracks pending profile detail fetches

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main8)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // --- Setup Window Insets ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Setup RecyclerView ---
        chatRecyclerView = findViewById(R.id.chat_list_recycler_view)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        chatAdapter = ChatListAdapter(mutualFollowList, ::onChatClicked)
        chatRecyclerView.adapter = chatAdapter

        fetchMutualFollowingUsers()

        // --- Navigation Fix: Back Button (image1) ---
        findViewById<ImageView>(R.id.image1)?.setOnClickListener {
            finish()
        }
    }

    /**
     * Step 1: Fetches users the current user is following.
     */
    private fun fetchMutualFollowingUsers() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) return

        val followingRef = database.getReference("following").child(currentUserId)

        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val myFollowingUids = snapshot.children.mapNotNull { it.key }

                if (myFollowingUids.isEmpty()) {
                    chatAdapter.notifyDataSetChanged()
                    return
                }

                checkIfFollowedBack(currentUserId, myFollowingUids)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatList", "Failed to fetch my following list: ${error.message}")
            }
        })
    }

    /**
     * Step 2: Checks if the users I follow also follow me back (mutual check).
     */
    private fun checkIfFollowedBack(currentUserId: String, followingUids: List<String>) {
        mutuallyFollowedUids.clear()

        var checksPending = followingUids.size

        for (targetUid in followingUids) {
            val followedBackRef = database.getReference("following").child(targetUid).child(currentUserId)

            followedBackRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // MUTUAL FOLLOW FOUND! Collect UID for later profile fetch.
                        mutuallyFollowedUids.add(targetUid)
                    }

                    checksPending--
                    if (checksPending == 0) {
                        // All mutual checks are done. Now start fetching their names.
                        startProfileFetch()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatList", "Mutual check failed for $targetUid: ${error.message}")
                    checksPending--
                    if (checksPending == 0) startProfileFetch()
                }
            })
        }
    }

    /**
     * Step 3 (NEW): Initiates fetching the names and emails for all mutual users.
     */
    private fun startProfileFetch() {
        mutualFollowList.clear()
        profileFetchCounter = mutuallyFollowedUids.size

        if (profileFetchCounter == 0) {
            // No mutual follows found.
            chatAdapter.notifyDataSetChanged()
            return
        }

        // Start fetching details for every UID we found
        for (uid in mutuallyFollowedUids) {
            fetchProfileDetails(uid)
        }
    }

    /**
     * Step 4: Fetches the name of the mutually followed user and adds them to the list.
     */
    private fun fetchProfileDetails(uid: String) {
        val userRef = database.getReference("users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                val email = snapshot.child("email").getValue(String::class.java) ?: ""

                // Add the fully constructed User object to the final list
                mutualFollowList.add(User(uid = uid, name = name, email = email, isFollowing = true))

                // Decrement the counter and check if we are done
                profileFetchCounter--
                if (profileFetchCounter == 0) {
                    // FINAL STEP: All data is ready, update the UI
                    chatAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatList", "Failed to fetch name for $uid: ${error.message}")

                // Even on error, decrement the counter to ensure the list eventually loads
                profileFetchCounter--
                if (profileFetchCounter == 0) {
                    chatAdapter.notifyDataSetChanged()
                }
            }
        })
    }

    /**
     * Handles the click on a chat row, starting the chat activity (MainActivity9).
     */
    private fun onChatClicked(user: User) {
        // Launch MainActivity9, passing the recipient's UID and name
        val intent = Intent(this, MainActivity9::class.java).apply {
            putExtra("RECIPIENT_ID", user.uid)
            putExtra("RECIPIENT_NAME", user.name)
        }
        startActivity(intent)
    }
}
