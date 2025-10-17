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

// *** FIX: ADD EXPLICIT IMPORTS TO RESOLVE REFERENCES ***
// These imports are crucial for MainActivity5 to recognize the classes defined in other files
import com.example.firstapp.Story
import com.example.firstapp.StoryAdapter
// *** END FIX ***


// Placeholder constant for the Post feature (included for completeness)
const val STATIC_POST_ID = "JOSHUA_I_TOKYO_POST"

class MainActivity5 : AppCompatActivity() {

    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    // Reference for the bottom nav profile image
    private lateinit var bottomNavProfileImage: CircleImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main5)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        var forward=findViewById<ImageView>(R.id.forward)
        forward.setOnClickListener {
            startActivity(Intent(this, MainActivity8::class.java))
        }

        var user=findViewById<ImageView>(R.id.batteryIcon)
        user.setOnClickListener {
            startActivity(Intent(this, MainActivity22::class.java))
        }
        
        var gallery=findViewById<ImageView>(R.id.homeIcon3)
        gallery.setOnClickListener {
            startActivity(Intent(this, MainActivity16::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize bottom navigation profile image
        bottomNavProfileImage = findViewById(R.id.profileImage3)

        // Load the current user's profile image for ALL spots
        loadMyProfilePicture()

        // --- Navigation Links ---
        var profileScreen = findViewById<ImageView>(R.id.profileImage3)
        profileScreen.setOnClickListener {
            val intent = Intent(this, MainActivity13::class.java)
            startActivity(intent)
        }
        // ... (other navigation links remain here) ...

        // --- STORIES FEATURE INTEGRATION ---
        storiesRecyclerView = findViewById(R.id.stories_recycler_view)
        storiesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val initialStoryList = mutableListOf<Story>()
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            initialStoryList.add(Story(id = "YOUR_STORY_PLACEHOLDER", userId = currentUserId))
        }

        // This line now successfully references the imported StoryAdapter
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

        loadStoriesFromFirebase()
    }

    /**
     * Fetches the user's profile picture in real-time and updates:
     * 1. The StoryAdapter (for the "Your Story" item).
     * 2. The bottom navigation bar profile picture (profileImage3).
     */
    private fun loadMyProfilePicture() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Listen for changes to the user's profile data
        database.getReference("users").child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val base64Pic = snapshot.child("profilePicture").getValue(String::class.java)

                    if (base64Pic != null && base64Pic.isNotBlank()) {
                        try {
                            val imageBytes = Base64.decode(base64Pic, Base64.NO_WRAP)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                            // 1. Update the StoryAdapter (the "Your Story" item)
                            storyAdapter.updateProfilePicture(base64Pic)

                            // 2. Update the Bottom Navigation Profile Image
                            bottomNavProfileImage.setImageBitmap(bitmap)

                        } catch (e: IllegalArgumentException) {
                            Log.e("MainActivity5", "Invalid Base64 for profile picture: ${e.message}")
                            storyAdapter.updateProfilePicture(null)
                            bottomNavProfileImage.setImageResource(R.drawable.person2) // Default
                        }
                    } else {
                        // Handle case where no picture is set
                        storyAdapter.updateProfilePicture(null)
                        bottomNavProfileImage.setImageResource(R.drawable.person2) // Default
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
                    // This line now successfully references the imported StoryAdapter
                    storyAdapter.updateStories(finalStoriesList)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
}
