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

class StoryAdapter(
    private var displayStories: MutableList<DisplayStory>,
    // --- FIX #1: The click listener now correctly expects a 'DisplayStory' object ---
    private val clickListener: (DisplayStory) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: CircleImageView = view.findViewById(R.id.story_profile_image)
        val username: TextView = view.findViewById(R.id.story_username)
        val addIcon: ImageView = view.findViewById(R.id.add_story_icon)
        val ringContainer: View = view.findViewById(R.id.story_ring_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        // This assumes you have a layout file at res/layout/item_story.xml
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val displayItem = displayStories[position]
        val story = displayItem.story
        val user = displayItem.user

        val hasActiveStory = story.imageUrl.isNotEmpty() && story.imageUrl.length > 100

        holder.username.text = user.name

        val base64Pic = user.profilePictureBase64
        if (!base64Pic.isNullOrBlank()) {
            try {
                val imageBytes = Base64.decode(base64Pic, Base64.NO_WRAP)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: IllegalArgumentException) {
                Log.e("StoryAdapter", "Invalid Base64 for profile picture for user ${user.uid}")
                holder.profileImage.setImageResource(R.drawable.person)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.person)
        }

        if (story.id == "YOUR_STORY_PLACEHOLDER") {
            holder.addIcon.visibility = View.VISIBLE
        } else {
            holder.addIcon.visibility = View.GONE
        }

        if (hasActiveStory) {
            holder.ringContainer.setBackgroundResource(R.drawable.create_grad)
        } else {
            holder.ringContainer.background = null
        }


        holder.itemView.setOnClickListener {
            // --- FIX #2: Pass the entire 'displayItem' object, not just the inner 'story' ---
            clickListener(displayItem)
        }
    }

    override fun getItemCount(): Int = displayStories.size

    fun updateDisplayStories(newDisplayStories: List<DisplayStory>) {
        displayStories.clear()
        displayStories.addAll(newDisplayStories)
        notifyDataSetChanged()
    }
}