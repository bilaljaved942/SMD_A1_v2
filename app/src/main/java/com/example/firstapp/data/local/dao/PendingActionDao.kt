package com.example.firstapp.data.local.dao

import androidx.room.*
import com.example.firstapp.data.local.entities.PendingActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: PendingActionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<PendingActionEntity>)
    
    @Update
    suspend fun updateAction(action: PendingActionEntity)
    
    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    suspend fun getAllPendingActions(): List<PendingActionEntity>
    
    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    fun getAllPendingActionsFlow(): Flow<List<PendingActionEntity>>
    
    @Query("SELECT * FROM pending_actions WHERE actionType = :actionType")
    suspend fun getPendingActionsByType(actionType: String): List<PendingActionEntity>
    
    @Query("SELECT * FROM pending_actions WHERE retryCount < maxRetries ORDER BY createdAt ASC")
    suspend fun getRetriableActions(): List<PendingActionEntity>
    
    @Query("UPDATE pending_actions SET retryCount = retryCount + 1, lastAttemptAt = :timestamp, errorMessage = :error WHERE id = :actionId")
    suspend fun incrementRetryCount(actionId: Long, timestamp: Long, error: String?)
    
    @Delete
    suspend fun deleteAction(action: PendingActionEntity)
    
    @Query("DELETE FROM pending_actions WHERE id = :actionId")
    suspend fun deleteActionById(actionId: Long)
    
    @Query("DELETE FROM pending_actions WHERE entityId = :entityId AND actionType = :actionType")
    suspend fun deleteActionByEntity(entityId: String, actionType: String)
    
    @Query("DELETE FROM pending_actions")
    suspend fun deleteAllActions()
}
