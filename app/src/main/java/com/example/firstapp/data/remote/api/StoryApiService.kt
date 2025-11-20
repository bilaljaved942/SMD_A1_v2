package com.example.firstapp.data.remote.api

import com.example.firstapp.data.remote.models.*
import retrofit2.Response
import retrofit2.http.*

interface StoryApiService {
    
    @POST("stories/create")
    suspend fun createStory(@Body request: CreateStoryRequest): Response<StoryResponse>
    
    @GET("stories/fetch")
    suspend fun fetchActiveStories(
        @Query("userId") userId: String
    ): Response<StoriesListResponse>
    
    @GET("stories/user/{userId}")
    suspend fun getUserStories(@Path("userId") userId: String): Response<StoriesListResponse>
    
    @PUT("stories/{storyId}/view")
    suspend fun markStoryAsViewed(
        @Path("storyId") storyId: String,
        @Query("viewerId") viewerId: String
    ): Response<GenericResponse>
    
    @DELETE("stories/{storyId}")
    suspend fun deleteStory(@Path("storyId") storyId: String): Response<GenericResponse>
}
