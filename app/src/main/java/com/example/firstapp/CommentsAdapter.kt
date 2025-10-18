package com.example.firstapp

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class CommentsAdapter(private val commentsList: List<Comment>) :
    RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    // --- ViewHolder Definition ---
    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userProfileImage: CircleImageView = itemView.findViewById(R.id.comment_user_profile_image) // ASSUME ID
        val usernameTextView: TextView = itemView.findViewById(R.id.comment_username_text) // ASSUME ID
        val commentTextView: TextView = itemView.findViewById(R.id.comment_text_view) // ASSUME ID

        fun bind(comment: Comment) {
            commentTextView.text = comment.text
            fetchUserDetails(comment.userId)
        }

        // Reusing/Adapting the user details fetching logic from FeedAdapter
        private fun fetchUserDetails(userId: String) {
            usernameTextView.text = "Loading..."
            userProfileImage.setImageResource(R.drawable.person) // Fallback image

            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)

                    if (user == null) {
                        usernameTextView.text = "User Missing"
                        userProfileImage.setImageResource(R.drawable.person)
                        return
                    }

                    val username = user.name.takeIf { it.isNotBlank() } ?: "Unknown User"
                    // Display username and comment side-by-side or format as desired
                    usernameTextView.text = username

                    val base64Pic = user.profilePictureBase64

                    if (!base64Pic.isNullOrBlank()) {
                        try {
                            val imageBytes = Base64.decode(base64Pic, Base64.NO_WRAP)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            userProfileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("CommentsAdapter", "Base64 Decoding FAILED: ${e.message}")
                            userProfileImage.setImageResource(R.drawable.person)
                        }
                    } else {
                        userProfileImage.setImageResource(R.drawable.person)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CommentsAdapter", "Firebase read FAILED: ${error.message}")
                    usernameTextView.text = "Error"
                    userProfileImage.setImageResource(R.drawable.person)
                }
            })
        }
    }

    // --- Adapter Overrides ---
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        // You'll need to create this layout (item_comment.xml)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(commentsList[position])
    }

    override fun getItemCount(): Int = commentsList.size
}