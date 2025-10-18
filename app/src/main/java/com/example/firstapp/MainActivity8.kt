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
import kotlin.collections.ArrayList
import User


class MainActivity8 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatListAdapter
    private val mutualFollowList = ArrayList<User>()

    private val mutuallyFollowedUids = ArrayList<String>()
    private var profileFetchCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main8)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        chatRecyclerView = findViewById(R.id.chat_list_recycler_view)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        chatAdapter = ChatListAdapter(mutualFollowList, ::onChatClicked)
        chatRecyclerView.adapter = chatAdapter

        fetchMutualFollowingUsers()

        findViewById<ImageView>(R.id.image1)?.setOnClickListener {
            finish()
        }
    }

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

    private fun checkIfFollowedBack(currentUserId: String, followingUids: List<String>) {
        mutuallyFollowedUids.clear()

        var checksPending = followingUids.size

        for (targetUid in followingUids) {
            val followedBackRef = database.getReference("following").child(targetUid).child(currentUserId)

            followedBackRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        mutuallyFollowedUids.add(targetUid)
                    }

                    checksPending--
                    if (checksPending == 0) {
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

    private fun startProfileFetch() {
        mutualFollowList.clear()
        profileFetchCounter = mutuallyFollowedUids.size

        if (profileFetchCounter == 0) {
            chatAdapter.notifyDataSetChanged()
            return
        }

        for (uid in mutuallyFollowedUids) {
            fetchProfileDetails(uid)
        }
    }

    /**
     * Fetches the name and profile picture of the mutually followed user and adds them to the list.
     */
    private fun fetchProfileDetails(uid: String) {
        val userRef = database.getReference("users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                val email = snapshot.child("email").getValue(String::class.java) ?: ""

                // Fetch the profile picture Base64 string
                val profilePic = snapshot.child("profilePicture").getValue(String::class.java)


                mutualFollowList.add(User(
                    uid = uid,
                    name = name,
                    email = email,
                    profilePictureBase64 = profilePic, // <--- Fetched and stored
                    isFollowing = true
                ))

                profileFetchCounter--
                if (profileFetchCounter == 0) {
                    mutualFollowList.sortBy { it.name }
                    chatAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatList", "Failed to fetch profile for $uid: ${error.message}")

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
        val intent = Intent(this, MainActivity9::class.java).apply {
            putExtra("RECIPIENT_USER_ID", user.uid)
            putExtra("RECIPIENT_NAME", user.name)
        }
        startActivity(intent)
    }
}
