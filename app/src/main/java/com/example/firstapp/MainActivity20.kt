package com.example.firstapp

import android.graphics.BitmapFactory
import android.util.Base64
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth

class MainActivity20 : AppCompatActivity() {

    private lateinit var storyImageView: ImageView
    private lateinit var closeButton: ImageView
    private lateinit var usernameTextView: TextView // text1 in XML
    private lateinit var timeTextView: TextView     // text2 in XML
    private lateinit var profileImageView: ImageView // image2 in XML - used for profile picture

    private var storiesList = mutableListOf<Story>()
    private var currentStoryIndex = 0
    private lateinit var targetUserId: String
    private var targetUser: User? = null

    // Story Duration (matches MainActivity5)
    private val handler = Handler(Looper.getMainLooper())
    private val storyDurationMs = 86400 * 1000L

    private val storyAdvancer = Runnable {
        showNextStory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main20)

        // Initialize UI elements using your XML IDs
        storyImageView = findViewById(R.id.story_full_image)
        closeButton = findViewById(R.id.story_close_button)
        usernameTextView = findViewById(R.id.text1)
        timeTextView = findViewById(R.id.text2)
        profileImageView = findViewById(R.id.image2) // ⭐ FIX: Using existing ImageView ID

        // ⭐ CORE FIX 1: Prioritize user ID from Intent ⭐
        val intentUserId = intent.getStringExtra("VIEW_USER_ID")
        val currentAuthId = FirebaseAuth.getInstance().currentUser?.uid

        if (intentUserId != null) {
            targetUserId = intentUserId
        } else if (currentAuthId != null) {
            targetUserId = currentAuthId
        } else {
            Toast.makeText(this, "Error: User ID is missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load the user's data first, then load their stories
        loadTargetUserProfile(targetUserId)

        closeButton.setOnClickListener {
            finish()
        }

        storyImageView.setOnClickListener {
            showNextStory()
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(storyAdvancer)
    }

    override fun onResume() {
        super.onResume()
        if (storiesList.isNotEmpty() && currentStoryIndex < storiesList.size) {
            handler.postDelayed(storyAdvancer, storyDurationMs)
        }
    }

    // ----------------------------------------------------------------------------------
    // NEW FUNCTION TO LOAD TARGET USER'S PROFILE DATA
    // ----------------------------------------------------------------------------------

    private fun loadTargetUserProfile(userId: String) {
        FirebaseDatabase.getInstance().getReference("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)?.copy(uid = userId)
                        ?: User(uid = userId, name = "Unknown User")
                    targetUser = user

                    updateHeaderUI()
                    loadTargetUserStories(userId)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity20", "Failed to load profile for $userId: ${error.message}")
                    targetUser = User(uid = userId, name = "User Missing")
                    updateHeaderUI()
                    loadTargetUserStories(userId)
                }
            })
    }

    // ----------------------------------------------------------------------------------
    // TARGETED STORY LOADING
    // ----------------------------------------------------------------------------------

    private fun loadTargetUserStories(userId: String) {
        val currentTime = System.currentTimeMillis()
        val expirationTimeMs = 86400 * 1000L
        val expirationThreshold = currentTime - expirationTimeMs

        FirebaseDatabase.getInstance().getReference("stories")
            .orderByChild("userId")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    storiesList.clear()
                    for (storySnapshot in snapshot.children) {
                        val story = storySnapshot.getValue(Story::class.java)
                        if (story != null && story.timestamp > expirationThreshold) {
                            storiesList.add(story)
                        }
                    }

                    if (storiesList.isNotEmpty()) {
                        storiesList.sortBy { it.timestamp }
                        showStory(0)
                    } else {
                        // This toast is the reason for the closure:
                        Toast.makeText(this@MainActivity20, "No active stories found for this user.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity20, "Failed to load stories: ${error.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun showStory(index: Int) {
        if (index < 0 || index >= storiesList.size) {
            finish()
            return
        }

        currentStoryIndex = index
        val story = storiesList[index]

        // Update Header (time is based on the story's timestamp)
        timeTextView.text = timeAgo(story.timestamp)

        // Base64 Decode and display image
        try {
            val base64String = story.imageUrl
            if (base64String.isNotEmpty() && base64String.length > 100) {
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                if (imageBytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    storyImageView.setImageBitmap(bitmap)
                } else {
                    storyImageView.setImageResource(R.drawable.person) // Default if Base64 is invalid
                }
            } else {
                storyImageView.setImageResource(R.drawable.person) // Default if no image URL
            }
        } catch (e: Exception) {
            storyImageView.setImageResource(R.drawable.person)
        }

        // Reset and start the 30-second timer
        handler.removeCallbacks(storyAdvancer)
        handler.postDelayed(storyAdvancer, storyDurationMs)
    }

    private fun showNextStory() {
        if (currentStoryIndex < storiesList.size - 1) {
            showStory(currentStoryIndex + 1)
        } else {
            finish()
        }
    }

    // ----------------------------------------------------------------------------------
    // HELPER FUNCTIONS FOR UI
    // ----------------------------------------------------------------------------------

    private fun updateHeaderUI() {
        val user = targetUser ?: return

        // Display Name: "Your Story" or friend's name
        val currentAuthId = FirebaseAuth.getInstance().currentUser?.uid
        val displayName = if (user.uid == currentAuthId) "Your Story" else user.name
        usernameTextView.text = displayName

        // Display Profile Picture: Using the ImageView ID 'image2'
        val base64Pic = user.profilePictureBase64
        if (!base64Pic.isNullOrBlank()) {
            try {
                val imageBytes = Base64.decode(base64Pic, Base64.NO_WRAP)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                profileImageView.setImageBitmap(bitmap)
            } catch (e: IllegalArgumentException) {
                profileImageView.setImageResource(R.drawable.person)
            }
        } else {
            // Revert to a default drawable if fetching fails or is missing
            profileImageView.setImageResource(R.drawable.person)
        }
    }

    private fun timeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            else -> "$days d ago"
        }
    }
}