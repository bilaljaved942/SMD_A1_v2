package com.example.firstapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView // ADDED: Required for the fix
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
// REMOVED: import io.agora.rtc2.Constants.ErrorCode // This line was causing the unresolved reference error

class MainActivity10 : AppCompatActivity() {

    private val appId = "d08089443f014cacb9579456912cdb6e"
    private val token = "007eJxTYGArvd4ZZ3j+3drzd1fMj1sSx7/yWZQ5X9yOXU3eHxxn2l1XYEgxsDCwsDQxMU4zMDRJTkxOsjQ1tzQxNbM0NEpOSTJL1f32MaMhkJFh6usQRkYGCATxBRiSMxJL4sszSzLic/NLMlKLGBgAcmomPA=="
    private val channelName = "chat_with_mother"


    private var agoraEngine: RtcEngine? = null
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    // Enhancement: Track the mute state reliably (instead of using queryDeviceScore())
    private var isAudioMuted = false // Added this state tracker

    private fun checkSelfPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, REQUESTED_PERMISSIONS[0]
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this, REQUESTED_PERMISSIONS[1]
                ) == PackageManager.PERMISSION_GRANTED
    }
    // END: Feature Implementation for Assignment 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main10)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- CONTROL BUTTON LOGIC ---

        // 1. CALL END (Hang Up) - Navigates to MainActivity9
        val callEndButton = findViewById<ImageView>(R.id.callEnd)
        callEndButton.setOnClickListener {
            // START: Agora Feature - Hang Up and Navigate
            leaveChannelAndNavigate()
            // END: Agora Feature
        }

        // 2. VOLUME IMAGE (Mute/Speaker Toggle)
        findViewById<ImageView>(R.id.volumeImage).setOnClickListener {
            // START: Feature Implementation for Assignment 2 - Mute/Unmute Toggle
            isAudioMuted = !isAudioMuted
            agoraEngine?.muteLocalAudioStream(isAudioMuted)

            // Optional: You could update the volumeImage icon here based on isAudioMuted

            Toast.makeText(this, if (isAudioMuted) "Audio Muted" else "Audio Unmuted", Toast.LENGTH_SHORT).show()
            // END: Feature Implementation for Assignment 2
        }

        // 3. MESSAGE (Chat/Message Toggle)
        findViewById<ImageView>(R.id.message).setOnClickListener {
            // This button could be used to switch between the full video screen and the chat interface.
            // For now, retaining the placeholder toast.
            Toast.makeText(this, "Messaging feature not fully implemented for call overlay.", Toast.LENGTH_SHORT).show()
        }

        // START: Feature Implementation for Assignment 2 - Agora Video Call Initialization
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        } else {
            setupVideoSDKEngine()
        }
        // END: Feature Implementation for Assignment 2
    }

    // START: Feature Implementation for Assignment 2 - Agora Engine and Call Logic

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine?.leaveChannel()
        RtcEngine.destroy()
        agoraEngine = null
    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine?.enableVideo()
            joinChannel()
        } catch (e: Exception) {
            Toast.makeText(this, "Agora init failed.", Toast.LENGTH_LONG).show()
        }
    }

    private fun joinChannel() {
        if (!checkSelfPermission()) return

        // Reset mute state on joining a new channel
        isAudioMuted = false
        agoraEngine?.muteLocalAudioStream(false) // Ensure audio is unmuted when joining

        val options = io.agora.rtc2.ChannelMediaOptions()
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION

        setupLocalVideo()

        agoraEngine?.joinChannel(token, channelName, 0, options)
    }

    private fun leaveChannelAndNavigate() {
        agoraEngine?.leaveChannel()
        Toast.makeText(this, "Call Ended.", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity9::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupLocalVideo() {
        val container = findViewById<FrameLayout>(R.id.local_video_container)
        // FIX 1: Using CreateRendererView alternative: creating a standard SurfaceView
        val surfaceView = SurfaceView(this@MainActivity10)
        surfaceView.setZOrderMediaOverlay(true)
        container.addView(surfaceView)

        // The SurfaceView is now bound to the Agora Engine via setupLocalVideo
        agoraEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(R.id.remote_video_container)
        container.removeAllViews()

        // FIX 1: Using CreateRendererView alternative: creating a standard SurfaceView
        val surfaceView = SurfaceView(this@MainActivity10)
        container.addView(surfaceView)

        // The SurfaceView is now bound to the Agora Engine via setupRemoteVideo
        agoraEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    // --- Agora Event Handler (Handles remote user joining/leaving) ---
    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                setupRemoteVideo(uid)
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            runOnUiThread { Toast.makeText(this@MainActivity10, "Connected to $channelName", Toast.LENGTH_SHORT).show() }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity10, "Remote user hung up.", Toast.LENGTH_SHORT).show()
                leaveChannelAndNavigate()
            }
        }

        // FIX 2: Updated method signature for onError to match the newer Agora SDK version (4.x)
        override fun onError(err: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity10, "Agora Error: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Permission Handling ---
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                setupVideoSDKEngine()
            } else {
                Toast.makeText(this, "Camera and audio permissions are required for calling.", Toast.LENGTH_LONG).show()
                leaveChannelAndNavigate()
            }
        }
    }
    // END: Feature Implementation for Assignment 2
}