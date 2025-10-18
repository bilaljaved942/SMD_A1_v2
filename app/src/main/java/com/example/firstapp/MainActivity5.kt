package com.example.firstapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import Post

const val STATIC_POST_ID = "JOSHUA_I_TOKYO_POST"

class MainActivity5 : AppCompatActivity() {

    // --- STORY PROPERTIES (Existing) ---
    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var bottomNavProfileImage: CircleImageView

    // --- POST FEED PROPERTIES (New) ---
    private lateinit var feedRecyclerView: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private val feedPostsList = mutableListOf<Post>()

    // --- FIREBASE ---
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_feed)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // --- 1. EXISTING NAVIGATION INTENTS ---
        findViewById<ImageView>(R.id.forward).setOnClickListener {
            startActivity(Intent(this, MainActivity8::class.java))
        }

        findViewById<ImageView>(R.id.batteryIcon).setOnClickListener {
            startActivity(Intent(this, MainActivity22::class.java))
        }

        findViewById<ImageView>(R.id.homeIcon3).setOnClickListener {
            startActivity(Intent(this, MainActivity16::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- 2. INITIALIZE PROFILE IMAGES & NAVIGATION ---
        bottomNavProfileImage = findViewById(R.id.profileImage3)

        // Navigation to own profile
        bottomNavProfileImage.setOnClickListener {
            val intent = Intent(this, MainActivity13::class.java)
            startActivity(intent)
        }

        loadMyProfilePicture()

        // --- 3. STORIES FEATURE INITIALIZATION (Existing) ---
        storiesRecyclerView = findViewById(R.id.stories_recycler_view)
        storiesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val initialStoryList = mutableListOf<Story>()
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            initialStoryList.add(Story(id = "YOUR_STORY_PLACEHOLDER", userId = currentUserId))
        }

        storyAdapter = StoryAdapter(initialStoryList) { story ->
            if (story.id == "YOUR_STORY_PLACEHOLDER") {
                if (story.imageUrl.isNotEmpty() && story.imageUrl.length > 100) {
                    val intent = Intent(this, MainActivity20::class.java).apply {
                        putExtra("VIEW_USER_ID", story.userId)
                    }
                    startActivity(intent)
                } else {
                    startActivity(Intent(this, MainActivity17::class.java))
                }
            } else {
                val intent = Intent(this, MainActivity18::class.java).apply {
                    putExtra("VIEW_USER_ID", story.userId)
                }
                startActivity(intent)
            }
        }
        storiesRecyclerView.adapter = storyAdapter

        // --- 4. POST FEED INITIALIZATION (New) ---
        feedRecyclerView = findViewById(R.id.recylerView)
        feedRecyclerView.layoutManager = LinearLayoutManager(this)
        feedAdapter = FeedAdapter(feedPostsList)
        feedRecyclerView.adapter = feedAdapter

        // --- 5. START DATA FETCHING ---
        loadStoriesFromFirebase()
        fetchUserFeed()
    }

    // ---------------------------------------------------------------------------------------------
    // EXISTING PROFILE & STORY LOGIC (Unchanged)
    // ---------------------------------------------------------------------------------------------

    private fun loadMyProfilePicture() {
        val currentUserId = auth.currentUser?.uid ?: return

        database.getReference("users").child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val base64Pic = snapshot.child("profilePicture").getValue(String::class.java)

                    if (base64Pic != null && base64Pic.isNotBlank()) {
                        try {
                            val imageBytes = Base64.decode(base64Pic, Base64.NO_WRAP)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                            storyAdapter.updateProfilePicture(base64Pic)
                            bottomNavProfileImage.setImageBitmap(bitmap)

                        } catch (e: IllegalArgumentException) {
                            Log.e("MainActivity5", "Invalid Base64 for profile picture: ${e.message}")
                            storyAdapter.updateProfilePicture(null)
                            bottomNavProfileImage.setImageResource(R.drawable.person2)
                        }
                    } else {
                        storyAdapter.updateProfilePicture(null)
                        bottomNavProfileImage.setImageResource(R.drawable.person2)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity5", "Failed to read user profile data: ${error.message}")
                }
            })
    }

    private fun loadStoriesFromFirebase() {
        val currentTime = System.currentTimeMillis()
        val expirationTimeMs = 15 * 1000L
        val expirationThreshold = currentTime - expirationTimeMs
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        FirebaseDatabase.getInstance().getReference("stories")
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val allStories = mutableListOf<Story>()

                    for (storySnapshot in snapshot.children) {
                        val story = storySnapshot.getValue(Story::class.java)
                        if (story != null && story.timestamp > expirationThreshold) {
                            allStories.add(story)
                        }
                    }

                    val finalStoriesList = mutableListOf<Story>()
                    if (currentUserId != null) {
                        finalStoriesList.add(Story(id = "YOUR_STORY_PLACEHOLDER", userId = currentUserId))
                    }

                    finalStoriesList.addAll(allStories)
                    storyAdapter.updateStories(finalStoriesList)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    // ---------------------------------------------------------------------------------------------
    // NEW POST FEED LOGIC (MODIFIED)
    // ---------------------------------------------------------------------------------------------

    private fun fetchUserFeed() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            displayDefaultPost()
            return
        }

        // Step 1: Get the list of users the current user is following (and self)
        getFollowingList(currentUserId) { followedUsers ->
            // Step 2: Fetch posts from those users
            fetchPostsFromFollowedUsers(followedUsers)
        }
    }

    private fun getFollowingList(currentUserId: String, callback: (List<String>) -> Unit) {
        val followingRef = database.getReference("following").child(currentUserId)

        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followedUsers = mutableListOf<String>()

                // --- FIX APPLIED HERE: DO NOT add currentUserId ---
                // The list will only contain UIDs of people the current user follows.
                // followedUsers.add(currentUserId) <--- REMOVED THIS LINE

                for (child in snapshot.children) {
                    followedUsers.add(child.key!!)
                }

                // If the user isn't following anyone, this list is empty, triggering the default post.
                callback(followedUsers)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity5", "Failed to read following list: ${error.message}")
                callback(emptyList()) // Return empty list to prevent self-post display
            }
        })
    }

    private fun fetchPostsFromFollowedUsers(followedUsers: List<String>) {
        if (followedUsers.isEmpty()) {
            displayDefaultPost()
            return
        }

        val postsRef = database.getReference("images")
        val fetchedPosts = mutableListOf<Post>()
        var pendingQueries = followedUsers.size

        for (userId in followedUsers) {
            postsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (postSnapshot in snapshot.children) {
                            val post = postSnapshot.getValue(Post::class.java)
                            post?.let { fetchedPosts.add(it) }
                        }

                        pendingQueries--
                        if (pendingQueries == 0) {
                            updateFeed(fetchedPosts)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("MainActivity5", "Failed to fetch posts for user $userId.")
                        pendingQueries--
                        if (pendingQueries == 0) {
                            updateFeed(fetchedPosts)
                        }
                    }
                })
        }
    }

    private fun updateFeed(posts: List<Post>) {
        feedPostsList.clear()

        if (posts.isEmpty()) {
            displayDefaultPost()
        } else {
            val sortedPosts = posts.sortedByDescending { it.timestamp }
            feedPostsList.addAll(sortedPosts)
            feedAdapter.notifyDataSetChanged()
        }
    }

    private fun displayDefaultPost() {
        feedPostsList.clear()

        val defaultPost = Post(
            postId = "default_placeholder",
            userId = "admin_socially",
            base64Image = "",
            caption = "Welcome to Socially! Follow friends like Bilal, Sohaib, and Abdulrehman to populate your feed, or create your first post!",
            timestamp = System.currentTimeMillis()
        )

        feedPostsList.add(defaultPost)
        feedAdapter.notifyDataSetChanged()
    }
}
