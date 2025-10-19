package com.example.firstapp

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class ChatListAdapter(
    private val userList: MutableList<User>,
    private val onChatClickListener: (User) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    // The ViewHolder is now corrected to look for 'chat_last_message'.
    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: CircleImageView = view.findViewById(R.id.chat_user_profile_image)
        val nameText: TextView = view.findViewById(R.id.chat_user_name)
        val lastMessage: TextView = view.findViewById(R.id.chat_last_message) // This is now correct
        val cameraIcon: ImageView = view.findViewById(R.id.chat_camera_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_user_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val user = userList[position]

        loadProfilePicture(holder.profileImage, user.profilePictureBase64)
        holder.nameText.text = if (user.name.isNotBlank()) user.name else user.uid.substring(0, 8) + "..."

        // --- ONLINE/OFFLINE LOGIC IS COMPLETELY REMOVED ---
        // Sets a static placeholder text for the last message.
        holder.lastMessage.text = "Start a conversation!"
        // --- END OF CHANGES ---

        holder.itemView.setOnClickListener {
            onChatClickListener(user)
        }
    }

    override fun getItemCount(): Int = userList.size

    private fun loadProfilePicture(imageView: CircleImageView, base64: String?) {
        if (!base64.isNullOrBlank()) {
            try {
                val imageBytes = Base64.decode(base64, Base64.NO_WRAP)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: IllegalArgumentException) {
                Log.e("ChatAdapter", "Failed to decode Base64 for profile.", e)
                imageView.setImageResource(R.drawable.person)
            }
        } else {
            imageView.setImageResource(R.drawable.person)
        }
    }
}