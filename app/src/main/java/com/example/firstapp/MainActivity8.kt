package com.example.firstapp

import android.content.Intent
import android.os.Bundle
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
import com.example.firstapp.repository.FollowRepository
import com.example.firstapp.repository.UserRepository
import com.example.firstapp.utils.SecurePreferences
import com.example.firstapp.utils.NetworkUtils
import kotlinx.coroutines.launch

/**
 * MainActivity8 - Chat List Activity (REFACTORED FOR REST APIs)
 * 
 * Shows list of mutual followers (users who follow you and you follow them)
 * Click on a user to open chat conversation
 */
class MainActivity8 : AppCompatActivity() {

    // Repositories
    private lateinit var followRepository: FollowRepository
    private lateinit var userRepository: UserRepository
    private lateinit var prefs: SecurePreferences

    // UI Components
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatListAdapter
    private lateinit var loadingProgressBar: ProgressBar
    
    private val mutualFollowList = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main8)

        // Initialize repositories
        followRepository = FollowRepository(this)
        userRepository = UserRepository(this)
        prefs = SecurePreferences(this)

        // Check if logged in
        if (!prefs.isLoggedIn()) {
            finish()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        loadMutualFollowers()
    }

    private fun setupViews() {
        // Back button
        findViewById<ImageView>(R.id.image1)?.setOnClickListener {
            finish()
        }

        // Loading indicator (add to layout if doesn't exist)
        // loadingProgressBar = findViewById(R.id.loadingProgressBar)

        // Chat list RecyclerView
        chatRecyclerView = findViewById(R.id.chat_list_recycler_view)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        chatAdapter = ChatListAdapter(mutualFollowList, ::onChatClicked)
        chatRecyclerView.adapter = chatAdapter
    }

    /**
     * Load mutual followers (users who follow you and you follow back)
     */
    private fun loadMutualFollowers() {
        // Show loading
        // loadingProgressBar?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = followRepository.getMutualFollowers()
                
                result.onSuccess { users ->
                    mutualFollowList.clear()
                    
                    // Convert UserEntity to User (legacy model for adapter)
                    val convertedUsers = users.map { userEntity ->
                        User(
                            uid = userEntity.uid.toString(),
                            name = userEntity.name,
                            email = userEntity.email,
                            profilePicture = userEntity.profilePictureBase64,
                            coverPhoto = userEntity.coverPhotoBase64,
                            bio = userEntity.bio,
                            online = userEntity.online,
                            lastOnline = userEntity.lastOnline
                        )
                    }.sortedBy { it.name }
                    
                    mutualFollowList.addAll(convertedUsers)
                    chatAdapter.notifyDataSetChanged()
                    
                    Log.d("MainActivity8", "Loaded ${users.size} mutual followers")
                    
                    if (users.isEmpty()) {
                        Toast.makeText(
                            this@MainActivity8,
                            "No mutual followers yet. Follow someone back!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                
                result.onFailure { error ->
                    Log.e("MainActivity8", "Failed to load mutual followers: ${error.message}")
                    
                    if (!NetworkUtils.isNetworkAvailable(this@MainActivity8)) {
                        Toast.makeText(
                            this@MainActivity8,
                            "No internet connection",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity8,
                            "Failed to load chat list: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity8", "Error loading mutual followers: ${e.message}", e)
                Toast.makeText(
                    this@MainActivity8,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Hide loading
                // loadingProgressBar?.visibility = View.GONE
            }
        }
    }

    /**
     * Handle chat item click
     */
    private fun onChatClicked(user: User) {
        val intent = Intent(this, MainActivity9::class.java).apply {
            putExtra("RECIPIENT_USER_ID", user.uid)
            putExtra("RECIPIENT_NAME", user.name)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh list when returning to this activity
        loadMutualFollowers()
    }
}
