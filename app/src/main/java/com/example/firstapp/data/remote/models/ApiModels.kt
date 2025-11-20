package com.example.firstapp.data.remote.models

import com.google.gson.annotations.SerializedName

// Authentication Models
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserResponse?,
    val token: String?
)

data class UserResponse(
    @SerializedName("uid")
    val uid: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("profilePicture")
    val profilePicture: String? = null,
    @SerializedName("coverPhoto")
    val coverPhoto: String? = null,
    @SerializedName("bio")
    val bio: String? = null,
    @SerializedName("fcmToken")
    val fcmToken: String? = null,
    @SerializedName("online")
    val online: Boolean = false,
    @SerializedName("lastOnline")
    val lastOnline: Long = 0L
)

data class SessionResponse(
    val success: Boolean,
    val isLoggedIn: Boolean,
    val user: UserResponse?
)

// Message Models
data class SendMessageRequest(
    val senderId: String,
    val receiverId: String,
    val content: String,
    val type: String = "text",
    val mediaBase64: String? = null,
    val vanishMode: Boolean = false,
    val callType: String? = null,
    val channelName: String? = null
)

data class MessageResponse(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    val mediaUrl: String? = null,
    val type: String = "text",
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val isDeleted: Boolean = false,
    val isSeen: Boolean = false,
    val seenAt: Long? = null,
    val callType: String? = null,
    val channelName: String? = null
)

data class MessagesListResponse(
    val success: Boolean,
    val messages: List<MessageResponse>
)

data class EditMessageRequest(
    val messageId: String,
    val content: String
)

data class DeleteMessageRequest(
    val messageId: String
)

// Post Models
data class CreatePostRequest(
    val userId: String,
    val caption: String,
    val location: String = "",
    val mediaBase64: String,
    val mediaType: String = "image"
)

data class PostResponse(
    val postId: String,
    val userId: String,
    val mediaUrl: String,
    val caption: String,
    val location: String,
    val timestamp: Long,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val userName: String? = null,
    val userProfilePicture: String? = null
)

data class PostsListResponse(
    val success: Boolean,
    val posts: List<PostResponse>
)

// Story Models
data class CreateStoryRequest(
    val userId: String,
    val mediaBase64: String,
    val isVideo: Boolean = false
)

data class StoryResponse(
    val id: String,
    val userId: String,
    val mediaUrl: String,
    val timestamp: Long,
    val expiresAt: Long,
    val viewed: Boolean = false,
    val isVideo: Boolean = false
)

data class StoriesListResponse(
    val success: Boolean,
    val stories: List<StoryResponse>
)

// Comment Models
data class AddCommentRequest(
    val postId: String,
    val userId: String,
    val content: String
)

data class CommentResponse(
    val commentId: String,
    val postId: String,
    val userId: String,
    val content: String,
    val timestamp: Long,
    val userName: String? = null,
    val userProfilePicture: String? = null
)

data class CommentsListResponse(
    val success: Boolean,
    val comments: List<CommentResponse>
)

// Like Models
data class LikeRequest(
    val postId: String,
    val userId: String
)

data class LikeResponse(
    val success: Boolean,
    val likesCount: Int
)

// Follow Models
data class FollowRequest(
    val followerId: String,
    val followingId: String
)

data class FollowResponse(
    val success: Boolean,
    val message: String,
    val status: String // "pending", "accepted"
)

data class FollowListResponse(
    val success: Boolean,
    val users: List<UserResponse>
)

// Search Models
data class SearchResponse(
    val success: Boolean,
    val users: List<UserResponse>
)

// Upload Models
data class UploadResponse(
    val success: Boolean,
    val url: String?,
    val message: String
)

// FCM Models
data class FcmTokenRequest(
    val userId: String,
    val token: String
)

data class SendNotificationRequest(
    val userId: String,
    val title: String,
    val body: String,
    val data: Map<String, String>? = null
)

// Online Status Models
data class OnlineStatusRequest(
    val userId: String,
    val online: Boolean
)

data class OnlineStatusResponse(
    val success: Boolean,
    val online: Boolean,
    val lastOnline: Long
)

// Generic Response
data class GenericResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)
