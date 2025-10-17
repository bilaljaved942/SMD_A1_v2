// Message.kt (Ensure this class includes the 'callType' field)

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val mediaBase64: String? = null,
    val type: String = "text", // Can be "text", "image", or "call_invite"
    val isEdited: Boolean = false,
    // NEW: Fields for call signaling
    val callType: String? = null, // "video" or "audio"
    val channelName: String? = null // Agora Channel ID
)