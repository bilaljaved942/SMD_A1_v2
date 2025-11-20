package com.example.firstapp.data.remote.api

import com.example.firstapp.data.remote.models.*
import retrofit2.Response
import retrofit2.http.*

interface CommentApiService {
    
    @POST("comments/add")
    suspend fun addComment(@Body request: AddCommentRequest): Response<CommentResponse>
    
    @GET("comments/{postId}")
    suspend fun getComments(@Path("postId") postId: String): Response<CommentsListResponse>
    
    @DELETE("comments/{commentId}")
    suspend fun deleteComment(@Path("commentId") commentId: String): Response<GenericResponse>
}
