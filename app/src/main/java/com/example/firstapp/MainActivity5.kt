package com.example.firstapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.firstapp.data.local.entities.PostEntity
import com.example.firstapp.data.local.entities.StoryEntity
import com.example.firstapp.repository.PostRepository
import com.example.firstapp.repository.StoryRepository
import com.example.firstapp.repository.AuthRepository
import com.example.firstapp.utils.SecurePreferences
import com.example.firstapp.utils.NetworkUtils
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import Post

/**
 * MainActivity5 - Home/Feed Activity (REFACTORED FOR REST APIs)
 * 
 * This activity now uses:
 * - PostRepository for feed posts
 * - StoryRepository for stories
 * - Room database for offline caching
 * - No more Firebase dependencies!
 */
class MainActivity5 : AppCompatActivity() {

    // Repositories
    private lateinit var postRepository: PostRepository
    private lateinit var storyRepository: StoryRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var prefs: SecurePreferences

    // UI Components
    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var bottomNavProfileImage: CircleImageView
    private lateinit var feedRecyclerView: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var loadingProgressBar: ProgressBar

    // Data
    private val feedPostsList = mutableListOf<Post>()
    private var currentPage = 1
    private var isLoading = false

    private val STORY_EXPIRATION_MS = 86400 * 1000L

