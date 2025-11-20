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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// *** DATA CLASS DEFINITION (Ensuring one definition for the project) ***
data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val mediaBase64: String? = null,
    val type: String = "text", // Can be "text", "image", or "call_invite"
    val isEdited: Boolean = false,
    val callType: String? = null, // "video" or "audio"
    val channelName: String? = null // Agora Channel ID
)
// *** END DEFINITION ***


class MessageAdapter(
    private val messageList: List<Message>,
    private val currentUserId: String,
    // Handlers passed from the Activity
    private val onClickForDelete: (Message) -> Unit,
    private val onLongClickForEdit: (Message) -> Unit,
    private val onJoinCallClick: ((Message) -> Unit)? = null // New parameter for joining calls
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val EDIT_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(5)

    // View Type Constants
    private val VIEW_TYPE_SENT_TEXT = 1
    private val VIEW_TYPE_RECEIVED_TEXT = 2
    private val VIEW_TYPE_SENT_IMAGE = 3
    private val VIEW_TYPE_RECEIVED_IMAGE = 4
    private val VIEW_TYPE_CALL_INVITE = 5 // New view type

    // Base ViewHolder
    open class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class TextViewHolder(view: View) : MessageViewHolder(view) {
        val messageBody: TextView = view.findViewById(R.id.text_message_body)
        val messageTime: TextView? = view.findViewById(R.id.text_message_time)
        val editIndicator: TextView? = view.findViewById(R.id.text_edit_indicator)
    }

    class ImageViewHolder(view: View) : MessageViewHolder(view) {
        val mediaImageView: ImageView = view.findViewById(R.id.image_message_media)
        val messageTime: TextView? = view.findViewById(R.id.image_message_time)
        val editIndicator: TextView? = view.findViewById(R.id.image_edit_indicator)
    }

    class CallInviteViewHolder(view: View) : MessageViewHolder(view) {
        val callInviteText: TextView = view.findViewById(R.id.call_invite_text)
        val callTypeIcon: ImageView = view.findViewById(R.id.call_type_icon)
        val joinCallButton: android.widget.Button = view.findViewById(R.id.join_call_button)
        val messageTime: TextView = view.findViewById(R.id.call_message_time)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        val isSender = message.senderId == currentUserId

        return when {
            message.type == "call_invite" -> VIEW_TYPE_CALL_INVITE
            message.type == "image" -> if (isSender) VIEW_TYPE_SENT_IMAGE else VIEW_TYPE_RECEIVED_IMAGE
            else -> if (isSender) VIEW_TYPE_SENT_TEXT else VIEW_TYPE_RECEIVED_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_SENT_TEXT -> TextViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false))
            VIEW_TYPE_RECEIVED_TEXT -> TextViewHolder(inflater.inflate(R.layout.item_message_received, parent, false))
            VIEW_TYPE_SENT_IMAGE -> ImageViewHolder(inflater.inflate(R.layout.item_message_image, parent, false))
            VIEW_TYPE_RECEIVED_IMAGE -> ImageViewHolder(inflater.inflate(R.layout.item_message_image_received, parent, false))
            VIEW_TYPE_CALL_INVITE -> CallInviteViewHolder(inflater.inflate(R.layout.item_message_call_invite, parent, false))

            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        val formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
        val isSender = message.senderId == currentUserId

        val isWithinEditWindow = System.currentTimeMillis() - message.timestamp < EDIT_WINDOW_MILLIS

        // --- 1. SET LISTENERS AND TIME ---
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)

        when (holder) {
            is CallInviteViewHolder -> {
                holder.messageTime.text = formattedTime
                val callTypeText = if (message.callType == "video") "Video Call" else "Voice Call"
                holder.callInviteText.text = callTypeText
                
                // Set icon based on call type
                if (message.callType == "video") {
                    holder.callTypeIcon.setImageResource(R.drawable.img19)
                } else {
                    holder.callTypeIcon.setImageResource(R.drawable.call)
                }

                // Handle Join Call button click
                holder.joinCallButton.setOnClickListener {
                    onJoinCallClick?.invoke(message)
                }
            }
            is TextViewHolder -> {
                holder.messageTime?.text = formattedTime
                holder.messageBody.text = message.content
                holder.editIndicator?.visibility = if (message.isEdited) View.VISIBLE else View.GONE

                if (isSender && isWithinEditWindow) {
                    holder.itemView.setOnClickListener { onClickForDelete(message) }
                    holder.itemView.setOnLongClickListener {
                        onLongClickForEdit(message)
                        true
                    }
                }
            }
            is ImageViewHolder -> {
                holder.messageTime?.text = formattedTime
                holder.editIndicator?.visibility = if (message.isEdited) View.VISIBLE else View.GONE

                if (isSender && isWithinEditWindow) {
                    holder.itemView.setOnClickListener { onClickForDelete(message) }
                }

                // --- 2. IMAGE DECODING ---
                message.mediaBase64?.let { base64 ->
                    try {
                        val decodedBytes: ByteArray = Base64.decode(base64, Base64.NO_WRAP)
                        val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        if (decodedBitmap != null) {
                            Glide.with(holder.mediaImageView.context)
                                .load(decodedBitmap)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .placeholder(R.drawable.loading_placeholder)
                                .error(R.drawable.error_placeholder)
                                .into(holder.mediaImageView)
                        } else {
                            holder.mediaImageView.setImageResource(R.drawable.error_placeholder)
                        }
                    } catch (e: Exception) {
                        Log.e("ChatAdapter", "Image decode failed for ${message.id}: ${e.message}")
                        holder.mediaImageView.setImageResource(R.drawable.error_placeholder)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = messageList.size
}
