package com.example.firstapp

import Story
import android.graphics.BitmapFactory
import android.util.Base64
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.activity.enableEdgeToEdge

class MainActivity18 : AppCompatActivity() {

    private lateinit var storyImageView: ImageView
    private lateinit var closeButton: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var timeTextView: TextView

    private var storiesList = mutableListOf<Story>()
    private var currentStoryIndex = 0

    // Handler for the 15-second story duration (Splash Screen Logic)
    private val handler = Handler(Looper.getMainLooper())
    private val storyDurationMs = 15 * 1000L

    private val storyAdvancer = Runnable {
        showNextStory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main18)

        // Initialize UI elements
        storyImageView = findViewById(R.id.story_full_image)
        closeButton = findViewById(R.id.story_close_button)
        usernameTextView = findViewById(R.id.text1)
        timeTextView = findViewById(R.id.text2)

        val viewUserId = intent.getStringExtra("VIEW_USER_ID")

        if (viewUserId != null) {
            loadUserStories(viewUserId)
        } else {
            Toast.makeText(this, "Error: No user selected to view story.", Toast.LENGTH_SHORT).show()
            finish()
        }

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
        // If stories are loaded, restart the timer
        if (storiesList.isNotEmpty()) {
            handler.postDelayed(storyAdvancer, storyDurationMs)
        }
    }

    private fun loadUserStories(userId: String) {
        val currentTime = System.currentTimeMillis()
        val expirationTimeMs = 15 * 1000L
        val expirationThreshold = currentTime - expirationTimeMs

        // Fetch all stories for the specific userId
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
                        Toast.makeText(this@MainActivity18, "No active stories found.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity18, "Failed to load stories: ${error.message}", Toast.LENGTH_SHORT).show()
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

        // Update Header
        usernameTextView.text = story.userId
        timeTextView.text = "Just now"

        // Base64 Decode and display
        try {
            val base64String = story.imageUrl
            if (base64String.isNotEmpty() && base64String.length > 100) {
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                if (imageBytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    storyImageView.setImageBitmap(bitmap)
                } else {
                    storyImageView.setImageResource(R.drawable.person)
                }
            } else {
                storyImageView.setImageResource(R.drawable.person)
            }
        } catch (e: Exception) {
            storyImageView.setImageResource(R.drawable.person)
        }

        // Reset and start the 15-second timer for the current story
        handler.removeCallbacks(storyAdvancer)
        handler.postDelayed(storyAdvancer, storyDurationMs)
    }

    private fun showNextStory() {
        if (currentStoryIndex < storiesList.size - 1) {
            showStory(currentStoryIndex + 1)
        } else {
            // Reached the end of the user's stories
            finish()
        }
    }
}