data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    // NEW: Field to store the Base64 profile picture string
    val profilePictureBase64: String? = null,
    var isFollowing: Boolean = false
)
