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
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
// Assuming Story, User, and DisplayStory are imported from DataModels.kt or defined elsewhere in the package

class StoryAdapter(
    private var displayStories: MutableList<DisplayStory>,
    private val clickListener: (Story) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: CircleImageView = view.findViewById(R.id.story_profile_image)
        val username: TextView = view.findViewById(R.id.story_username)
        val addIcon: ImageView = view.findViewById(R.id.add_story_icon)
        val ringContainer: View = view.findViewById(R.id.story_ring_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val displayItem = displayStories[position]
        val story = displayItem.story
        val user = displayItem.user

        // Determine if an active story exists
        val hasActiveStory = story.imageUrl.isNotEmpty() && story.imageUrl.length > 100

        // ⭐ SET USERNAME: Use the 'name' property from the User object
        holder.username.text = user.name

        // ⭐ SET PROFILE PICTURE: Use the 'profilePictureBase64' property
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
            // Default image if no profile picture is set
            holder.profileImage.setImageResource(R.drawable.person)
        }

        // --- LOGIC FOR "YOUR STORY" ---
        if (story.id == "YOUR_STORY_PLACEHOLDER") {
            holder.addIcon.visibility = View.VISIBLE
        } else {
            // --- LOGIC FOR ALL OTHER USERS' STORIES ---
            holder.addIcon.visibility = View.GONE
        }

        // Story Ring Logic
        if (hasActiveStory) {
            holder.ringContainer.setBackgroundResource(R.drawable.create_grad)
        } else {
            holder.ringContainer.background = null
        }


        holder.itemView.setOnClickListener {
            // Pass the original Story object to the click listener for navigation
            clickListener(story)
        }
    }

    override fun getItemCount(): Int = displayStories.size

    // Updates the adapter with the combined data
    fun updateDisplayStories(newDisplayStories: List<DisplayStory>) {
        displayStories.clear()
        displayStories.addAll(newDisplayStories)
        notifyDataSetChanged()
    }
}