data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    // You can add a status field if the user is already following this person
    var isFollowing: Boolean = false
)
