package com.example.firstapp.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.firstapp.data.local.SociallyDatabase
import com.example.firstapp.data.remote.RetrofitClient
import com.example.firstapp.data.remote.models.*
import com.example.firstapp.utils.NetworkUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OfflineSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val database = SociallyDatabase.getDatabase(context)
    private val gson = Gson()
    private val TAG = "OfflineSyncWorker"
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
                Log.w(TAG, "No network available, postponing sync")
                return@withContext Result.retry()
            }
            
            Log.d(TAG, "Starting offline sync...")
            
            // Sync all pending actions
            val pendingActions = database.pendingActionDao().getRetriableActions()
            var successCount = 0
            var failCount = 0
            
            for (action in pendingActions) {
                try {
                    val success = processPendingAction(action)
                    if (success) {
                        database.pendingActionDao().deleteActionById(action.id)
                        successCount++
                    } else {
                        database.pendingActionDao().incrementRetryCount(
                            action.id,
                            System.currentTimeMillis(),
                            "Failed to sync"
                        )
                        failCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing action ${action.id}: ${e.message}")
                    database.pendingActionDao().incrementRetryCount(
                        action.id,
                        System.currentTimeMillis(),
                        e.message
                    )
                    failCount++
                }
            }
            
            Log.d(TAG, "Sync completed: $successCount success, $failCount failed")
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync worker error: ${e.message}")
            Result.retry()
        }
    }
    
    private suspend fun processPendingAction(action: com.example.firstapp.data.local.entities.PendingActionEntity): Boolean {
        return when (action.actionType) {
            "send_message" -> syncMessage(action)
            "upload_post" -> syncPost(action)
            "upload_story" -> syncStory(action)
            "follow_request" -> syncFollow(action)
            "like_post" -> syncLike(action)
            "add_comment" -> syncComment(action)
            else -> {
                Log.w(TAG, "Unknown action type: ${action.actionType}")
                true // Mark as success to remove it
            }
        }
    }
    
    private suspend fun syncMessage(action: com.example.firstapp.data.local.entities.PendingActionEntity): Boolean {
        return try {
            val request = gson.fromJson(action.payload, SendMessageRequest::class.java)
            val response = RetrofitClient.messageApi.sendMessage(request)
            
            if (response.isSuccessful && response.body() != null) {
                // Update local message as synced
                database.messageDao().markMessageAsSynced(action.entityId, System.currentTimeMillis())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing message: ${e.message}")
            false
        }
    }
    
    private suspend fun syncPost(action: com.example.firstapp.data.local.entities.PendingActionEntity): Boolean {
        return try {
            val request = gson.fromJson(action.payload, CreatePostRequest::class.java)
            val response = RetrofitClient.postApi.createPost(request)
            
            if (response.isSuccessful && response.body() != null) {
                database.postDao().markPostAsSynced(action.entityId, System.currentTimeMillis())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing post: ${e.message}")
            false
        }
    }
    
    private suspend fun syncStory(action: com.example.firstapp.data.local.entities.PendingActionEntity): Boolean {
        return try {
            val request = gson.fromJson(action.payload, CreateStoryRequest::class.java)
            val response = RetrofitClient.storyApi.createStory(request)
            
            if (response.isSuccessful && response.body() != null) {
                database.storyDao().markStoryAsSynced(action.entityId, System.currentTimeMillis())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing story: ${e.message}")
            false
        }
    }
    
    private suspend fun syncFollow(action: com.example.firstapp.data.local.entities.PendingActionEntity): Boolean {
        return try {
            val request = gson.fromJson(action.payload, FollowRequest::class.java)
            val response = RetrofitClient.followApi.sendFollowRequest(request)
            
            if (response.isSuccessful) {
                database.followDao().markFollowAsSynced(request.followerId, request.followingId)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing follow: ${e.message}")
            false
        }
    }
    
    private suspend fun syncLike(action: com.example.firstapp.data.local.entities.PendingActionEntity): Boolean {
        return try {
            val payload = gson.fromJson(action.payload, Map::class.java) as Map<*, *>
            val postId = payload["postId"] as? String ?: return false
            val userId = payload["userId"] as? String ?: return false
            
            val request = LikeRequest(postId, userId)
            val response = RetrofitClient.postApi.likePost(postId, request)
            
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing like: ${e.message}")
            false
        }
    }
    
    private suspend fun syncComment(action: com.example.firstapp.data.local.entities.PendingActionEntity): Boolean {
        return try {
            val request = gson.fromJson(action.payload, AddCommentRequest::class.java)
            val response = RetrofitClient.commentApi.addComment(request)
            
            if (response.isSuccessful) {
                database.commentDao().markCommentAsSynced(action.entityId, System.currentTimeMillis())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing comment: ${e.message}")
            false
        }
    }
}
