package com.example.firstapp.data.remote.api

import com.example.firstapp.data.remote.models.*
import retrofit2.Response
import retrofit2.http.*

interface MessageApiService {
    
    @POST("messages/send")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<MessageResponse>
    
    @GET("messages/fetch")
    suspend fun fetchMessages(
        @Query("userId1") userId1: String,
        @Query("userId2") userId2: String,
        @Query("since") since: Long? = null
    ): Response<MessagesListResponse>
    
    @PUT("messages/edit")
    suspend fun editMessage(@Body request: EditMessageRequest): Response<GenericResponse>
    
    @HTTP(method = "DELETE", path = "messages/delete", hasBody = true)
    suspend fun deleteMessage(@Body request: DeleteMessageRequest): Response<GenericResponse>
    
    @POST("messages/uploadMedia")
    suspend fun uploadMedia(
        @Body mediaRequest: Map<String, String>
    ): Response<UploadResponse>
    
    @PUT("messages/{messageId}/seen")
    suspend fun markMessageAsSeen(@Path("messageId") messageId: String): Response<GenericResponse>
    
    @GET("messages/conversations/{userId}")
    suspend fun getUserConversations(@Path("userId") userId: String): Response<MessagesListResponse>
}
