package com.example.firstapp

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import User
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
// User data class definition is assumed to be accessible

class ChatListAdapter(
    private val userList: MutableList<User>,
    private val onChatClickListener: (User) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    // Helper function to decode and set the image (Reused from MainActivity5 logic)
    private fun loadProfilePicture(imageView: CircleImageView, base64: String?) {
        if (!base64.isNullOrBlank()) {
            try {
                val imageBytes = Base64.decode(base64, Base64.NO_WRAP)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: IllegalArgumentException) {
                Log.e("ChatAdapter", "Failed to decode Base64 image for profile.")
                imageView.setImageResource(R.drawable.person)
            }
        } else {
            // Set a default picture if no Base64 data is present
            imageView.setImageResource(R.drawable.person)
        }
    }

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // CRITICAL FIX: Ensure this is CircleImageView to match the XML
        val profileImage: CircleImageView = view.findViewById(R.id.chat_user_profile_image)
        val nameText: TextView = view.findViewById(R.id.chat_user_name)
        val lastMessage: TextView = view.findViewById(R.id.chat_last_message)
        val cameraIcon: ImageView = view.findViewById(R.id.chat_camera_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        // NOTE: Layout inflation relies on the corrected XML filename
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_user_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val user = userList[position]

        // --- 1. Load Profile Picture (NEW) ---
        // We use the profilePictureBase64 field that MainActivity8 fetched
        loadProfilePicture(holder.profileImage, user.profilePictureBase64)
        // -------------------------------------

        // Display Name
        holder.nameText.text = if (user.name.isNotBlank()) user.name else user.uid.substring(0, 8) + "..."

        // Placeholder for last message
        holder.lastMessage.text = "Start chatting!"

        // Handle opening the chat window when the row is clicked
        holder.itemView.setOnClickListener {
            onChatClickListener(user)
        }
    }

    override fun getItemCount(): Int = userList.size
}
