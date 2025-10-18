import com.google.firebase.database.PropertyName

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    // Use annotation to map the database field "profilePicture"
    @get:PropertyName("profilePicture") @set:PropertyName("profilePicture")
    var profilePictureBase64: String? = null,
    var isFollowing: Boolean = false
)
