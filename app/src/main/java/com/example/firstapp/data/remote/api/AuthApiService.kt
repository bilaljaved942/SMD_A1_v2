package com.example.firstapp.data.remote.api

import com.example.firstapp.data.remote.models.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    
    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<GenericResponse>
    
    @GET("auth/session")
    suspend fun checkSession(@Header("Authorization") token: String): Response<SessionResponse>
    
    @GET("users/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): Response<UserResponse>
    
    @PUT("users/{userId}")
    suspend fun updateUserProfile(
        @Path("userId") userId: String,
        @Body user: UserResponse
    ): Response<GenericResponse>
    
    @Multipart
    @POST("users/{userId}/profile-picture")
    suspend fun uploadProfilePicture(
        @Path("userId") userId: String,
        @Part("image") imageBase64: String
    ): Response<UploadResponse>
    
    @Multipart
    @POST("users/{userId}/cover-photo")
    suspend fun uploadCoverPhoto(
        @Path("userId") userId: String,
        @Part("image") imageBase64: String
    ): Response<UploadResponse>
}
