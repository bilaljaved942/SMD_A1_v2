// Post.kt

data class Post(
    val postId: String = "",
    val userId: String = "",
    val base64Image: String = "",
    val imageUrl: String? = null,
    val caption: String = "",
    val location: String = "",
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    val commentsCount: Int = 0
)