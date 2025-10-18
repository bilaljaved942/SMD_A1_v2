package com.example.firstapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CommentsActivity : AppCompatActivity() {

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentEditText: EditText
    private lateinit var postCommentButton: Button
    private lateinit var commentsAdapter: CommentsAdapter
    private val commentsList = mutableListOf<Comment>()

    // Key identifier for the post whose comments we are viewing
    private lateinit var postId: String
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        // --- CRITICAL CHECK: Retrieve the unique POST_ID ---
        postId = intent.getStringExtra("POST_ID") ?: run {
            // Log the error and notify the user if the POST_ID is missing
            Log.e("CommentsActivity", "FATAL: Post ID is missing from Intent extras.")
            Toast.makeText(this, "Error: Cannot load comments (Missing Post ID).", Toast.LENGTH_LONG).show()
            finish() // Close the activity since we can't load comments
            return
        }

        Log.d("CommentsActivity", "Loading comments for Post ID: $postId")

        // Initialize Views
        commentsRecyclerView = findViewById(R.id.comments_recycler_view)
        commentEditText = findViewById(R.id.comment_edit_text)
        postCommentButton = findViewById(R.id.post_comment_button)

        // Setup RecyclerView
        commentsAdapter = CommentsAdapter(commentsList)
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentsRecyclerView.adapter = commentsAdapter

        // Load existing comments
        loadComments()

        // Setup post button click listener
        postCommentButton.setOnClickListener {
            postComment()
        }
    }

    /**
     * Loads comments from the unique Firebase path: "comments/{postId}"
     */
    private fun loadComments() {
        // Correct path: Ensures comments are scoped to the specific post ID
        val commentsRef = FirebaseDatabase.getInstance().getReference("comments").child(postId)

        commentsRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentsList.clear()
                for (commentSnapshot in snapshot.children) {
                    val comment = commentSnapshot.getValue(Comment::class.java)
                    comment?.let { commentsList.add(it) }
                }
                commentsAdapter.notifyDataSetChanged()

                if (commentsList.isNotEmpty()) {
                    commentsRecyclerView.scrollToPosition(commentsList.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CommentsActivity", "Failed to load comments: ${error.message}")
                Toast.makeText(this@CommentsActivity, "Failed to load comments: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Posts a new comment to the unique Firebase path: "comments/{postId}"
     */
    private fun postComment() {
        val commentText = commentEditText.text.toString().trim()
        if (commentText.isBlank() || currentUserId == null) {
            Toast.makeText(this, "Login and write a comment.", Toast.LENGTH_SHORT).show()
            return
        }

        val commentsRef = FirebaseDatabase.getInstance().getReference("comments").child(postId)
        val commentKey = commentsRef.push().key ?: return

        val newComment = Comment(
            commentId = commentKey,
            userId = currentUserId,
            text = commentText,
            timestamp = System.currentTimeMillis()
        )

        commentsRef.child(commentKey).setValue(newComment)
            .addOnSuccessListener {
                commentEditText.setText("")
                // No need for Toast, real-time listener will handle update
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to post comment: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
