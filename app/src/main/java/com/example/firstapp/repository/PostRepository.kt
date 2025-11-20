package com.example.firstapp.repository

import android.content.Context
import android.util.Log
import com.example.firstapp.data.local.SociallyDatabase
import com.example.firstapp.data.local.entities.PostEntity
import com.example.firstapp.data.local.entities.PendingActionEntity
import com.example.firstapp.data.remote.RetrofitClient
import com.example.firstapp.data.remote.models.*
import com.example.firstapp.utils.SecurePreferences
import com.example.firstapp.utils.NetworkUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class PostRepository(private val context: Context) {

    private val postApi = RetrofitClient.postApi
    private val postDao = SociallyDatabase.getDatabase(context).postDao()
    private val pendingActionDao = SociallyDatabase.getDatabase(context).pendingActionDao()
    private val prefs = SecurePreferences(context)
    private val gson = Gson()

    /**
     * Create a new post
     */
    suspend fun createPost(
        caption: String?,
        mediaBase64: String?,
        location: String? = null
    ): Result<PostResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = prefs.getUserId() ?: "" ?: return@withContext Result.failure(Exception("Not logged in"))

                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken()!!
                    val request = CreatePostRequest(userId, caption ?: "", location ?: "", mediaBase64 ?: "", if (mediaBase64?.startsWith("data:video") == true) "video" else "image")
                    val response = postApi.createPost(request)

                    if (response.isSuccessful && response.isSuccessful) {
                        val postResponse = response.body()!!
                        
                        // Save to local database
                        val postEntity = PostEntity(
                            postId = postResponse.postId,
                            userId = postResponse.userId,
                            mediaBase64 = mediaBase64,
                            mediaUrl = postResponse.mediaUrl,
                            mediaType = if (postResponse.mediaUrl?.contains("video") == true) "video" else "image",
                            caption = postResponse.caption,
                            location = postResponse.location,
                            timestamp = System.currentTimeMillis(),
                            likesCount = postResponse.likesCount,
                            commentsCount = postResponse.commentsCount,
                            isSynced = true
                        )
                        postDao.insertPosts(listOf(postEntity))

                        Log.d("PostRepository", "Post created: ${postResponse.postId}")
                        Result.success(postResponse)
                    } else {
                        Result.failure(Exception("Failed to create post"))
                    }
                } else {
                    // Queue for later sync
                    val payload = gson.toJson(mapOf(
                        "caption" to caption,
                        "mediaBase64" to mediaBase64,
                        "location" to location
                    ))
                    
                    val pendingAction = PendingActionEntity(
                        actionType = "upload_post",
                        entityId = "temp_${System.currentTimeMillis()}",
                        payload = payload,
                        retryCount = 0,
                        maxRetries = 5
                    )
                    pendingActionDao.insertAction(pendingAction)

                    // Save locally with isSynced = false
                    val postEntity = PostEntity(
                        postId = pendingAction.entityId,
                        userId = userId,
                        mediaBase64 = mediaBase64,
                        mediaUrl = null,
                        mediaType = if (mediaBase64?.startsWith("data:video") == true) "video" else "image",
                        caption = caption ?: "",
                        location = location ?: "",
                        timestamp = System.currentTimeMillis(),
                        likesCount = 0,
                        commentsCount = 0,
                        isSynced = false
                    )
                    postDao.insertPosts(listOf(postEntity))

                    Log.d("PostRepository", "Post queued for sync")
                    Result.failure(Exception("No internet - post will sync later"))
                }
            } catch (e: Exception) {
                Log.e("PostRepository", "Create post error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get feed posts (user + following)
     */
    suspend fun getFeed(page: Int = 1, limit: Int = 20): Result<List<PostEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken()!!
                    val response = postApi.getUserFeed("Bearer $token", page, limit)

                    if (response.isSuccessful && response.isSuccessful) {
                        val posts = response.body()!!.posts.map { postResponse ->
                            PostEntity(
                                postId = postResponse.postId,
                                userId = postResponse.userId,
                                mediaBase64 = null, // Not sent from server
                                mediaUrl = postResponse.mediaUrl,
                                mediaType = if (postResponse.mediaUrl?.contains("video") == true) "video" else "image",
                                caption = postResponse.caption,
                                location = postResponse.location,
                                timestamp = System.currentTimeMillis(),
                                likesCount = postResponse.likesCount,
                                commentsCount = postResponse.commentsCount,
                                isSynced = true
                            )
                        }

                        // Cache in local database
                        if (page == 1) {
                            // Clear old cached posts on first page
                            // postDao.deleteAll() // Implement if needed
                        }
                        postDao.insertPosts(posts)

                        Log.d("PostRepository", "Loaded ${posts.size} posts from feed")
                        Result.success(posts)
                    } else {
                        // Fall back to cached data
                        val cachedPosts = postDao.getAllPosts() // This returns Flow, need to collect
                        Result.failure(Exception("API error - using cache"))
                    }
                } else {
                    // Use cached data
                    Log.d("PostRepository", "No internet - loading cached posts")
                    Result.failure(Exception("No internet - using cache"))
                }
            } catch (e: Exception) {
                Log.e("PostRepository", "Get feed error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get posts by user ID
     */
    suspend fun getPostsByUser(userId: String): Result<List<PostEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken()!!
                    val response = postApi.getUserPosts(userId)

                    if (response.isSuccessful && response.isSuccessful) {
                        val posts = response.body()!!.posts.map { postResponse ->
                            PostEntity(
                                postId = postResponse.postId,
                                userId = postResponse.userId,
                                mediaBase64 = null,
                                mediaUrl = postResponse.mediaUrl,
                                mediaType = if (postResponse.mediaUrl?.contains("video") == true) "video" else "image",
                                caption = postResponse.caption,
                                location = postResponse.location,
                                timestamp = System.currentTimeMillis(),
                                likesCount = postResponse.likesCount,
                                commentsCount = postResponse.commentsCount,
                                isSynced = true
                            )
                        }

                        postDao.insertPosts(posts)
                        Result.success(posts)
                    } else {
                        Result.failure(Exception("Failed to load user posts"))
                    }
                } else {
                    Result.failure(Exception("No internet connection"))
                }
            } catch (e: Exception) {
                Log.e("PostRepository", "Get user posts error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Like/Unlike a post
     */
    suspend fun toggleLike(postId: String, isLiked: Boolean): Result<LikeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val userId = prefs.getUserId() ?: return@withContext Result.failure(Exception("Not logged in")); val request = LikeRequest(postId, userId)
                    
                    val response = if (isLiked) {
                        postApi.unlikePost(postId, userId)
                    } else {
                        postApi.likePost(postId, request)
                    }

                    if (response.isSuccessful && response.isSuccessful) {
                        val likeResponse = response.body()!!
                        
                        // Update local like count
                        postDao.updateLikesCount(postId, likeResponse.likesCount)

                        Log.d("PostRepository", "Like toggled: $postId")
                        Result.success(likeResponse)
                    } else {
                        Result.failure(Exception("Failed to toggle like"))
                    }
                } else {
                    // Queue for later
                    val payload = gson.toJson(mapOf("postId" to postId, "isLiked" to !isLiked))
                    val pendingAction = PendingActionEntity(
                        actionType = "like_post",
                        entityId = postId,
                        payload = payload,
                        retryCount = 0,
                        maxRetries = 3
                    )
                    pendingActionDao.insertAction(pendingAction)
                    
                    // Update locally
                    val currentPost = postDao.getPostsByUser("") // TODO: Get specific post
                    // postDao.updateLikesCount(postId, if (isLiked) -1 else +1)

                    Result.failure(Exception("No internet - will sync later"))
                }
            } catch (e: Exception) {
                Log.e("PostRepository", "Toggle like error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Delete a post
     */
    suspend fun deletePost(postId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken()!!
                    val response = postApi.deletePost(postId)

                    if (response.isSuccessful) {
                        postDao.deletePostById(postId)
                        Log.d("PostRepository", "Post deleted: $postId")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to delete post"))
                    }
                } else {
                    Result.failure(Exception("No internet connection"))
                }
            } catch (e: Exception) {
                Log.e("PostRepository", "Delete post error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get all posts as Flow for reactive UI
     */
    fun getAllPostsFlow(): Flow<List<PostEntity>> {
        return postDao.getAllPostsFlow()
    }

    /**
     * Get posts by user as Flow
     */
    fun getPostsByUserFlow(userId: String): Flow<List<PostEntity>> {
        return postDao.getPostsByUserFlow(userId)
    }
}
