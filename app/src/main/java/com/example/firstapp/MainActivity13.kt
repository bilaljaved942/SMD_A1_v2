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

class MainActivity13 : AppCompatActivity() {

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postsAdapter: PostsAdapter
    private val postsList = mutableListOf<Post>()
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    private lateinit var profileNameTextView: TextView
    private lateinit var profileImageView: CircleImageView
    private lateinit var profileImage3: CircleImageView

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

        profileNameTextView = findViewById(R.id.text2_1)
        profileImageView = findViewById(R.id.profileImage)
        profileImage3 = findViewById(R.id.profileImage3)
        val editProfileButton = findViewById<TextView>(R.id.text2_5)
        postsCountTextView = findViewById(R.id.posts)
        followersCountTextView = findViewById(R.id.followers)
        followingCountTextView = findViewById(R.id.following)

        // --- THIS IS THE NEW, DIRECT LOGOUT LOGIC ---
        findViewById<ImageView>(R.id.image3).setOnClickListener {
            val currentUser = auth.currentUser
            val userId = currentUser?.uid

            // 1. Check if a user is actually logged in.
            if (userId == null) {
                // If not, just go to the login screen without any database operations.
                startActivity(Intent(this, MainActivity3::class.java))
                finish()
                return@setOnClickListener
            }

            // 2. Prepare the data to write to Firebase.
            val statusUpdate = mapOf<String, Any>(
                "online" to false,
                "lastOnline" to System.currentTimeMillis()
            )

            // 3. Get the database reference and MANUALLY write 'online: false'.
            val userStatusRef = database.getReference("users").child(userId)
            userStatusRef.updateChildren(statusUpdate).addOnCompleteListener { task ->
                // This 'addOnCompleteListener' ensures the next steps only run AFTER the database is updated.

                // 4. Now, sign the user out.
                auth.signOut()

                // 5. Finally, navigate to the login screen.
                val intent = Intent(this, MainActivity3::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
        // --- END OF NEW LOGIC ---

        editProfileButton.setOnClickListener {
            val intent = Intent(this, MainActivity15::class.java)
            editProfileLauncher.launch(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        postsRecyclerView = findViewById(R.id.posts_recycler_view)
        postsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        postsAdapter = PostsAdapter(postsList)
        postsRecyclerView.adapter = postsAdapter

        fetchUserProfileData()

        findViewById<ImageView>(R.id.homeIcon).setOnClickListener {
            startActivity(Intent(this, MainActivity5::class.java))
        }
    }

    //----------------------------------------------------------------------------------------------
    // Data Fetching Logic (All your existing code is preserved below)
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
                        profileImage3.setImageBitmap(bitmap)
                    } catch (e: IllegalArgumentException) {
                        Log.e("Profile", "Invalid Base64 for profile picture: ${e.message}")
                        profileImageView.setImageResource(R.drawable.person)
                        profileImage3.setImageResource(R.drawable.person)
                    }
                } else {
                    profileImageView.setImageResource(R.drawable.person)
                    profileImage3.setImageResource(R.drawable.person)
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
                    post?.let { postsList.add(it) }
                }
                postsList.reverse()
                postsAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity13", "Failed to read posts from Firebase.", error.toException())
            }
        })
    }

    private fun fetchFollowCounts(currentUserId: String) {
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
        val allFollowingRef = database.getReference("following")
        allFollowingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var followersCount = 0
                for (userSnapshot in snapshot.children) {
                    if (userSnapshot.hasChild(currentUserId)) {
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