package com.example.firstapp.repository

import android.content.Context
import android.util.Log
import com.example.firstapp.data.local.SociallyDatabase
import com.example.firstapp.data.local.entities.FollowEntity
import com.example.firstapp.data.local.entities.PendingActionEntity
import com.example.firstapp.data.local.entities.UserEntity
import com.example.firstapp.data.remote.RetrofitClient
import com.example.firstapp.data.remote.models.*
import com.example.firstapp.utils.SecurePreferences
import com.example.firstapp.utils.NetworkUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class FollowRepository(private val context: Context) {

    private val followApi = RetrofitClient.followApi
    private val followDao = SociallyDatabase.getDatabase(context).followDao()
    private val userDao = SociallyDatabase.getDatabase(context).userDao()
    private val pendingActionDao = SociallyDatabase.getDatabase(context).pendingActionDao()
    private val prefs = SecurePreferences(context)
    private val gson = Gson()

    /**
     * Follow a user
     */
    suspend fun followUser(followingId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val followerId = prefs.getUserId() ?: "" ?: return@withContext Result.failure(Exception("Not logged in"))

                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken()!!
                    val request = FollowRequest(followerId, followingId)
                    val response = followApi.sendFollowRequest(request)

                    if (response.isSuccessful && response.body()?.success == true) {
                        // Save to local database
                        val followEntity = FollowEntity(
                            followerId = followerId,
                            followingId = followingId,
                            status = "accepted", // Auto-accept for now
                            timestamp = System.currentTimeMillis(),
                            isSynced = true
                        )
                        followDao.insertFollow(followEntity)

                        Log.d("FollowRepository", "Followed user: $followingId")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(response.body()?.message ?: "Failed to follow"))
                    }
                } else {
                    // Queue for later
                    val payload = gson.toJson(mapOf("followingId" to followingId))
                    val pendingAction = PendingActionEntity(
                        actionType = "follow_request",
                        entityId = followingId,
                        payload = payload,
                        retryCount = 0,
                        maxRetries = 3
                    )
                    pendingActionDao.insertAction(pendingAction)

                    // Save locally with isSynced = false
                    val followEntity = FollowEntity(
                        followerId = followerId,
                        followingId = followingId,
                        status = "pending",
                        timestamp = System.currentTimeMillis(),
                        isSynced = false
                    )
                    followDao.insertFollow(followEntity)

                    Result.failure(Exception("No internet - will sync later"))
                }
            } catch (e: Exception) {
                Log.e("FollowRepository", "Follow error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Unfollow a user
     */
    suspend fun unfollowUser(followingId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val followerId = prefs.getUserId() ?: "" ?: return@withContext Result.failure(Exception("Not logged in"))

                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken()!!
                    val response = followApi.unfollowUser("Bearer $token", followingId)

                    if (response.isSuccessful && response.body()?.success == true) {
                        // Delete from local database
                        followDao.deleteFollowRelation(followerId, followingId)

                        Log.d("FollowRepository", "Unfollowed user: $followingId")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to unfollow"))
                    }
                } else {
                    Result.failure(Exception("No internet connection"))
                }
            } catch (e: Exception) {
                Log.e("FollowRepository", "Unfollow error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get followers list
     */
    suspend fun getFollowers(userId: String): Result<List<UserEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken() ?: return@withContext Result.failure(Exception("Not logged in"))
                    val response = followApi.getFollowers(userId)

                    if (response.isSuccessful && response.body()?.success == true) {
                        val users = response.body()!!.users.map { userResponse ->
                            UserEntity(
                                uid = userResponse.uid.toIntOrNull() ?: 0,
                                name = userResponse.name,
                                email = userResponse.email,
                                profilePictureBase64 = userResponse.profilePicture,
                                coverPhotoBase64 = userResponse.coverPhoto,
                                bio = userResponse.bio,
                                fcmToken = userResponse.fcmToken,
                                online = userResponse.online,
                                lastOnline = userResponse.lastOnline,
                                lastSynced = System.currentTimeMillis()
                            )
                        }

                        // Cache users
                        users.forEach { userDao.insertUser(it) }

                        Log.d("FollowRepository", "Got ${users.size} followers")
                        Result.success(users)
                    } else {
                        Result.failure(Exception("Failed to get followers"))
                    }
                } else {
                    Result.failure(Exception("No internet connection"))
                }
            } catch (e: Exception) {
                Log.e("FollowRepository", "Get followers error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get following list
     */
    suspend fun getFollowing(userId: String): Result<List<UserEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken() ?: return@withContext Result.failure(Exception("Not logged in"))
                    val response = followApi.getFollowing(userId)

                    if (response.isSuccessful && response.body()?.success == true) {
                        val users = response.body()!!.users.map { userResponse ->
                            UserEntity(
                                uid = userResponse.uid.toIntOrNull() ?: 0,
                                name = userResponse.name,
                                email = userResponse.email,
                                profilePictureBase64 = userResponse.profilePicture,
                                coverPhotoBase64 = userResponse.coverPhoto,
                                bio = userResponse.bio,
                                fcmToken = userResponse.fcmToken,
                                online = userResponse.online,
                                lastOnline = userResponse.lastOnline,
                                lastSynced = System.currentTimeMillis()
                            )
                        }

                        // Cache users
                        users.forEach { userDao.insertUser(it) }

                        Log.d("FollowRepository", "Got ${users.size} following")
                        Result.success(users)
                    } else {
                        Result.failure(Exception("Failed to get following"))
                    }
                } else {
                    Result.failure(Exception("No internet connection"))
                }
            } catch (e: Exception) {
                Log.e("FollowRepository", "Get following error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get mutual followers (for chat list)
     */
    suspend fun getMutualFollowers(): Result<List<UserEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = prefs.getUserId() ?: "" ?: return@withContext Result.failure(Exception("Not logged in"))
                
                // Get users I follow
                val followingResult = getFollowing(currentUserId)
                if (followingResult.isFailure) {
                    return@withContext followingResult
                }
                
                val following = followingResult.getOrNull() ?: emptyList()
                val mutualUsers = mutableListOf<UserEntity>()
                
                // Check if they follow me back
                for (user in following) {
                    val followersResult = getFollowers(user.uid.toString())
                    if (followersResult.isSuccess) {
                        val followers = followersResult.getOrNull() ?: emptyList()
                        if (followers.any { it.uid.toString() == currentUserId }) {
                            mutualUsers.add(user)
                        }
                    }
                }
                
                Log.d("FollowRepository", "Got ${mutualUsers.size} mutual followers")
                Result.success(mutualUsers)
            } catch (e: Exception) {
                Log.e("FollowRepository", "Get mutual followers error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Check if following a user
     */
    suspend fun isFollowing(followingId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val followerId = prefs.getUserId() ?: "" ?: return@withContext false
                val follow = followDao.getFollow(followerId, followingId)
                follow != null && follow.status == "accepted"
            } catch (e: Exception) {
                Log.e("FollowRepository", "Check following error: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Get followers as Flow
     */
    fun getFollowersFlow(userId: String): Flow<List<FollowEntity>> {
        return flow { emit(followDao.getFollowers(userId)) }
    }

    /**
     * Get following as Flow
     */
    fun getFollowingFlow(userId: String): Flow<List<FollowEntity>> {
        return flow { emit(followDao.getFollowing(userId)) }
    }
}
