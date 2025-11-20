package com.example.firstapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.firstapp.agora.AgoraManager
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas

/**
 * VideoCallActivity - Extends VoiceCallActivity to add video functionality
 * 
 * Additional features:
 * - Local video preview
 * - Remote video display
 * - Camera on/off
 * - Switch camera (front/back)
 */
class VideoCallActivity : VoiceCallActivity() {

    companion object {
        private const val TAG = "VideoCallActivity"
    }

    // UI Components
    private lateinit var localVideoContainer: FrameLayout
    private lateinit var remoteVideoContainer: FrameLayout
    private lateinit var cameraButton: ImageButton
    private lateinit var switchCameraButton: ImageButton

    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null
    private var isCameraOn = true
    private var remoteUid: Int = 0

    private val videoEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.d(TAG, "Joined video channel: $channel")
                setupLocalVideo()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.d(TAG, "Remote user joined: $uid")
                remoteUid = uid
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Log.d(TAG, "Remote user left: $uid")
                if (uid == remoteUid) {
                    removeRemoteVideo()
                }
            }
        }

        override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
            runOnUiThread {
                Log.d(TAG, "First remote video decoded: $uid")
                setupRemoteVideo(uid)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Note: This uses the same layout as voice call for now
        // Create a proper video_call layout with local and remote video containers
        
        // Override with video-specific layout if available
        // setContentView(R.layout.activity_video_call)
        
        initVideoViews()
        checkVideoPermissions()
    }

    private fun initVideoViews() {
        // These views should be in your video call layout
        localVideoContainer = findViewById(R.id.localVideoContainer)
        remoteVideoContainer = findViewById(R.id.remoteVideoContainer)
        cameraButton = findViewById(R.id.cameraButton)
        switchCameraButton = findViewById(R.id.switchCameraButton)

        cameraButton.setOnClickListener { toggleCamera() }
        switchCameraButton.setOnClickListener { switchCamera() }
    }

    private fun checkVideoPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
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
                101
            )
        } else {
            startVideoCall()
        }
    }

    private fun startVideoCall() {
        // Initialize Agora with video event handler
        val initialized = AgoraManager.initialize(this, videoEventHandler)
        if (!initialized) {
            Toast.makeText(this, "Failed to initialize video call", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Enable video
        AgoraManager.enableVideo()

        // Join channel
        val joined = AgoraManager.joinChannel(channelName)
        if (!joined) {
            Toast.makeText(this, "Failed to join video call", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup local video
        setupLocalVideo()
    }

    private fun setupLocalVideo() {
        val engine = AgoraManager.getEngine() ?: return

        // Create local surface view
        localSurfaceView = SurfaceView(baseContext)
        localSurfaceView?.setZOrderMediaOverlay(true)
        localVideoContainer.addView(localSurfaceView)

        // Setup local video
        val videoCanvas = VideoCanvas(localSurfaceView, Constants.RENDER_MODE_HIDDEN, 0)
        engine.setupLocalVideo(videoCanvas)
        engine.startPreview()

        Log.d(TAG, "Local video setup complete")
    }

    private fun setupRemoteVideo(uid: Int) {
        val engine = AgoraManager.getEngine() ?: return

        // Remove existing remote view if any
        removeRemoteVideo()

        // Create remote surface view
        remoteSurfaceView = SurfaceView(baseContext)
        remoteVideoContainer.addView(remoteSurfaceView)

        // Setup remote video
        val videoCanvas = VideoCanvas(remoteSurfaceView, Constants.RENDER_MODE_HIDDEN, uid)
        engine.setupRemoteVideo(videoCanvas)

        Log.d(TAG, "Remote video setup complete for uid: $uid")
    }

    private fun removeRemoteVideo() {
        remoteSurfaceView?.let { view ->
            remoteVideoContainer.removeView(view)
            remoteSurfaceView = null
        }
    }

    private fun toggleCamera() {
        isCameraOn = !isCameraOn
        val engine = AgoraManager.getEngine() ?: return

        if (isCameraOn) {
            engine.enableLocalVideo(true)
            cameraButton.setImageResource(R.drawable.camera_on)
            Toast.makeText(this, "Camera on", Toast.LENGTH_SHORT).show()
        } else {
            engine.enableLocalVideo(false)
            cameraButton.setImageResource(R.drawable.camera_off)
            Toast.makeText(this, "Camera off", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchCamera() {
        AgoraManager.switchCamera()
        Toast.makeText(this, "Camera switched", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Stop video preview
        val engine = AgoraManager.getEngine()
        engine?.stopPreview()
        engine?.disableVideo()

        // Remove views
        localSurfaceView?.let { localVideoContainer.removeView(it) }
        remoteSurfaceView?.let { remoteVideoContainer.removeView(it) }
    }
}
