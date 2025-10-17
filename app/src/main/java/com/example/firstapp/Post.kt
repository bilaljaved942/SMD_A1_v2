// Post.kt

data class Post(
    val postId: String = "",
    val userId: String = "",
    val base64Image: String = "",
    val caption: String = "",
    val location: String = "",
    val timestamp: Long = 0L
)