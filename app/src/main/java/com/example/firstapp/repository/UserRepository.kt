package com.example.firstapp.repository

import android.content.Context
import android.util.Log
import com.example.firstapp.data.local.SociallyDatabase
import com.example.firstapp.data.local.entities.UserEntity
import com.example.firstapp.data.remote.RetrofitClient
import com.example.firstapp.data.remote.models.*
import com.example.firstapp.utils.SecurePreferences
import com.example.firstapp.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class UserRepository(private val context: Context) {

    private val userApi = RetrofitClient.userApi
    private val userDao = SociallyDatabase.getDatabase(context).userDao()
    private val prefs = SecurePreferences(context)

    /**
     * Search users by query
     */
    suspend fun searchUsers(query: String): Result<List<UserEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    // Search in local cache
                    val cachedUsers = userDao.searchUsersByName("%$query%")
                    return@withContext Result.failure(Exception("Offline - showing cached results"))
                }

                val token = prefs.getAuthToken() ?: return@withContext Result.failure(Exception("Not logged in"))
                val response = userApi.searchUsers("Bearer $token", query)

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

                    // Cache search results
                    users.forEach { userDao.insertUser(it) }

                    Log.d("UserRepository", "Found ${users.size} users for query: $query")
                    Result.success(users)
                } else {
                    Result.failure(Exception("Search failed"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Search error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): Result<UserEntity> {
        return withContext(Dispatchers.IO) {
            try {
                // Try cache first
                val cachedUser = userDao.getUserById(userId)
                // Note: getUserById returns Flow, need to collect first value
                // For now, try API
                
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    return@withContext Result.failure(Exception("No internet connection"))
                }

                val token = prefs.getAuthToken() ?: return@withContext Result.failure(Exception("Not logged in"))
                // Assuming there's a getUserById endpoint (not in current API list)
                // For now, use search
                val response = userApi.searchUsers("Bearer $token", userId)

                if (response.isSuccessful && response.body()?.success == true) {
                    val userResponse = response.body()!!.users.find { it.uid == userId }
                    if (userResponse != null) {
                        val userEntity = UserEntity(
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
                        userDao.insertUser(userEntity)
                        Result.success(userEntity)
                    } else {
                        Result.failure(Exception("User not found"))
                    }
                } else {
                    Result.failure(Exception("Failed to fetch user"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Get user error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update online status
     */
    suspend fun updateOnlineStatus(online: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = prefs.getUserId() ?: "" ?: return@withContext Result.failure(Exception("Not logged in"))
                
                // Update locally first
                userDao.updateUserOnlineStatus(userId, online, System.currentTimeMillis())

                if (!NetworkUtils.isNetworkAvailable(context)) {
                    return@withContext Result.success(Unit)
                }

                val token = prefs.getAuthToken()!!
                val request = OnlineStatusRequest(userId, online)
                val response = userApi.setOnlineStatus(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("UserRepository", "Online status updated: $online")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to update status"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Update status error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get user's online status
     */
    suspend fun getUserOnlineStatus(userId: String): Result<OnlineStatusResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    return@withContext Result.failure(Exception("No internet connection"))
                }

                val token = prefs.getAuthToken() ?: return@withContext Result.failure(Exception("Not logged in"))
                val response = userApi.getOnlineStatus(userId)

                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to get status"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Get status error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Register FCM token
     */
    suspend fun registerFcmToken(fcmToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = prefs.getUserId() ?: "" ?: return@withContext Result.failure(Exception("Not logged in"))
                
                // Update locally
                userDao.updateFcmToken(userId, fcmToken)

                if (!NetworkUtils.isNetworkAvailable(context)) {
                    return@withContext Result.success(Unit)
                }

                val token = prefs.getAuthToken()!!
                val currentUserId = prefs.getUserId() ?: return@withContext Result.failure(Exception("Not logged in"))
                val request = FcmTokenRequest(currentUserId, fcmToken)
                val response = userApi.registerFcmToken(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("UserRepository", "FCM token registered")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to register FCM token"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "FCM token error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get online users
     */
    fun getOnlineUsersFlow(): Flow<List<UserEntity>> {
        return flow { emit(userDao.getOnlineUsers()) }
    }

    /**
     * Get user by ID as Flow
     */
    fun getUserByIdFlow(userId: String): Flow<UserEntity?> {
        return flow { emit(userDao.getUserById(userId)) }
    }

    /**
     * Search users in local cache
     */
    fun searchUsersInCache(query: String): Flow<List<UserEntity>> {
        return flow { emit(userDao.searchUsersByName("%$query%")) }
    }
}
