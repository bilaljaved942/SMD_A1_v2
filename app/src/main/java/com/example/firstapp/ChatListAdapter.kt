package com.example.firstapp

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class ChatListAdapter(
    private val userList: MutableList<User>,
    private val onChatClickListener: (User) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: CircleImageView = view.findViewById(R.id.chat_user_profile_image)
        val nameText: TextView = view.findViewById(R.id.chat_user_name)
        // UPDATED: Changed from lastMessage to statusText
        val statusText: TextView = view.findViewById(R.id.chat_user_status)
        val cameraIcon: ImageView = view.findViewById(R.id.chat_camera_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_user_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val user = userList[position]
        val context = holder.itemView.context

        loadProfilePicture(holder.profileImage, user.profilePictureBase64)
        holder.nameText.text = if (user.name.isNotBlank()) user.name else user.uid.substring(0, 8) + "..."

        // --- UPDATED PRESENCE LOGIC ---
        // Sets the text and color based on the 'online' field
        if (user.online) {
            holder.statusText.text = "Online"
            holder.statusText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
        } else {
            holder.statusText.text = "Offline"
            holder.statusText.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }
        // --- END OF UPDATED CODE ---

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
                Log.e("ChatAdapter", "Failed to decode Base64 image for profile.")
                imageView.setImageResource(R.drawable.person)
            }
        } else {
            imageView.setImageResource(R.drawable.person)
        }
    }
}