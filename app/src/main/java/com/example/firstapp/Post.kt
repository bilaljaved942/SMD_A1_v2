// Post.kt

data class Post(
    // Keep default values for fields that aren't saved in MainActivity16
    val postId: String = "",
    val userId: String = "",

    // CRITICAL: This MUST match the key used for saving in MainActivity16
    val base64Image: String = "",

    val caption: String = "",
    val location: String = "",
    // CRITICAL: This also matches the key used for saving
    val timestamp: Long = 0L
)