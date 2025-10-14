package com.example.firstapp

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView

// NOTE: Story data class must be defined in its own file or accessible here.
data class Story(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val viewed: Boolean = false,
    val isVideo: Boolean = false
)

class StoryAdapter(
    private var stories: MutableList<Story>,
    private val clickListener: (Story) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

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
        val story = stories[position]

        // Determine if an active story exists for this user
        val hasActiveStory = story.imageUrl.isNotEmpty() && story.imageUrl.length > 100

        // --- LOGIC FOR "YOUR STORY" ---
        if (story.id == "YOUR_STORY_PLACEHOLDER") {
            holder.username.text = "Your Story"
            holder.addIcon.visibility = View.VISIBLE

            if (hasActiveStory) {
                // SCENARIO 1: STORY IS ACTIVE
                holder.ringContainer.setBackgroundResource(R.drawable.create_grad)

                // FIX: ALWAYS show the DP in the small circle, even when a story is active.
                holder.profileImage.setImageResource(R.drawable.person)
            } else {
                // SCENARIO 2: NO STORY UPLOADED YET
                holder.ringContainer.background = null
                holder.profileImage.setImageResource(R.drawable.person)
            }

        } else {
            // --- LOGIC FOR ALL OTHER USERS' STORIES ---

            holder.username.text = story.userId
            holder.addIcon.visibility = View.GONE

            if (hasActiveStory) {
                // SCENARIO 3: FRIEND'S STORY IS ACTIVE
                holder.ringContainer.setBackgroundResource(R.drawable.create_grad)

                // FIX: ALWAYS show the DP in the small circle for friends as well.
                holder.profileImage.setImageResource(R.drawable.person)
            } else {
                // SCENARIO 4: FRIEND HAS NO ACTIVE STORY
                holder.ringContainer.background = null
                holder.profileImage.setImageResource(R.drawable.person)
            }
        }

        holder.itemView.setOnClickListener {
            clickListener(story)
        }
    }

    override fun getItemCount(): Int = stories.size

    // ... (updateStories function remains the same) ...
    fun updateStories(newStories: List<Story>) {
        val uniqueStoriesMap = mutableMapOf<String, Story>()
        var userOwnStory: Story? = null

        for (story in newStories) {
            if (story.id == "YOUR_STORY_PLACEHOLDER") {
                uniqueStoriesMap["YOUR_STORY_PLACEHOLDER"] = story
                continue
            }

            if (story.userId == currentUserId) {
                if (userOwnStory == null || story.timestamp > userOwnStory.timestamp) {
                    userOwnStory = story
                }
                continue
            }

            val existingStory = uniqueStoriesMap[story.userId]
            if (existingStory == null || story.timestamp > existingStory.timestamp) {
                uniqueStoriesMap[story.userId] = story
            }
        }

        stories.clear()

        val placeholder = uniqueStoriesMap["YOUR_STORY_PLACEHOLDER"]
        if (placeholder != null) {
            if (userOwnStory != null) {
                // MERGE: Copy the story data into the placeholder for the conditional check in MA5.kt
                val mergedStory = placeholder.copy(
                    userId = userOwnStory.userId,
                    imageUrl = userOwnStory.imageUrl
                )
                stories.add(mergedStory)
            } else {
                stories.add(placeholder)
            }
        }

        val sortedOtherUsersStories = uniqueStoriesMap.values
            .filter { it.id != "YOUR_STORY_PLACEHOLDER" }
            .sortedByDescending { it.timestamp }

        stories.addAll(sortedOtherUsersStories)

        notifyDataSetChanged()
    }
}