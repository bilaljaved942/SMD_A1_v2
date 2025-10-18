package com.example.firstapp

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
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
import User
// User and UserAdapter assumed to be accessible in this package

class MainActivity22 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main22)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_find_people)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.back_arrow)?.setOnClickListener {
            finish()
        }

        usersRecyclerView = findViewById(R.id.users_recycler_view)
        usersRecyclerView.layoutManager = LinearLayoutManager(this)

        val currentUserId = auth.currentUser?.uid ?: ""
        userAdapter = UserAdapter(userList, currentUserId, ::onFollowButtonClicked)
        usersRecyclerView.adapter = userAdapter

        fetchUsers()
    }

    /**
     * Fetches all user profiles from the /users node, excluding the current user.
     */
    private fun fetchUsers() {
        val usersRef = database.getReference("users")
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "Authentication error. Please log in.", Toast.LENGTH_LONG).show()
            return
        }

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                val processedUids = HashSet<String>()

                for (userSnapshot in snapshot.children) {
                    val uid = userSnapshot.key

                    if (uid != null && uid != currentUserId && !processedUids.contains(uid)) {
                        val name = userSnapshot.child("name").getValue(String::class.java)

                        if (name != null && name.isNotBlank()) {
                            val email = userSnapshot.child("email").getValue(String::class.java) ?: ""

                            // CRITICAL: Fetch the profile picture Base64 string
                            val profilePic = userSnapshot.child("profilePicture").getValue(String::class.java)

                            userList.add(User(
                                uid = uid,
                                name = name,
                                email = email,
                                profilePictureBase64 = profilePic, // <-- ADDED
                                isFollowing = false
                            ))
                            processedUids.add(uid)
                        } else {
                            Log.w("FindPeople", "Skipped user with UID $uid due to missing name.")
                        }
                    }
                }

                fetchFollowingStatus(currentUserId)
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
    private fun fetchFollowingStatus(currentUserId: String) {
        val followingRef = database.getReference("following").child(currentUserId)

        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followingUids = snapshot.children.mapNotNull { it.key }

                userList.forEach { user ->
                    user.isFollowing = followingUids.contains(user.uid)
                }

                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FindPeople", "Failed to fetch following status: ${error.message}")
                userAdapter.notifyDataSetChanged()
            }
        })
    }

    /**
     * Handles the follow/unfollow Firebase transaction.
     */
    private fun onFollowButtonClicked(user: User, position: Int) {
        val currentUserId = auth.currentUser?.uid ?: return
        val targetUserId = user.uid
        val followRef = database.getReference("following").child(currentUserId).child(targetUserId)

        if (user.isFollowing) {
            // UNFOLLOW
            followRef.removeValue()
                .addOnSuccessListener {
                    user.isFollowing = false
                    userAdapter.notifyItemChanged(position)
                    Toast.makeText(this, "Unfollowed ${user.name}", Toast.LENGTH_SHORT).show()
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
                    userAdapter.notifyItemChanged(position)
                    Toast.makeText(this, "Following ${user.name}", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Follow failed.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
