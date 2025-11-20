package com.example.firstapp.data.remote.api

import com.example.firstapp.data.remote.models.*
import retrofit2.Response
import retrofit2.http.*

interface PostApiService {
    
    @POST("posts/create")
    suspend fun createPost(@Body request: CreatePostRequest): Response<PostResponse>
    
    @GET("posts/feed/{userId}")
    suspend fun getUserFeed(
        @Path("userId") userId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PostsListResponse>
    
    @GET("posts/user/{userId}")
    suspend fun getUserPosts(@Path("userId") userId: String): Response<PostsListResponse>
    
    @GET("posts/{postId}")
    suspend fun getPostById(@Path("postId") postId: String): Response<PostResponse>
    
    @DELETE("posts/{postId}")
    suspend fun deletePost(@Path("postId") postId: String): Response<GenericResponse>
    
    @POST("posts/{postId}/like")
    suspend fun likePost(@Path("postId") postId: String, @Body request: LikeRequest): Response<LikeResponse>
    
    @DELETE("posts/{postId}/unlike")
    suspend fun unlikePost(
        @Path("postId") postId: String,
        @Query("userId") userId: String
    ): Response<LikeResponse>
    
    @GET("posts/{postId}/likes")
    suspend fun getPostLikes(@Path("postId") postId: String): Response<GenericResponse>
}
