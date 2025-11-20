package com.example.firstapp.data.remote.api

import com.example.firstapp.data.remote.models.*
import retrofit2.Response
import retrofit2.http.*

interface FollowApiService {
    
    @POST("follow/request")
    suspend fun sendFollowRequest(@Body request: FollowRequest): Response<FollowResponse>
    
    @POST("follow/accept")
    suspend fun acceptFollowRequest(@Body request: FollowRequest): Response<GenericResponse>
    
    @POST("follow/reject")
    suspend fun rejectFollowRequest(@Body request: FollowRequest): Response<GenericResponse>
    
    @DELETE("follow/unfollow")
    suspend fun unfollowUser(
        @Query("followerId") followerId: String,
        @Query("followingId") followingId: String
    ): Response<GenericResponse>
    
    @GET("follow/followers/{userId}")
    suspend fun getFollowers(@Path("userId") userId: String): Response<FollowListResponse>
    
    @GET("follow/following/{userId}")
    suspend fun getFollowing(@Path("userId") userId: String): Response<FollowListResponse>
    
    @GET("follow/pending/{userId}")
    suspend fun getPendingRequests(@Path("userId") userId: String): Response<FollowListResponse>
    
    @GET("follow/status")
    suspend fun getFollowStatus(
        @Query("followerId") followerId: String,
        @Query("followingId") followingId: String
    ): Response<GenericResponse>
}
