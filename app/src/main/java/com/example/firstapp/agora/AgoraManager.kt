package com.example.firstapp.agora

import android.content.Context
import android.util.Log
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

/**
 * Agora Manager - Handles RTC engine initialization and management
 * 
 * This singleton manages the Agora RTC engine lifecycle
 */
object AgoraManager {
    
    private const val TAG = "AgoraManager"
    
    private var rtcEngine: RtcEngine? = null
    private var isInitialized = false
    
    /**
     * Initialize Agora RTC Engine
     * Call this once in Application onCreate() or before first use
     */
    fun initialize(context: Context, eventHandler: IRtcEngineEventHandler): Boolean {
        if (isInitialized && rtcEngine != null) {
            Log.d(TAG, "Agora already initialized")
            return true
        }
        
        if (!AgoraConfig.isConfigured()) {
            Log.e(TAG, "Agora not configured. Add App ID in AgoraConfig.kt")
            return false
        }
        
        try {
            val config = RtcEngineConfig().apply {
                mContext = context.applicationContext
                mAppId = AgoraConfig.APP_ID
                mEventHandler = eventHandler
                mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                mAudioScenario = Constants.AUDIO_SCENARIO_DEFAULT
            }
            
            rtcEngine = RtcEngine.create(config)
            isInitialized = true
            
            Log.d(TAG, "Agora RTC Engine initialized successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Agora: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Get RTC Engine instance
     */
    fun getEngine(): RtcEngine? {
        if (!isInitialized) {
            Log.e(TAG, "Agora not initialized. Call initialize() first")
        }
        return rtcEngine
    }
    
    /**
     * Join a channel
     * @param channelName Unique channel name
     * @param token Optional token for authentication (null for testing)
     * @param uid User ID (0 for auto-assign)
     */
    fun joinChannel(channelName: String, token: String? = null, uid: Int = 0): Boolean {
        val engine = rtcEngine ?: run {
            Log.e(TAG, "RTC Engine not initialized")
            return false
        }
        
        try {
            // Enable audio
            engine.enableAudio()
            
            // Join channel
            val result = engine.joinChannel(token, channelName, uid, null)
            
            if (result == 0) {
                Log.d(TAG, "Successfully joined channel: $channelName")
                return true
            } else {
                Log.e(TAG, "Failed to join channel: $result")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error joining channel: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Leave current channel
     */
    fun leaveChannel(): Boolean {
        val engine = rtcEngine ?: return false
        
        try {
            val result = engine.leaveChannel()
            Log.d(TAG, "Left channel with result: $result")
            return result == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving channel: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Enable/disable local audio
     */
    fun muteLocalAudio(muted: Boolean) {
        rtcEngine?.muteLocalAudioStream(muted)
        Log.d(TAG, "Local audio muted: $muted")
    }
    
    /**
     * Enable/disable speaker
     */
    fun enableSpeaker(enabled: Boolean) {
        rtcEngine?.setEnableSpeakerphone(enabled)
        Log.d(TAG, "Speaker enabled: $enabled")
    }
    
    /**
     * Enable video (for video calls)
     */
    fun enableVideo() {
        rtcEngine?.enableVideo()
        Log.d(TAG, "Video enabled")
    }
    
    /**
     * Disable video
     */
    fun disableVideo() {
        rtcEngine?.disableVideo()
        Log.d(TAG, "Video disabled")
    }
    
    /**
     * Switch camera (front/back)
     */
    fun switchCamera() {
        rtcEngine?.switchCamera()
        Log.d(TAG, "Camera switched")
    }
    
    /**
     * Mute remote user
     */
    fun muteRemoteAudio(uid: Int, muted: Boolean) {
        rtcEngine?.muteRemoteAudioStream(uid, muted)
        Log.d(TAG, "Remote user $uid audio muted: $muted")
    }
    
    /**
     * Destroy RTC Engine
     * Call this when app is closing or engine no longer needed
     */
    fun destroy() {
        try {
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
            rtcEngine = null
            isInitialized = false
            Log.d(TAG, "Agora RTC Engine destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying engine: ${e.message}", e)
        }
    }
    
    /**
     * Get SDK version
     */
    fun getSdkVersion(): String {
        return RtcEngine.getSdkVersion()
    }
    
    /**
     * Check if initialized
     */
    fun isInitialized(): Boolean = isInitialized
}
