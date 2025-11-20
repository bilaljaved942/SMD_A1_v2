package com.example.firstapp.data.remote.api

import com.example.firstapp.data.remote.models.*
import retrofit2.Response
import retrofit2.http.*

interface UserApiService {
    
    @GET("users/search")
    suspend fun searchUsers(
        @Query("query") query: String,
        @Query("filter") filter: String? = null // "followers", "following", or null for all
    ): Response<SearchResponse>
    
    @POST("users/online")
    suspend fun setOnlineStatus(@Body request: OnlineStatusRequest): Response<OnlineStatusResponse>
    
    @POST("users/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): Response<GenericResponse>
    
    @GET("users/online-status/{userId}")
    suspend fun getOnlineStatus(@Path("userId") userId: String): Response<OnlineStatusResponse>
    
    @POST("notifications/send")
    suspend fun sendNotification(@Body request: SendNotificationRequest): Response<GenericResponse>
}
