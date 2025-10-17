package com.example.firstapp

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
// import Story // <-- REMOVED THIS AMBIGUOUS IMPORT

// *** FIX: Story data class is now DEFINED HERE for self-containment ***
data class Story(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val viewed: Boolean = false,
    val isVideo: Boolean = false
)
// *** END FIX ***

class StoryAdapter(
    private var stories: MutableList<Story>,
    private val clickListener: (Story) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // NEW FIELD: To hold the current user's Base64 profile picture
    private var currentUserProfilePicBase64: String? = null

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

        // Determine if an active story exists for this user (Story logic)
        val hasActiveStory = story.imageUrl.isNotEmpty() && story.imageUrl.length > 100

        // --- LOGIC FOR "YOUR STORY" ---
        if (story.id == "YOUR_STORY_PLACEHOLDER") {
            holder.username.text = "Your Story"
            holder.addIcon.visibility = View.VISIBLE

            // >>> PROFILE PICTURE LOGIC <<<
            if (!currentUserProfilePicBase64.isNullOrBlank()) {
                try {
                    val imageBytes = Base64.decode(currentUserProfilePicBase64, Base64.NO_WRAP)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    holder.profileImage.setImageBitmap(bitmap)
                } catch (e: IllegalArgumentException) {
                    holder.profileImage.setImageResource(R.drawable.person)
                }
            } else {
                holder.profileImage.setImageResource(R.drawable.person)
            }
            // >>> END PROFILE PICTURE LOGIC <<<

            // Story Ring Logic (Existing)
            if (hasActiveStory) {
                holder.ringContainer.setBackgroundResource(R.drawable.create_grad)
            } else {
                holder.ringContainer.background = null
            }

        } else {
            // --- LOGIC FOR ALL OTHER USERS' STORIES (Existing) ---
            holder.username.text = story.userId
            holder.addIcon.visibility = View.GONE

            // Default image for friends
            holder.profileImage.setImageResource(R.drawable.person)

            if (hasActiveStory) {
                holder.ringContainer.setBackgroundResource(R.drawable.create_grad)
            } else {
                holder.ringContainer.background = null
            }
        }

        holder.itemView.setOnClickListener {
            clickListener(story)
        }
    }

    override fun getItemCount(): Int = stories.size

    // NEW FUNCTION: Public method to receive the user's Base64 profile picture
    fun updateProfilePicture(base64: String?) {
        currentUserProfilePicBase64 = base64
        // Only refresh the first item (Your Story)
        if (stories.isNotEmpty() && stories.first().id == "YOUR_STORY_PLACEHOLDER") {
            notifyItemChanged(0)
        }
    }

    // EXISTING LOGIC: Handles fetching and merging stories
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
