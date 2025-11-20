package com.example.firstapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.firstapp.agora.AgoraConfig
import com.example.firstapp.agora.AgoraManager
import com.example.firstapp.repository.MessageRepository
import com.example.firstapp.utils.SecurePreferences
import io.agora.rtc2.IRtcEngineEventHandler
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * VoiceCallActivity - Handles voice calls using Agora RTC
 *
 * Features:
 * - Join voice channel
 * - Mute/unmute microphone
 * - Speaker on/off
 * - End call
 * - Call duration timer
 */
open class VoiceCallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VoiceCallActivity"
        private const val PERMISSION_REQUEST_CODE = 100

        // Intent extras
        const val EXTRA_CHANNEL_NAME = "channel_name"
        const val EXTRA_RECIPIENT_NAME = "recipient_name"
        const val EXTRA_RECIPIENT_ID = "recipient_id"
        const val EXTRA_IS_CALLER = "is_caller"
    }

    // UI Components
    private lateinit var recipientNameText: TextView
    private lateinit var callStatusText: TextView
    private lateinit var callDurationText: TextView
    private lateinit var muteButton: ImageButton
    private lateinit var speakerButton: ImageButton
    private lateinit var endCallButton: ImageButton

    // Data
    protected lateinit var channelName: String
    private lateinit var recipientName: String
    private lateinit var recipientId: String
    private var isCaller: Boolean = false

    private var isMuted = false
    private var isSpeakerOn = true
    private var callStartTime: Long = 0
    private var isInChannel = false


    // Repositories
    private lateinit var messageRepository: MessageRepository
    private lateinit var prefs: SecurePreferences

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.d(TAG, "Joined channel successfully: $channel")
                callStatusText.text = getString(R.string.connected)
                isInChannel = true
                callStartTime = System.currentTimeMillis()
                startCallTimer()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.d(TAG, "Remote user joined: $uid")
                callStatusText.text = getString(R.string.call_in_progress)
                Toast.makeText(this@VoiceCallActivity, "User joined", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Log.d(TAG, "Remote user left: $uid")
                Toast.makeText(this@VoiceCallActivity, "User left the call", Toast.LENGTH_SHORT).show()
                endCall()
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            runOnUiThread {
                Log.d(TAG, "Left channel")
                isInChannel = false
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                Log.e(TAG, "Agora error: $err")
                Toast.makeText(this@VoiceCallActivity, "Call error: $err", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_call)

        // Get intent data
        channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: return finish()
        recipientName = intent.getStringExtra(EXTRA_RECIPIENT_NAME) ?: "Unknown"
        recipientId = intent.getStringExtra(EXTRA_RECIPIENT_ID) ?: ""
        isCaller = intent.getBooleanExtra(EXTRA_IS_CALLER, false)

        // Initialize
        messageRepository = MessageRepository(this)
        prefs = SecurePreferences(this)

        initViews()
        checkPermissions()
    }

    private fun initViews() {
        recipientNameText = findViewById(R.id.recipientNameText)
        callStatusText = findViewById(R.id.callStatusText)
        callDurationText = findViewById(R.id.callDurationText)
        muteButton = findViewById(R.id.muteButton)
        speakerButton = findViewById(R.id.speakerButton)
        endCallButton = findViewById(R.id.endCallButton)

        recipientNameText.text = recipientName
        callStatusText.text = if (isCaller) "Calling..." else "Incoming call..."

        muteButton.setOnClickListener { toggleMute() }
        speakerButton.setOnClickListener { toggleSpeaker() }
        endCallButton.setOnClickListener { endCall() }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            startCall()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCall()
            } else {
                Toast.makeText(this, "Permissions required for voice call", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCall() {
        if (!AgoraConfig.isConfigured()) {
            Toast.makeText(this, "Agora not configured", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize Agora
        val initialized = AgoraManager.initialize(this, rtcEventHandler)
        if (!initialized) {
            Toast.makeText(this, "Failed to initialize Agora", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Join channel
        val joined = AgoraManager.joinChannel(channelName)
        if (!joined) {
            Toast.makeText(this, "Failed to join call", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Enable speaker by default
        AgoraManager.enableSpeaker(true)

        // If caller, send call invite message
        if (isCaller) {
            sendCallInvite()
        }
    }

    private fun sendCallInvite() {
        lifecycleScope.launch {
            try {
                val result = messageRepository.sendMessage(
                    receiverId = recipientId,
                    content = "Voice call",
                    mediaBase64 = null,
                    type = "call_invite",
                    callType = "voice",
                    channelName = channelName
                )

                result.onFailure { error ->
                    Log.e(TAG, "Failed to send call invite: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending invite: ${e.message}", e)
            }
        }
    }

    private fun toggleMute() {
        isMuted = !isMuted
        AgoraManager.muteLocalAudio(isMuted)

        // Use existing drawable resources
        muteButton.alpha = if (isMuted) 0.5f else 1.0f

        Toast.makeText(
            this,
            if (isMuted) "Microphone muted" else "Microphone on",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        AgoraManager.enableSpeaker(isSpeakerOn)

        // Use alpha transparency for visual feedback
        speakerButton.alpha = if (isSpeakerOn) 1.0f else 0.5f

        Toast.makeText(
            this,
            if (isSpeakerOn) "Speaker on" else "Speaker off",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun startCallTimer() {
        val runnable = object : Runnable {
            override fun run() {
                if (isInChannel) {
                    val duration = (System.currentTimeMillis() - callStartTime) / 1000
                    val minutes = duration / 60
                    val seconds = duration % 60
                    callDurationText.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
                    callDurationText.postDelayed(this, 1000)
                }
            }
        }
        callDurationText.post(runnable)
    }

    private fun endCall() {
        AgoraManager.leaveChannel()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isInChannel) {
            AgoraManager.leaveChannel()
        }
    }
}