package com.example.firstapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import Post
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

// NOTE: Ensure 'Post' data class and 'PostsAdapter' class are correctly defined

class MainActivity13 : AppCompatActivity() {

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postsAdapter: PostsAdapter
    private val postsList = mutableListOf<Post>()
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    // References for User Info
    private lateinit var profileNameTextView: TextView
    private lateinit var profileImageView: CircleImageView

    // References for the Count TextViews using the unique IDs: R.id.posts, R.id.followers, R.id.following
    private lateinit var postsCountTextView: TextView
    private lateinit var followersCountTextView: TextView
    private lateinit var followingCountTextView: TextView

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main13)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        // --- View Initialization ---
        profileNameTextView = findViewById(R.id.text2_1)
        profileImageView = findViewById(R.id.profileImage)
        val editProfileButton = findViewById<TextView>(R.id.text2_5)

        // Initialize the Count TextViews with the correct, unique IDs
        postsCountTextView = findViewById(R.id.posts)
        followersCountTextView = findViewById(R.id.followers)
        followingCountTextView = findViewById(R.id.following)

        // --- Click Handlers ---
        findViewById<ImageView>(R.id.image3).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity3::class.java))
            finish()
        }

        editProfileButton.setOnClickListener {
            val intent = Intent(this, MainActivity15::class.java)
            editProfileLauncher.launch(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup RecyclerView for Posts
        postsRecyclerView = findViewById(R.id.posts_recycler_view)
        postsRecyclerView.layoutManager = GridLayoutManager(this, 3)

        postsAdapter = PostsAdapter(postsList)
        postsRecyclerView.adapter = postsAdapter

        // Start data fetching
        fetchUserProfileData()

        findViewById<ImageView>(R.id.homeIcon).setOnClickListener {
            startActivity(Intent(this, MainActivity5::class.java))
        }
    }

    //----------------------------------------------------------------------------------------------
    // Data Fetching Logic
    //----------------------------------------------------------------------------------------------

    private fun fetchUserProfileData() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            profileNameTextView.text = "Guest User"
            Log.e("MainActivity13", "User not logged in.")
            return
        }

        fetchUserNameAndPicture(currentUserId)
        fetchUserPosts(currentUserId)
        fetchFollowCounts(currentUserId)
    }

    private fun fetchUserNameAndPicture(userId: String) {
        val userRef = database.getReference("users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                profileNameTextView.text = name ?: (auth.currentUser?.email ?: "User Profile")

                val base64Pic = snapshot.child("profilePicture").getValue(String::class.java)

                if (base64Pic != null && base64Pic.isNotBlank()) {
                    try {
                        val imageBytes = Base64.decode(base64Pic, Base64.NO_WRAP)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        profileImageView.setImageBitmap(bitmap)
                    } catch (e: IllegalArgumentException) {
                        Log.e("Profile", "Invalid Base64 for profile picture: ${e.message}")
                        profileImageView.setImageResource(R.drawable.person)
                    }
                } else {
                    profileImageView.setImageResource(R.drawable.person)
                }
            }

            override fun onCancelled(error: DatabaseError) {
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

                val postCount = snapshot.childrenCount
                postsCountTextView.text = postCount.toString()

                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        val postId = postSnapshot.key
                        postsList.add(it.copy(postId = postId ?: ""))
                    }
                }

                postsList.reverse()
                postsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity13", "Failed to read posts from Firebase.", error.toException())
            }
        })
    }

    /**
     * Fetches the real-time count for Following and Followers.
     * The Followers count uses a query scan since the dedicated 'followers' node is missing.
     */
    private fun fetchFollowCounts(currentUserId: String) {

        // 1. Get FOLLOWING count (R.id.following) - Path remains the same, works correctly.
        val followingRef = database.getReference("following").child(currentUserId)
        followingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followingCount = snapshot.childrenCount
                followingCountTextView.text = followingCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FollowCounts", "Failed to read following count.", error.toException())
            }
        })

        // 2. Get FOLLOWERS count (R.id.followers) - Using query scan against 'following' node.
        val allFollowingRef = database.getReference("following")
        allFollowingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var followersCount = 0

                // Iterate through every user's 'following' list (e.g., Bilal, Sohaib, Abdulrehman)
                for (userSnapshot in snapshot.children) {
                    // Check if the current user (e.g., Bilal's profile being viewed)
                    // is listed as a child in this user's (e.g., Sohaib's) 'following' list.
                    if (userSnapshot.hasChild(currentUserId)) {
                        // If true, the userSnapshot.key is a FOLLOWER.
                        followersCount++
                    }
                }

                followersCountTextView.text = followersCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FollowCounts", "Failed to read followers count via query.", error.toException())
            }
        })
    }
}