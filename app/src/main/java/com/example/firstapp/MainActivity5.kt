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
// Assuming Post, Story, User, and DisplayStory are imported from DataModels.kt or defined elsewhere in the package

const val STATIC_POST_ID = "JOSHUA_I_TOKYO_POST"

class MainActivity5 : AppCompatActivity() {

    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var bottomNavProfileImage: CircleImageView
    private lateinit var feedRecyclerView: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private val feedPostsList = mutableListOf<Post>() // Assuming Post is defined

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Story Expiration: 30 seconds for quick testing. Change to 86400000L for 24 hours.
    private val STORY_EXPIRATION_MS = 86400 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_feed)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // --- 1. EXISTING NAVIGATION INTENTS (unchanged) ---
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
        bottomNavProfileImage.setOnClickListener {
            val intent = Intent(this, MainActivity13::class.java)
            startActivity(intent)
        }
        loadMyProfilePicture()

        // --- 3. STORIES FEATURE INITIALIZATION ---
        storiesRecyclerView = findViewById(R.id.stories_recycler_view)
        storiesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val initialStoryList = mutableListOf<DisplayStory>()
        storyAdapter = StoryAdapter(initialStoryList) { story ->

            // Story Click Handler (FIXED to use MainActivity20 for all viewing)
            val isYourStoryPlaceholder = story.id == "YOUR_STORY_PLACEHOLDER"
            val hasActiveStory = story.imageUrl.isNotEmpty() && story.imageUrl.length > 100

            if (isYourStoryPlaceholder) {
                if (hasActiveStory) {
                    // YOUR STORY: Active -> Open Story Player/Manager (MainActivity20)
                    val intent = Intent(this, MainActivity20::class.java).apply {
                        putExtra("VIEW_USER_ID", story.userId)
                    }
                    startActivity(intent)
                } else {
                    // YOUR STORY: Empty -> Open Story Creation (MainActivity17)
                    startActivity(Intent(this, MainActivity17::class.java))
                }
            } else {
                // FRIEND'S STORY (e.g., Sohaib's): Open Story Player (MainActivity20)
                val intent = Intent(this, MainActivity20::class.java).apply {
                    putExtra("VIEW_USER_ID", story.userId)
                }
                startActivity(intent)
            }
        }
        storiesRecyclerView.adapter = storyAdapter

        // --- 4. POST FEED INITIALIZATION (unchanged) ---
        feedRecyclerView = findViewById(R.id.recylerView)
        feedRecyclerView.layoutManager = LinearLayoutManager(this)
        feedAdapter = FeedAdapter(feedPostsList)
        feedRecyclerView.adapter = feedAdapter

        // --- 5. START DATA FETCHING ---
        loadStoriesFromFirebase()
        fetchUserFeed()
    }

    // ---------------------------------------------------------------------------------------------
    // PROFILE PICTURE LOADING (Unchanged)
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
                            bottomNavProfileImage.setImageBitmap(bitmap)

                        } catch (e: IllegalArgumentException) {
                            Log.e("MainActivity5", "Invalid Base64 for profile picture: ${e.message}")
                            bottomNavProfileImage.setImageResource(R.drawable.person2)
                        }
                    } else {
                        bottomNavProfileImage.setImageResource(R.drawable.person2)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity5", "Failed to read user profile data: ${error.message}")
                }
            })
    }

    // ---------------------------------------------------------------------------------------------
    // ⭐ CORE STORIES LOGIC (Unique, Filtered, Expiring) ⭐
    // ---------------------------------------------------------------------------------------------

    private fun loadStoriesFromFirebase() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            storyAdapter.updateDisplayStories(emptyList())
            return
        }

        // 1. Get the list of users the current user is following (and self)
        getFollowingList(currentUserId) { followedUsers ->
            val usersToFetch = followedUsers.toMutableSet().apply { add(currentUserId) }.toList()

            // 2. Fetch the User data (Profile/Username) for these users.
            fetchUserDataAndStories(usersToFetch, currentUserId)
        }
    }

    private fun fetchUserDataAndStories(userIds: List<String>, currentUserId: String) {
        if (userIds.isEmpty()) return

        val usersRef = database.getReference("users")
        val usersMap = mutableMapOf<String, User>()
        var pendingUserQueries = userIds.size

        // Fetch User Data for all involved users
        for (userId in userIds) {
            usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)?.copy(uid = userId)
                        ?: User(uid = userId, name = "User Missing")

                    usersMap[userId] = user

                    pendingUserQueries--
                    if (pendingUserQueries == 0) {
                        fetchLatestStories(usersMap, currentUserId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity5", "Failed to fetch user data for $userId: ${error.message}")
                    pendingUserQueries--
                    if (pendingUserQueries == 0) {
                        fetchLatestStories(usersMap, currentUserId)
                    }
                }
            })
        }
    }

    private fun fetchLatestStories(usersMap: Map<String, User>, currentUserId: String) {
        val storiesRef = database.getReference("stories")
        val currentTime = System.currentTimeMillis()
        val expirationThreshold = currentTime - STORY_EXPIRATION_MS

        storiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latestStories = mutableMapOf<String, Story>()
                val expiredStoryIds = mutableListOf<String>()

                for (storySnapshot in snapshot.children) {
                    // Get the story and its ID
                    val story = storySnapshot.getValue(Story::class.java)?.copy(id = storySnapshot.key!!)

                    if (story != null && usersMap.containsKey(story.userId)) {

                        if (story.timestamp > expirationThreshold) {
                            // Active Story: Keep the LATEST story for each user (ensures uniqueness/no overlap)
                            val existingStory = latestStories[story.userId]
                            if (existingStory == null || story.timestamp > existingStory.timestamp) {
                                latestStories[story.userId] = story
                            }
                        } else {
                            // Expired Story: Mark for deletion
                            expiredStoryIds.add(story.id)
                        }
                    }
                }

                // Execute the deletion of expired stories
                deleteExpiredStories(expiredStoryIds)

                // Construct the final list of DisplayStory objects
                val finalDisplayStories = mutableListOf<DisplayStory>()

                // 1. Handle "Your Story" Placeholder
                val ownUser = usersMap[currentUserId] ?: User(uid = currentUserId, name = "Your Story")
                val ownStory = latestStories[currentUserId]

                val yourStoryPlaceholder = DisplayStory(
                    story = Story(
                        id = "YOUR_STORY_PLACEHOLDER",
                        userId = currentUserId,
                        imageUrl = ownStory?.imageUrl ?: ""
                    ),
                    user = ownUser.copy(name = "Your Story") // Ensure "Your Story" label is used
                )
                finalDisplayStories.add(yourStoryPlaceholder)

                // 2. Add Followed Users' Stories (only users who have a non-expired story)
                val followedUsersStories = latestStories.values
                    .filter { it.userId != currentUserId }
                    .mapNotNull { story ->
                        usersMap[story.userId]?.let { user ->
                            DisplayStory(story, user)
                        }
                    }
                    .sortedByDescending { it.story.timestamp } // Sort by newest story first

                finalDisplayStories.addAll(followedUsersStories)

                storyAdapter.updateDisplayStories(finalDisplayStories)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity5", "Failed to fetch stories: ${error.message}")
                storyAdapter.updateDisplayStories(emptyList())
            }
        })
    }

    private fun deleteExpiredStories(expiredStoryIds: List<String>) {
        val storiesRef = database.getReference("stories")

        expiredStoryIds.forEach { storyId ->
            storiesRef.child(storyId).removeValue()
                .addOnFailureListener { e ->
                    Log.e("MainActivity5", "Failed to delete expired story: $storyId, Error: ${e.message}")
                }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // POST FEED LOGIC (Unchanged for this solution)
    // ---------------------------------------------------------------------------------------------

    private fun fetchUserFeed() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            displayDefaultPost()
            return
        }
        getFollowingList(currentUserId) { followedUsers ->
            fetchPostsFromFollowedUsers(followedUsers)
        }
    }

    private fun getFollowingList(currentUserId: String, callback: (List<String>) -> Unit) {
        val followingRef = database.getReference("following").child(currentUserId)
        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followedUsers = mutableListOf<String>()
                for (child in snapshot.children) {
                    followedUsers.add(child.key!!)
                }
                callback(followedUsers)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity5", "Failed to read following list: ${error.message}")
                callback(emptyList())
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