    // Screenshot monitor
    private var screenshotMonitor: com.example.firstapp.utils.ScreenshotMonitor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_feed)

        // Initialize repositories
        postRepository = PostRepository(this)
        storyRepository = StoryRepository(this)
        authRepository = AuthRepository(this)
        prefs = SecurePreferences(this)

        // Check if logged in
        if (!prefs.isLoggedIn()) {
            navigateToLogin()
            return
        }

        // Setup navigation
        setupNavigation()
        
        // Setup UI
        setupViews()
        
        // Load data
        loadMyProfilePicture()
        loadStories()
        loadFeed()
    }

    private fun setupNavigation() {
        findViewById<ImageView>(R.id.forward).setOnClickListener {
            startActivity(Intent(this, MainActivity8::class.java))
        }
        
        findViewById<ImageView>(R.id.homeIcon3).setOnClickListener {
            startActivity(Intent(this, MainActivity16::class.java))
        }
        
        findViewById<ImageView>(R.id.batteryIcon).setOnClickListener {
            startActivity(Intent(this, MainActivity22::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupViews() {
        // Bottom navigation profile
        bottomNavProfileImage = findViewById(R.id.profileImage3)
        bottomNavProfileImage.setOnClickListener {
            val intent = Intent(this, MainActivity13::class.java)
            startActivity(intent)
        }

        // Loading indicator (if exists in layout)
        // loadingProgressBar = findViewById(R.id.loadingProgressBar) // Add to layout if needed

        // Swipe refresh (if exists in layout)
        // swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout) // Add to layout if needed
        // swipeRefreshLayout.setOnRefreshListener {
        //     refreshData()
        // }

        // Stories RecyclerView
        storiesRecyclerView = findViewById(R.id.stories_recycler_view)
        storiesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val initialStoryList = mutableListOf<DisplayStory>()
        storyAdapter = StoryAdapter(initialStoryList) { displayStory ->
            val isYourStoryPlaceholder = displayStory.story.id == "YOUR_STORY_PLACEHOLDER"
            val hasActiveStory = displayStory.story.imageUrl.isNotEmpty() && displayStory.story.imageUrl.length > 100

            if (isYourStoryPlaceholder) {
                if (hasActiveStory) {
                    val intent = Intent(this, MainActivity20::class.java).apply {
                        putExtra("VIEW_USER_ID", prefs.getUserId())
                    }
                    startActivity(intent)
                } else {
                    startActivity(Intent(this, MainActivity17::class.java))
                }
            } else {
                val intent = Intent(this, MainActivity20::class.java).apply {
                    putExtra("VIEW_USER_ID", displayStory.user.uid)
                }
                startActivity(intent)
            }
        }
        storiesRecyclerView.adapter = storyAdapter

        // Feed RecyclerView
        feedRecyclerView = findViewById(R.id.recylerView)
        feedRecyclerView.layoutManager = LinearLayoutManager(this)
        feedAdapter = FeedAdapter(feedPostsList)
        feedRecyclerView.adapter = feedAdapter

        // Pagination scroll listener
        feedRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount 
                    && firstVisibleItemPosition >= 0) {
                    // Load next page
                    currentPage++
                    loadFeed(false)
                }
            }
        })
    }

    private fun loadMyProfilePicture() {
        val currentUserId = prefs.getUserId() ?: return
        
        lifecycleScope.launch {
            try {
                // TODO: Get user from Room cache
                // For now, use placeholder
                val profilePic = prefs.getAuthToken() // This is just placeholder
                bottomNavProfileImage.setImageResource(R.drawable.person2)
                
                // When UserRepository is created, use:
                // val user = userRepository.getUserById(currentUserId)
                // user?.profilePictureBase64?.let { base64 ->
                //     val bitmap = ImageUtils.base64ToBitmap(base64)
                //     bottomNavProfileImage.setImageBitmap(bitmap)
                // }
            } catch (e: Exception) {
                Log.e("MainActivity5", "Failed to load profile picture: ${e.message}")
                bottomNavProfileImage.setImageResource(R.drawable.person2)
            }
        }
    }

    /**
     * Load stories from API (with offline cache)
     */
    private fun loadStories() {
        val currentUserId = prefs.getUserId() ?: return
        
        lifecycleScope.launch {
            try {
                // First, show cached stories immediately
                storyRepository.getActiveStoriesFlow().collect { cachedStories ->
                    updateStoriesUI(cachedStories, currentUserId)
                }

                // Then fetch from API
                val result = storyRepository.fetchActiveStories()
                
                result.onSuccess { stories ->
                    Log.d("MainActivity5", "Loaded ${stories.size} stories from API")
                    updateStoriesUI(stories, currentUserId)
                }
                
                result.onFailure { error ->
                    // Already showing cached data, just log error
                    Log.e("MainActivity5", "Failed to fetch stories: ${error.message}")
                    if (!NetworkUtils.isNetworkAvailable(this@MainActivity5)) {
                        Toast.makeText(
                            this@MainActivity5, 
                            "Showing cached stories (offline)", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity5", "Error loading stories: ${e.message}", e)
            }
        }
    }

    /**
     * Update stories UI with API data
     */
    private fun updateStoriesUI(stories: List<StoryEntity>, currentUserId: String) {
        val finalDisplayStories = mutableListOf<DisplayStory>()
        
        // Your story placeholder
        val ownStory = stories.find { it.userId == currentUserId }
        val yourStoryPlaceholder = DisplayStory(
            story = Story(
                id = "YOUR_STORY_PLACEHOLDER",
                userId = currentUserId,
                imageUrl = ownStory?.mediaUrl ?: ownStory?.mediaBase64 ?: ""
            ),
            user = User(uid = currentUserId, name = "Your Story")
        )
        finalDisplayStories.add(yourStoryPlaceholder)
        
        // Other users' stories
        val otherStories = stories
            .filter { it.userId != currentUserId }
            .sortedByDescending { it.timestamp }
            .map { storyEntity ->
                DisplayStory(
                    story = Story(
                        id = storyEntity.id,
                        userId = storyEntity.userId,
                        imageUrl = storyEntity.mediaUrl ?: storyEntity.mediaBase64 ?: "",
                        timestamp = storyEntity.timestamp
                    ),
                    user = User(uid = storyEntity.userId, name = "User") // TODO: Get user name from UserRepository
                )
            }
        
        finalDisplayStories.addAll(otherStories)
        storyAdapter.updateDisplayStories(finalDisplayStories)
    }

    /**
     * Load feed posts from API (with pagination)
     */
    private fun loadFeed(clearExisting: Boolean = true) {
        if (isLoading) return
        
        isLoading = true
        // loadingProgressBar?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                if (clearExisting) {
                    currentPage = 1
                }

                val result = postRepository.getFeed(page = currentPage, limit = 20)
                
                result.onSuccess { posts ->
                    if (clearExisting) {
                        feedPostsList.clear()
                    }
                    
                    // Convert PostEntity to Post (legacy model for adapter)
                    val convertedPosts = posts.map { postEntity ->
                        Post(
                            postId = postEntity.postId,
                            userId = postEntity.userId,
                            base64Image = postEntity.mediaBase64 ?: "",
                            imageUrl = postEntity.mediaUrl,
                            caption = postEntity.caption ?: "",
                            timestamp = postEntity.timestamp,
                            likesCount = postEntity.likesCount,
                            commentsCount = postEntity.commentsCount
                        )
                    }
                    
                    if (convertedPosts.isEmpty() && feedPostsList.isEmpty()) {
                        displayDefaultPost()
                    } else {
                        feedPostsList.addAll(convertedPosts)
                        feedAdapter.notifyDataSetChanged()
                    }
                    
                    Log.d("MainActivity5", "Loaded ${posts.size} posts from page $currentPage")
                }
                
                result.onFailure { error ->
                    Log.e("MainActivity5", "Failed to load feed: ${error.message}")
                    
                    if (feedPostsList.isEmpty()) {
                        if (!NetworkUtils.isNetworkAvailable(this@MainActivity5)) {
                            Toast.makeText(
                                this@MainActivity5,
                                "No internet - showing cached posts",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Load from cache via Flow
                            loadCachedFeed()
                        } else {
                            displayDefaultPost()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity5", "Error loading feed: ${e.message}", e)
                if (feedPostsList.isEmpty()) {
                    displayDefaultPost()
                }
            } finally {
                isLoading = false
                // loadingProgressBar?.visibility = View.GONE
                // swipeRefreshLayout?.isRefreshing = false
            }
        }
    }

    /**
     * Load cached posts from Room when offline
     */
    private fun loadCachedFeed() {
        lifecycleScope.launch {
            try {
                postRepository.getAllPostsFlow().collect { cachedPosts ->
                    feedPostsList.clear()
                    
                    val convertedPosts = cachedPosts.map { postEntity ->
                        Post(
                            postId = postEntity.postId,
                            userId = postEntity.userId,
                            base64Image = postEntity.mediaBase64 ?: "",
                            imageUrl = postEntity.mediaUrl,
                            caption = postEntity.caption ?: "",
                            timestamp = postEntity.timestamp,
                            likesCount = postEntity.likesCount,
                            commentsCount = postEntity.commentsCount
                        )
                    }
                    
                    if (convertedPosts.isEmpty()) {
                        displayDefaultPost()
                    } else {
                        feedPostsList.addAll(convertedPosts.sortedByDescending { it.timestamp })
                        feedAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity5", "Error loading cached feed: ${e.message}", e)
            }
        }
    }

    /**
     * Refresh all data (pull-to-refresh)
     */
    private fun refreshData() {
        loadStories()
        loadFeed(clearExisting = true)
    }

    private fun displayDefaultPost() {
        feedPostsList.clear()
        val defaultPost = Post(
            postId = "default_placeholder",
            userId = "admin_socially",
            base64Image = "",
            caption = "Welcome to Socially! Follow friends to populate your feed, or create your first post!",
            timestamp = System.currentTimeMillis()
        )
        feedPostsList.add(defaultPost)
        feedAdapter.notifyDataSetChanged()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, com.example.firstapp.ui.auth.LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        refreshData()
        
        // Cleanup expired stories
        lifecycleScope.launch {
            storyRepository.cleanupExpiredStories()
        }

        // Start screenshot monitoring
        if (screenshotMonitor == null) {
            screenshotMonitor = com.example.firstapp.utils.ScreenshotMonitor(this)
        }
        screenshotMonitor?.start()
    }

    override fun onPause() {
        super.onPause()
        // Stop screenshot monitoring to avoid leaks
        screenshotMonitor?.stop()
    }
}
