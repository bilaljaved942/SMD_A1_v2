package com.example.firstapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import User // Ensure User.kt is imported

class UserAdapter(
    private val userList: MutableList<User>,
    private val currentUserId: String,
    private val onFollowClickListener: (User, Int) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.user_profile_image)
        val nameText: TextView = view.findViewById(R.id.user_name_text)
        val followButton: Button = view.findViewById(R.id.follow_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // CRITICAL FIX: Inflate the single list item layout, NOT the entire activity layout.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_follow, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        val context = holder.itemView.context

        // FIX: Removed the redundant 'if (user.uid == currentUserId)' check.
        // The current user is now correctly filtered out in the Activity's fetchUsers() function.

        // Ensure the item is visible and correctly sized (needed since we removed the hiding block)
        holder.itemView.visibility = View.VISIBLE
        holder.itemView.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Display Name (Fallback to UID if name is missing)
        holder.nameText.text = if (user.name.isNotBlank()) user.name else user.uid.substring(0, 8) + "..."

        // Set button state based on isFollowing flag
        if (user.isFollowing) {
            holder.followButton.text = "Following"
            // We need custom drawables for 'red_rect' and 'red_border_rect' for proper Instagram style
            holder.followButton.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            holder.followButton.setTextColor(ContextCompat.getColor(context, R.color.black))
        } else {
            holder.followButton.text = "Follow"
            holder.followButton.setBackgroundResource(R.drawable.red_rect) // Assuming this is your filled button drawable
            holder.followButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }

        // Handle click
        holder.followButton.setOnClickListener {
            onFollowClickListener(user, position)
        }
    }

    override fun getItemCount(): Int = userList.size
}
