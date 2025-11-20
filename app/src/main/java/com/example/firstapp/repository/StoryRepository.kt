package com.example.firstapp.repository

import android.content.Context
import android.util.Log
import com.example.firstapp.data.local.SociallyDatabase
import com.example.firstapp.data.local.entities.StoryEntity
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

class StoryRepository(private val context: Context) {

    private val storyApi = RetrofitClient.storyApi
    private val storyDao = SociallyDatabase.getDatabase(context).storyDao()
    private val pendingActionDao = SociallyDatabase.getDatabase(context).pendingActionDao()
    private val prefs = SecurePreferences(context)
    private val gson = Gson()

    /**
     * Create a new story
     */
    suspend fun createStory(mediaBase64: String): Result<StoryResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = prefs.getUserId() ?: "" ?: return@withContext Result.failure(Exception("Not logged in"))

                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken()!!
                    val request = CreateStoryRequest(userId, mediaBase64)
                    val response = storyApi.createStory(request)

                    if (response.isSuccessful) {
                        val storyResponse = response.body()!!
                        
                        // Save to local database
                        val storyEntity = StoryEntity(
                            id = storyResponse.id,
                            userId = storyResponse.userId,
                            mediaUrl = storyResponse.mediaUrl,
                            mediaBase64 = mediaBase64,
                            timestamp = System.currentTimeMillis(),
                            expiresAt = storyResponse.expiresAt,
                            viewed = false,
                            isVideo = storyResponse.isVideo,
                            isSynced = true
                        )
                        storyDao.insertStories(listOf(storyEntity))

                        Log.d("StoryRepository", "Story created: ${storyResponse.id}")
                        Result.success(storyResponse)
                    } else {
                        Result.failure(Exception("Failed to create story"))
                    }
                } else {
                    // Queue for later sync
                    val payload = gson.toJson(mapOf("mediaBase64" to mediaBase64))
                    
                    val pendingAction = PendingActionEntity(
                        actionType = "upload_story",
                        entityId = "temp_${System.currentTimeMillis()}",
                        payload = payload,
                        retryCount = 0,
                        maxRetries = 5
                    )
                    pendingActionDao.insertAction(pendingAction)

                    // Save locally with isSynced = false
                    val storyEntity = StoryEntity(
                        id = pendingAction.entityId,
                        userId = userId,
                        mediaUrl = "",
                        mediaBase64 = mediaBase64 ?: "",
                        timestamp = System.currentTimeMillis(),
                        expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 24 hours
                        viewed = false,
                        isVideo = mediaBase64?.startsWith("data:video") == true,
                        isSynced = false
                    )
                    storyDao.insertStories(listOf(storyEntity))

                    Log.d("StoryRepository", "Story queued for sync")
                    Result.failure(Exception("No internet - story will sync later"))
                }
            } catch (e: Exception) {
                Log.e("StoryRepository", "Create story error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Fetch active stories (from user + following)
     */
    suspend fun fetchActiveStories(): Result<List<StoryEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken()!!
                    val currentUserId = prefs.getUserId() ?: return@withContext Result.failure(Exception("Not logged in"))
                    val response = storyApi.fetchActiveStories(currentUserId)

                    if (response.isSuccessful && response.body()?.success == true) {
                        val stories = response.body()!!.stories.map { storyResponse ->
                            StoryEntity(
                                id = storyResponse.id,
                                userId = storyResponse.userId,
                                mediaUrl = storyResponse.mediaUrl,
                                mediaBase64 = null, // Not sent from server
                                timestamp = System.currentTimeMillis(),
                                expiresAt = storyResponse.expiresAt,
                                viewed = storyResponse.viewed ?: false,
                                isVideo = storyResponse.isVideo,
                                isSynced = true
                            )
                        }

                        // Delete expired stories first
                        storyDao.deleteExpiredStories(System.currentTimeMillis())

                        // Cache in local database
                        storyDao.insertStories(stories)

                        Log.d("StoryRepository", "Loaded ${stories.size} stories")
                        Result.success(stories)
                    } else {
                        // Fall back to cached data
                        Result.failure(Exception("API error - using cache"))
                    }
                } else {
                    // Use cached data
                    Log.d("StoryRepository", "No internet - loading cached stories")
                    Result.failure(Exception("No internet - using cache"))
                }
            } catch (e: Exception) {
                Log.e("StoryRepository", "Fetch stories error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get stories by user ID
     */
    suspend fun getStoriesByUser(userId: String): Result<List<StoryEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken()!!
                    val response = storyApi.getUserStories(userId)

                    if (response.isSuccessful && response.body()?.success == true) {
                        val stories = response.body()!!.stories.map { storyResponse ->
                            StoryEntity(
                                id = storyResponse.id,
                                userId = storyResponse.userId,
                                mediaUrl = storyResponse.mediaUrl,
                                mediaBase64 = null,
                                timestamp = System.currentTimeMillis(),
                                expiresAt = storyResponse.expiresAt,
                                viewed = storyResponse.viewed ?: false,
                                isVideo = storyResponse.isVideo,
                                isSynced = true
                            )
                        }

                        storyDao.insertStories(stories)
                        Result.success(stories)
                    } else {
                        Result.failure(Exception("Failed to load user stories"))
                    }
                } else {
                    Result.failure(Exception("No internet connection"))
                }
            } catch (e: Exception) {
                Log.e("StoryRepository", "Get user stories error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Mark story as viewed
     */
    suspend fun markStoryViewed(storyId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Update locally immediately
                storyDao.markStoryAsViewed(storyId)

                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken()!!
                    val viewerId = prefs.getUserId() ?: return@withContext Result.failure(Exception("Not logged in"))
                    val response = storyApi.markStoryAsViewed(storyId, viewerId)

                    if (response.isSuccessful) {
                        Log.d("StoryRepository", "Story viewed: $storyId")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to mark story as viewed"))
                    }
                } else {
                    // Already updated locally, will sync later if needed
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.e("StoryRepository", "Mark viewed error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Delete a story
     */
    suspend fun deleteStory(storyId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken()!!
                    val response = storyApi.deleteStory(storyId)

                    if (response.isSuccessful) {
                        storyDao.deleteStoryById(storyId)
                        Log.d("StoryRepository", "Story deleted: $storyId")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to delete story"))
                    }
                } else {
                    Result.failure(Exception("No internet connection"))
                }
            } catch (e: Exception) {
                Log.e("StoryRepository", "Delete story error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get active stories as Flow for reactive UI
     */
    fun getActiveStoriesFlow(): Flow<List<StoryEntity>> {
        val currentTime = System.currentTimeMillis()
        return storyDao.getActiveStories(currentTime)
    }

    /**
     * Get stories by user as Flow
     */
    fun getStoriesByUserFlow(userId: String): Flow<List<StoryEntity>> {
        val currentTime = System.currentTimeMillis()
        return flow { emit(storyDao.getActiveStoriesByUser(userId, currentTime)) }
    }

    /**
     * Clean up expired stories
     */
    suspend fun cleanupExpiredStories() {
        withContext(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                storyDao.deleteExpiredStories(currentTime)
                Log.d("StoryRepository", "Expired stories cleaned up")
            } catch (e: Exception) {
                Log.e("StoryRepository", "Cleanup error: ${e.message}", e)
            }
        }
    }
}
