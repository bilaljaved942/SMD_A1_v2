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
import User // Ensure this import is correct
import kotlin.collections.ArrayList

class MainActivity22 : AppCompatActivity() { // <-- CORRECTED CLASS NAME

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main22) // Loads the correct layout file

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Assuming R.id.main_find_people is the root view ID for proper insets handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_find_people)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Navigation Handler ---
        findViewById<ImageView>(R.id.back_arrow)?.setOnClickListener {
            finish()
        }

        // --- Setup RecyclerView ---
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

                for (userSnapshot in snapshot.children) {
                    val uid = userSnapshot.key

                    // Filter out the current user and ensure UID exists
                    if (uid != null && uid != currentUserId) {
                        // Use "New User" as a fallback name if registration failed to save it
                        val name = userSnapshot.child("name").getValue(String::class.java) ?: "New User"
                        val email = userSnapshot.child("email").getValue(String::class.java) ?: ""

                        userList.add(User(uid = uid, name = name, email = email, isFollowing = false))
                    }
                }

                // Now that we have the list, check the following status
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
