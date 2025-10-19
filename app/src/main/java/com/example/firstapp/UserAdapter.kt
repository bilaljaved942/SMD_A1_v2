package com.example.firstapp

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

// User data class definition is assumed to be accessible

class UserAdapter(
    private val userList: MutableList<User>,
    private val currentUserId: String,
    private val onFollowClickListener: (User, Int) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    // Helper function updated to accept CircleImageView
    private fun loadProfilePicture(imageView: CircleImageView, base64: String?) {
        if (!base64.isNullOrBlank()) {
            try {
                val imageBytes = Base64.decode(base64, Base64.NO_WRAP)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                // CircleImageView handles cropping/scaling automatically
                imageView.setImageBitmap(bitmap)
            } catch (e: IllegalArgumentException) {
                Log.e("UserAdapter", "Failed to decode Base64 image for profile.")
                imageView.setImageResource(R.drawable.person)
            }
        } else {
            // Set a default picture if no Base64 data is present
            imageView.setImageResource(R.drawable.person)
        }
    }

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // CRITICAL FIX: Change type to CircleImageView to match the XML
        val profileImage: CircleImageView = view.findViewById(R.id.user_profile_image)
        val nameText: TextView = view.findViewById(R.id.user_name_text)
        val followButton: Button = view.findViewById(R.id.follow_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_follow, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        val context = holder.itemView.context

        // --- 1. Load Profile Picture ---
        // holder.profileImage is now a CircleImageView, fitting the helper function
        loadProfilePicture(holder.profileImage, user.profilePictureBase64)
        // -------------------------------------

        holder.itemView.visibility = View.VISIBLE
        holder.itemView.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        holder.nameText.text = if (user.name.isNotBlank()) user.name else user.uid.substring(0, 8) + "..."

        // Set button state
        if (user.isFollowing) {
            holder.followButton.text = "Following"
            holder.followButton.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            holder.followButton.setTextColor(ContextCompat.getColor(context, R.color.black))
        } else {
            holder.followButton.text = "Follow"
            holder.followButton.setBackgroundResource(R.drawable.red_rect)
            holder.followButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }

        holder.followButton.setOnClickListener {
            onFollowClickListener(user, position)
        }
    }

    override fun getItemCount(): Int = userList.size
}
