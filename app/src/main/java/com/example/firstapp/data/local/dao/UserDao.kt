package com.example.firstapp.data.local.dao

import androidx.room.*
import com.example.firstapp.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Query("SELECT * FROM users WHERE uid = :userId")
    suspend fun getUserById(userId: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE uid = :userId")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>
    
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE name LIKE '%' || :query || '%'")
    suspend fun searchUsersByName(query: String): List<UserEntity>
    
    @Query("SELECT * FROM users WHERE online = 1")
    suspend fun getOnlineUsers(): List<UserEntity>
    
    @Query("UPDATE users SET online = :isOnline, lastOnline = :timestamp WHERE uid = :userId")
    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean, timestamp: Long)
    
    @Query("UPDATE users SET fcmToken = :token WHERE uid = :userId")
    suspend fun updateFcmToken(userId: String, token: String)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("DELETE FROM users WHERE uid = :userId")
    suspend fun deleteUserById(userId: String)
    
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>
}
