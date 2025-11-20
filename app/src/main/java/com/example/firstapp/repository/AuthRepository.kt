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
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {

    private val authApi = RetrofitClient.authApi
    private val userDao = SociallyDatabase.getDatabase(context).userDao()
    private val prefs = SecurePreferences(context)

    /**
     * Sign up a new user
     * @return Result with AuthResponse or error
     */
    suspend fun signup(name: String, email: String, password: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    return@withContext Result.failure(Exception("No internet connection"))
                }

                val request = SignupRequest(name, email, password)
                val response = authApi.signup(request)

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null && authResponse.success && authResponse.user != null) {
                        // Save to local database
                        val userEntity = UserEntity(
                            uid = authResponse.user?.uid?.toIntOrNull() ?: 0,
                            name = authResponse.user.name,
                            email = authResponse.user.email,
                            profilePictureBase64 = authResponse.user.profilePicture,
                            coverPhotoBase64 = authResponse.user.coverPhoto,
                            bio = authResponse.user.bio,
                            fcmToken = authResponse.user.fcmToken,
                            online = authResponse.user.online,
                            lastOnline = authResponse.user.lastOnline,
                            lastSynced = System.currentTimeMillis()
                        )
                        userDao.insertUser(userEntity)

                        // Save session
                        prefs.saveUserSession(authResponse.token!!,
                            authResponse.user.uid,
                            authResponse.user.email,
                            authResponse.user.name
                        )

                        Log.d("AuthRepository", "Signup successful: ${authResponse.user.email}")
                        Result.success(authResponse)
                    } else {
                        Result.failure(Exception(authResponse?.message ?: "Signup failed"))
                    }
                } else {
                    Result.failure(Exception("Server error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Signup error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Login existing user
     * @return Result with AuthResponse or error
     */
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    return@withContext Result.failure(Exception("No internet connection"))
                }

                val request = LoginRequest(email, password)
                val response = authApi.login(request)

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null && authResponse.success && authResponse.user != null) {
                        // Save to local database
                        val userEntity = UserEntity(
                            uid = authResponse.user?.uid?.toIntOrNull() ?: 0,
                            name = authResponse.user.name,
                            email = authResponse.user.email,
                            profilePictureBase64 = authResponse.user.profilePicture,
                            coverPhotoBase64 = authResponse.user.coverPhoto,
                            bio = authResponse.user.bio,
                            fcmToken = authResponse.user.fcmToken,
                            online = authResponse.user.online,
                            lastOnline = authResponse.user.lastOnline,
                            lastSynced = System.currentTimeMillis()
                        )
                        userDao.insertUser(userEntity)

                        // Save session
                        prefs.saveUserSession(authResponse.token!!,
                            authResponse.user.uid,
                            authResponse.user.email,
                            authResponse.user.name
                        )

                        Log.d("AuthRepository", "Login successful: ${authResponse.user.email}")
                        Result.success(authResponse)
                    } else {
                        Result.failure(Exception(authResponse?.message ?: "Login failed"))
                    }
                } else {
                    Result.failure(Exception("Invalid credentials"))
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Login error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Logout current user
     */
    suspend fun logout(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = prefs.getAuthToken()
                    if (token != null) {
                        val response = authApi.logout("Bearer $token")
                        if (!response.isSuccessful) {
                            Log.w("AuthRepository", "Logout API failed but clearing local session")
                        }
                    }
                }

                // Clear local session regardless of API response
                prefs.clearSession()
                Log.d("AuthRepository", "Logout successful")
                Result.success(Unit)
            } catch (e: Exception) {
                // Even if API fails, clear local session
                prefs.clearSession()
                Log.e("AuthRepository", "Logout error: ${e.message}", e)
                Result.success(Unit) // Consider it success if local cleared
            }
        }
    }

    /**
     * Check current session validity
     * @return Result with SessionResponse or error
     */
    suspend fun checkSession(): Result<SessionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = prefs.getAuthToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("No token found"))
                }

                if (!NetworkUtils.isNetworkAvailable(context)) {
                    // Use cached user data
                    val userId = prefs.getUserId() ?: ""
                    if (userId != null) {
                        val cachedUser = userDao.getUserById(userId)
                        // TODO: Convert Flow to single value if needed
                        return@withContext Result.failure(Exception("Offline - using cache"))
                    }
                    return@withContext Result.failure(Exception("No internet connection"))
                }

                val response = authApi.checkSession("Bearer $token")

                if (response.isSuccessful) {
                    val sessionResponse = response.body()
                    if (sessionResponse != null && sessionResponse.isLoggedIn) {
                        // Update local user data
                        sessionResponse.user?.let { user ->
                            val userEntity = UserEntity(
                                uid = user.uid.toIntOrNull() ?: 0,
                                name = user.name,
                                email = user.email,
                                profilePictureBase64 = user.profilePicture,
                                coverPhotoBase64 = user.coverPhoto,
                                bio = user.bio,
                                fcmToken = user.fcmToken,
                                online = user.online,
                                lastOnline = user.lastOnline,
                                lastSynced = System.currentTimeMillis()
                            )
                            userDao.insertUser(userEntity)
                        }

                        Log.d("AuthRepository", "Session valid")
                        Result.success(sessionResponse)
                    } else {
                        prefs.clearSession()
                        Result.failure(Exception("Session invalid"))
                    }
                } else {
                    prefs.clearSession()
                    Result.failure(Exception("Session expired"))
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Session check error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(
        name: String? = null,
        bio: String? = null,
        profilePicBase64: String? = null,
        coverPhotoBase64: String? = null
    ): Result<UserResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    return@withContext Result.failure(Exception("No internet connection"))
                }

                val token = prefs.getAuthToken() ?: return@withContext Result.failure(Exception("Not authenticated"))
                val userId = prefs.getUserId() ?: "" ?: return@withContext Result.failure(Exception("User ID not found"))

                // TODO: Implement update profile API call when backend supports it
                // For now, just update locally
                val cachedUser = userDao.getUserById(userId)
                // Update entity and save
                
                Result.failure(Exception("Update profile not implemented yet"))
            } catch (e: Exception) {
                Log.e("AuthRepository", "Update profile error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get current user from cache
     */
    suspend fun getCurrentUser(): UserEntity? {
        return withContext(Dispatchers.IO) {
            val userId = prefs.getUserId() ?: ""
            if (userId != null) {
                // TODO: Convert Flow to single value
                null
            } else {
                null
            }
        }
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return prefs.isLoggedIn()
    }
}
