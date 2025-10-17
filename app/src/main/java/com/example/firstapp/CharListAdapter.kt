package com.example.firstapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import User

class ChatListAdapter(
    private val userList: MutableList<User>,
    private val onChatClickListener: (User) -> Unit // Handles opening the chat
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.chat_user_profile_image)
        val nameText: TextView = view.findViewById(R.id.chat_user_name)
        val lastMessage: TextView = view.findViewById(R.id.chat_last_message)
        val cameraIcon: ImageView = view.findViewById(R.id.chat_camera_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_user_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val user = userList[position]

        // Display Name
        holder.nameText.text = if (user.name.isNotBlank()) user.name else user.uid.substring(0, 8) + "..."
        // Placeholder for last message (In a full app, you'd fetch this from the /chats node)
        holder.lastMessage.text = "Start chatting!"

        // Handle opening the chat window when the row is clicked
        holder.itemView.setOnClickListener {
            onChatClickListener(user)
        }
    }

    override fun getItemCount(): Int = userList.size
}