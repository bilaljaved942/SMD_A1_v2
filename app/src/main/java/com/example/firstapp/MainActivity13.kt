package com.example.firstapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import Post
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.auth.FirebaseAuth

class MainActivity13 : AppCompatActivity() {

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postsAdapter: PostsAdapter
    private val postsList = mutableListOf<Post>()
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    // Reference for the name TextView (using the specified ID: text2_1)
    private lateinit var profileNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main13)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        var logout=findViewById<ImageView>(R.id.image3)
        logout.setOnClickListener {
            startActivity(Intent(this, MainActivity3::class.java))
            finish()
        }

        // --- 1. Initialize Name TextView ---
        profileNameTextView = findViewById(R.id.text2_1)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        postsRecyclerView = findViewById(R.id.posts_recycler_view)
        postsRecyclerView.layoutManager = GridLayoutManager(this, 3)

        postsAdapter = PostsAdapter(postsList)
        postsRecyclerView.adapter = postsAdapter

        // Start fetching user details (name) and posts
        fetchUserProfileAndPosts()

        // --- Your Existing Navigation Logic ---
        findViewById<ImageView>(R.id.homeIcon).setOnClickListener {
            startActivity(Intent(this, MainActivity5::class.java))
        }
        // ... (other navigation code)
    }

    private fun fetchUserProfileAndPosts() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            // Set a default state for non-logged-in users
            profileNameTextView.text = "Guest User"
            Log.e("MainActivity13", "User not logged in. Cannot fetch profile details.")
            postsList.clear()
            postsAdapter.notifyDataSetChanged()
            return
        }

        // Fetch and display name
        fetchUserName(currentUserId)

        // Fetch and display posts
        fetchUserPosts(currentUserId)
    }

    /**
     * Fetches the user's name from /users/{userId} and updates the TextView (text2_1).
     */
    private fun fetchUserName(userId: String) {
        // Look up the profile under the dedicated 'users' node
        val userRef = database.getReference("users").child(userId)
        profileNameTextView.text = "Loading..."

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Fetch the "name" field saved during registration
                val name = snapshot.child("name").getValue(String::class.java)

                if (name != null && name.isNotBlank()) {
                    profileNameTextView.text = name
                } else {
                    // Fallback to email if name is missing
                    val email = auth.currentUser?.email
                    profileNameTextView.text = email ?: "User Profile"
                    Log.w("Profile", "Name field not found, using email as fallback.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                profileNameTextView.text = "Error"
                Log.e("Profile", "Failed to read user data: ${error.message}")
            }
        })
    }

    private fun fetchUserPosts(currentUserId: String) {
        val postsRef = database.getReference("images")
        val userPostsQuery = postsRef.orderByChild("userId").equalTo(currentUserId)

        userPostsQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        val postId = postSnapshot.key
                        postsList.add(it.copy(postId = postId ?: ""))
                    }
                }

                postsList.reverse()
                postsAdapter.notifyDataSetChanged()

                Log.d("MainActivity13", "Fetched ${postsList.size} posts for user $currentUserId.")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity13", "Failed to read posts from Firebase.", error.toException())
            }
        })
    }
}
