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
import com.google.firebase.database.FirebaseDatabase
import Post
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView // Ensure this import is present

// Assuming you have a Post data class and a PostsAdapter class
// import Post // Uncomment if Post is in a separate file
// import PostsAdapter // Uncomment if PostsAdapter is in a separate file

class MainActivity13 : AppCompatActivity() {

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postsAdapter: PostsAdapter
    private val postsList = mutableListOf<Post>()
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    // References
    private lateinit var profileNameTextView: TextView
    // CRITICAL: Profile image ID from your layout is 'profileImage'
    private lateinit var profileImageView: CircleImageView // Changed to CircleImageView

    // Launcher for handling the result when returning from MainActivity15 (Edit Profile)
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // This launcher handles the result from MainActivity15.
        // Since fetchUserNameAndPicture uses a real-time listener,
        // the profile picture will already be updated when this code runs.
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
        // ID for the name TextView in activity_main13.xml is 'text2_1'
        profileNameTextView = findViewById(R.id.text2_1)
        // ID for the profile image in activity_main13.xml is 'profileImage'
        profileImageView = findViewById(R.id.profileImage)
        val editProfileButton = findViewById<TextView>(R.id.text2_5)

        // --- Click Handlers ---

        // 1. Logout
        findViewById<ImageView>(R.id.image3).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity3::class.java))
            finish()
        }

        // 2. Navigation to Edit Profile (MainActivity15)
        editProfileButton.setOnClickListener {
            val intent = Intent(this, MainActivity15::class.java)
            editProfileLauncher.launch(intent) // Use launcher to expect a result
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

        fetchUserProfileAndPosts()

        findViewById<ImageView>(R.id.homeIcon).setOnClickListener {
            startActivity(Intent(this, MainActivity5::class.java))
        }
    }

    // --- Data Fetching Logic ---

    private fun fetchUserProfileAndPosts() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            profileNameTextView.text = "Guest User"
            Log.e("MainActivity13", "User not logged in.")
            postsList.clear()
            postsAdapter.notifyDataSetChanged()
            return
        }

        // Fetches Name and Picture
        fetchUserNameAndPicture(currentUserId)
        fetchUserPosts(currentUserId)
    }

    /**
     * Fetches the user's name and profile picture Base64 string in real-time
     * and displays it in the CircleImageView (profileImage).
     */
    private fun fetchUserNameAndPicture(userId: String) {
        val userRef = database.getReference("users").child(userId)

        // Use addValueEventListener for real-time updates:
        // When MainActivity15 updates the 'profilePicture' value,
        // this listener fires and updates the image instantly.
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Fetch Name (kept for completeness)
                val name = snapshot.child("name").getValue(String::class.java)
                profileNameTextView.text = name ?: (auth.currentUser?.email ?: "User Profile")

                // Fetch Picture Base64 String
                val base64Pic = snapshot.child("profilePicture").getValue(String::class.java)

                if (base64Pic != null && base64Pic.isNotBlank()) {
                    try {
                        // Decode the Base64 string back into a byte array
                        val imageBytes = Base64.decode(base64Pic, Base64.NO_WRAP)
                        // Create a Bitmap from the byte array
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        // Set the Bitmap to the ImageView
                        profileImageView.setImageBitmap(bitmap)
                    } catch (e: IllegalArgumentException) {
                        Log.e("Profile", "Invalid Base64 for profile picture: ${e.message}")
                        // Fallback to default image
                        profileImageView.setImageResource(R.drawable.person)
                    }
                } else {
                    // Default image if no picture is set
                    profileImageView.setImageResource(R.drawable.person)
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
                    // Make sure your Post data class matches the Firebase structure
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