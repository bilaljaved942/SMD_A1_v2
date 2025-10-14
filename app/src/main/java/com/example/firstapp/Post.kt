// Post.kt (or define this at the top of your relevant files)

data class Post(
    val postId: String = "",
    val userId: String = "",
    val base64Image: String = "", // Base64 String for the media
    val caption: String = "",
    val location: String = "",
    val timestamp: Long = 0L
)