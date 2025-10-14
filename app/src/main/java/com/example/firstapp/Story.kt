// Story.kt (or wherever your Story data class is defined)

data class Story(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val viewed: Boolean = false,
    val isVideo: Boolean = false // CRITICAL FIX: This field must be added
)