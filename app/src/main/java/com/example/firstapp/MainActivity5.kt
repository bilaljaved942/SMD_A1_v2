package com.example.firstapp

import android.content.Intent
import android.os.Bundle
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
import com.example.firstapp.StoryAdapter

// Placeholder constant for the Post feature (included for completeness)
const val STATIC_POST_ID = "JOSHUA_I_TOKYO_POST"


class MainActivity5 : AppCompatActivity() {

    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... (Existing stack clearing code) ...

        enableEdgeToEdge()
        setContentView(R.layout.activity_main5)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var profileScreen=findViewById<ImageView>(R.id.profileImage3)
        profileScreen.setOnClickListener {
            val intent = Intent(this, MainActivity13::class.java)
            startActivity(intent)
        }

        var homeIcone=findViewById<ImageView>(R.id.homeIcon3)
        homeIcone.setOnClickListener {
            val intent = Intent(this, MainActivity16::class.java)
            startActivity(intent)
        }

        var homeIcone2=findViewById<ImageView>(R.id.homeIcon2)
        homeIcone2.setOnClickListener {
            val intent = Intent(this, MainActivity6::class.java)
            startActivity(intent)
        }

        var homeIcone3=findViewById<ImageView>(R.id.homeIcon4)
        homeIcone2.setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            startActivity(intent)
        }

        var message=findViewById<ImageView>(R.id.forward)
        message.setOnClickListener {
            val intent = Intent(this, MainActivity8::class.java)
            startActivity(intent)
        }

        var follower=findViewById<ImageView>(R.id.batteryIcon)
        follower.setOnClickListener {
            val intent = Intent(this, MainActivity22::class.java)
            startActivity(intent)
        }

        // --- STORIES FEATURE INTEGRATION (OPTIMIZED FOR IMMEDIATE DISPLAY) ---
        storiesRecyclerView = findViewById(R.id.stories_recycler_view)
        storiesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // 1. Initialize the list with the placeholder object immediately
        val initialStoryList = mutableListOf<Story>()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            initialStoryList.add(Story(id = "YOUR_STORY_PLACEHOLDER", userId = currentUserId))
        }

        // 2. Initialize the adapter with this small, immediate list
        storyAdapter = StoryAdapter(initialStoryList) { story ->

            if (story.id == "YOUR_STORY_PLACEHOLDER") {
                // Conditional Logic: Toggle between Viewer (20) and Camera (17)

                if (story.imageUrl.isNotEmpty() && story.imageUrl.length > 100) {
                    // If image exists, open YOUR dedicated story viewer (MainActivity20)
                    val intent = Intent(this, MainActivity20::class.java).apply {
                        putExtra("VIEW_USER_ID", story.userId)
                    }
                    startActivity(intent)

                } else {
                    // If no image, launch the UPLOAD CAMERA (MainActivity17)
                    startActivity(Intent(this, MainActivity17::class.java))
                }

            } else {
                // Friend's Story Click (Direct to general viewer MainActivity18)
                val intent = Intent(this, MainActivity18::class.java).apply {
                    putExtra("VIEW_USER_ID", story.userId)
                }
                startActivity(intent)
            }
        }
        storiesRecyclerView.adapter = storyAdapter // Placeholder is now visible instantly

        // 3. Start the heavy database call asynchronously
        loadStoriesFromFirebase()
        // --- END STORIES FEATURE INTEGRATION ---

        // ... (Existing navigation logic for bottom bar remains) ...
    }

    private fun loadStoriesFromFirebase() {
        val currentTime = System.currentTimeMillis()

        // Expiration time is 15 seconds for testing
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

                        // Apply expiration filter
                        if (story != null && story.timestamp > expirationThreshold) {
                            allStories.add(story)
                        }
                    }

                    // 1. Re-add the placeholder data (if it exists)
                    val finalStoriesList = mutableListOf<Story>()
                    if (currentUserId != null) {
                        finalStoriesList.add(Story(id = "YOUR_STORY_PLACEHOLDER", userId = currentUserId))
                    }

                    // 2. Add all fetched stories for the adapter to filter/deduplicate
                    finalStoriesList.addAll(allStories)

                    // This second call updates the placeholder's image and adds friends' stories
                    storyAdapter.updateStories(finalStoriesList)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
}