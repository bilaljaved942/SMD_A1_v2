package com.example.firstapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import Post
// NOTE: Assuming Post and User data classes are defined elsewhere
// NOTE: Assuming CommentsActivity is implemented as planned

class FeedAdapter(private val postsList: List<Post>) :
    RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    // Get current authenticated user's ID
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // --- ViewHolder Definition ---
    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Post Header Views
        val postProfileImage: CircleImageView = itemView.findViewById(R.id.post_profile_image)
        val postUsername: TextView = itemView.findViewById(R.id.post_username)

        // Post Content Views
        val postImageView: ImageView = itemView.findViewById(R.id.post_image_view)
        val captionTextView: TextView = itemView.findViewById(R.id.caption_full_text)

        // Like/Comment/Count Views
        val likeButton: ImageView = itemView.findViewById(R.id.like_button)
        val likesCountTextView: TextView = itemView.findViewById(R.id.likes_display_text)

        // NEW: Comment Button and Count Views (Assuming IDs based on standard naming)
        val commentButton: ImageView = itemView.findViewById(R.id.comment_button)
        val commentCountTextView: TextView = itemView.findViewById(R.id.comments_display_text)


        fun bind(post: Post) {

            // --- 1. Display Post Image and Caption ---
            if (post.base64Image.isNotBlank()) {
                try {
                    val imageBytes = Base64.decode(post.base64Image, Base64.NO_WRAP)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    postImageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Log.e("FeedAdapter", "Error decoding post image: ${e.message}")
                    postImageView.setImageResource(R.drawable.japan)
                }
            } else {
                postImageView.setImageResource(R.drawable.japan)
            }
            captionTextView.text = "${post.caption}"

            // --- 2. Fetch Post Header Details ---
            fetchUserDetails(post.userId)

            // --- 3. LIKE FEATURE INTEGRATION ---
            if (currentUserId != null) {
                listenForLikes(post.postId)
                likeButton.setOnClickListener {
                    toggleLike(post.postId, currentUserId)
                }
            } else {
                likesCountTextView.text = "0 likes (Login to like)"
                likeButton.setOnClickListener(null)
            }

            // --- 4. COMMENTS FEATURE INTEGRATION (NEW) ---
            listenForCommentCount(post.postId)

            commentButton.setOnClickListener {
                // Launch the CommentsActivity, passing the postId
                val intent = Intent(itemView.context, CommentsActivity::class.java).apply {
                    putExtra("POST_ID", post.postId)
                }
                itemView.context.startActivity(intent)
            }
        }

        /**
         * Attaches a ValueEventListener to the specific post's likes node.
         * 1. Updates the total likes count in real-time.
         * 2. Checks if the current user has liked the post to update the icon color.
         */
        private fun listenForLikes(postId: String) {
            val likesRef = FirebaseDatabase.getInstance().getReference("likes").child(postId)

            likesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val likeCount = snapshot.childrenCount
                    likesCountTextView.text = "$likeCount likes"

                    // Check if current user's UID exists under this post's likes node
                    val isLiked = currentUserId != null && snapshot.hasChild(currentUserId)

                    // Update the heart icon color
                    if (isLiked) {
                        // Set to red/filled icon
                        likeButton.setImageResource(R.drawable.img5)
                        likeButton.setColorFilter(itemView.context.resources.getColor(R.color.Red))
                    } else {
                        // Set to gray/unfilled icon
                        likeButton.setImageResource(R.drawable.img5)
                        likeButton.setColorFilter(itemView.context.resources.getColor(R.color.LightGray))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FeedAdapter", "Failed to read likes: ${error.message}")
                }
            })
        }

        /**
         * Toggles the like status (like/unlike) for the current user on the given post.
         */
        private fun toggleLike(postId: String, userId: String) {
            val postLikesRef = FirebaseDatabase.getInstance().getReference("likes").child(postId).child(userId)

            postLikesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // User has already liked it -> UNLIKE (decrement)
                        snapshot.ref.removeValue()
                        Log.d("LikeToggle", "User $userId unliked post $postId")
                    } else {
                        // User has not liked it -> LIKE (increment)
                        snapshot.ref.setValue(true)
                        Log.d("LikeToggle", "User $userId liked post $postId")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LikeToggle", "Transaction failed: ${error.message}")
                }
            })
        }

        /**
         * Attaches a ValueEventListener to the specific post's comments node to display count.
         */
        private fun listenForCommentCount(postId: String) {
            val commentsRef = FirebaseDatabase.getInstance().getReference("comments").child(postId)

            commentsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val commentCount = snapshot.childrenCount
                    // Display count in a friendly way (like Instagram's 'View all X comments')
                    if (commentCount > 0) {
                        commentCountTextView.visibility = View.VISIBLE
                        commentCountTextView.text = "View all $commentCount comments"
                    } else {
                        commentCountTextView.visibility = View.GONE
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FeedAdapter", "Failed to read comments count: ${error.message}")
                    commentCountTextView.text = "View comments"
                }
            })
        }

        // --- fetchUserDetails ---
        private fun fetchUserDetails(userId: String) {
            postUsername.text = "Loading..."
            postProfileImage.setImageResource(R.drawable.person)

            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)

                    if (user == null) {
                        postUsername.text = "User Missing"
                        postProfileImage.setImageResource(R.drawable.person)
                        return
                    }

                    val username = user.name.takeIf { it.isNotBlank() } ?: "Unknown User"
                    postUsername.text = username

                    val base64Pic = user.profilePictureBase64// Use the correct field name (profilePicture)

                    if (!base64Pic.isNullOrBlank()) {
                        try {
                            val imageBytes = Base64.decode(base64Pic!!, Base64.NO_WRAP)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            postProfileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("FeedAdapter", "Base64 Decoding FAILED: ${e.message}")
                            postProfileImage.setImageResource(R.drawable.person)
                        }
                    } else {
                        postProfileImage.setImageResource(R.drawable.person)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FeedAdapter", "Firebase read FAILED: ${error.message}")
                    postUsername.text = "Error"
                    postProfileImage.setImageResource(R.drawable.person)
                }
            })
        }
    }

    // --- Adapter Overrides (Unchanged) ---
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(postsList[position])
    }

    override fun getItemCount(): Int = postsList.size
}
