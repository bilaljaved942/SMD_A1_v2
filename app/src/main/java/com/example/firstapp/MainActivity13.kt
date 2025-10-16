package com.example.firstapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import Post
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity13 : AppCompatActivity() {

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postsAdapter: PostsAdapter
    private val postsList = mutableListOf<Post>()
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main13)

        database = FirebaseDatabase.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        postsRecyclerView = findViewById(R.id.posts_recycler_view)
        postsRecyclerView.layoutManager = GridLayoutManager(this, 3)

        postsAdapter = PostsAdapter(postsList)
        postsRecyclerView.adapter = postsAdapter

        fetchPostsFromFirebase()

        // ... (Your navigation logic remains here)
    }

    private fun fetchPostsFromFirebase() {
        val postsRef = database.getReference("images")

        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()
                for (postSnapshot in snapshot.children) {
                    // This now successfully maps data with "base64Image" and "timestamp"
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        postsList.add(it)
                    }
                }

                postsList.reverse()
                postsAdapter.notifyDataSetChanged()

                Log.d("MainActivity13", "Fetched ${postsList.size} posts.")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity13", "Failed to read posts from Firebase.", error.toException())
            }
        })
    }
}