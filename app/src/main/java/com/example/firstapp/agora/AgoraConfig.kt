package com.example.firstapp.agora

/**
 * Agora Configuration
 * 
 * IMPORTANT: Get your Agora App ID from https://console.agora.io/
 * 
 * Steps:
 * 1. Sign up at https://www.agora.io/
 * 2. Create a new project
 * 3. Copy the App ID
 * 4. Replace "YOUR_APP_ID_HERE" below
 * 
 * For production, use App ID + Token authentication
 * For development, App ID only is sufficient
 */
object AgoraConfig {
    /**
     * Your Agora App ID
     * Get it from: https://console.agora.io/projects
     */
    const val APP_ID = "754a9c9b659a45379b526a99eb2102b2"
    
    /**
     * App Certificate (optional, for token generation)
     * Get it from project settings in Agora Console
     */
    const val APP_CERTIFICATE = ""
    
    /**
     * Enable/disable logs
     */
    const val ENABLE_LOGS = true
    
    /**
     * Token expiration time (in seconds)
     * Default: 24 hours
     */
    const val TOKEN_EXPIRATION_TIME = 86400
    
    /**
     * Channel encryption settings
     */
    const val ENABLE_ENCRYPTION = false
    const val ENCRYPTION_SECRET = ""
    
    /**
     * Check if Agora is configured
     */
    fun isConfigured(): Boolean {
        return APP_ID != "YOUR_APP_ID_HERE" && APP_ID.isNotBlank()
    }
    
    /**
     * Get configuration status message
     */
    fun getConfigurationMessage(): String {
        return if (isConfigured()) {
            "Agora configured with App ID: ${APP_ID.take(10)}..."
        } else {
            "Agora not configured. Please add your App ID in AgoraConfig.kt"
        }
    }
}
