package com.example.firstapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.agora.rtc2.*

// NOTE: Please ensure you create a file named MainActivity23.kt for this code.
class MainActivity23 : AppCompatActivity() {

    // IMPORTANT: Replace the token below with your NEW, valid Agora Token
    private val appId = "d08089443f014cacb9579456912cdb6e"
    private val token = "007eJxTYLjN2zexRunx5fLbDovf7N710tT8+oHzPwLfmoR3PE7fpvNFgSHFwMLAwtLExDjNwNAkOTE5ydLU3NLE1MzS0Cg5JcksNbD6c0ZDICODoK8fEyMDBIL4AgzJGYkl8eWZJRnxufklGalFDAwABD0m9Q="
    private val channelName = "chat_with_mother" // Use your existing channel name

    private var agoraEngine: RtcEngine? = null
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA // Still needed for permission checks before switching
    )
    private var isAudioMuted = false

    // UI reference mapped to R.id.text1 location in your provided XML
    private lateinit var callStatusText: TextView
    // Assuming the camera button ID for switching video is 'camera_button'
    // as established in the previous XML modification steps.
    private lateinit var cameraButton: ImageView

    private fun checkSelfPermission(): Boolean {
        // Only require AUDIO permission for the voice call to start
        return ContextCompat.checkSelfPermission(
            this, REQUESTED_PERMISSIONS[0] // Check RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Use the XML layout for the audio interface
        setContentView(R.layout.activity_main23)

        // Map the TextView that displays the call status (R.id.text1 in your structure)
        callStatusText = findViewById(R.id.text1)

        // Map the buttons
        val callEndButton = findViewById<ImageView>(R.id.callEnd)
        val volumeButton = findViewById<ImageView>(R.id.volumeImage)
        val messageButton = findViewById<ImageView>(R.id.message)

        // Assuming you added an ImageView with ID 'camera_button' to your XML
        // It's located next to volumeImage.
        cameraButton = findViewById(R.id.message)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- BUTTON LISTENERS ---

        // 1. CALL END (Hang Up)
        callEndButton.setOnClickListener {
            leaveChannelAndNavigate()
        }

        // 2. CAMERA BUTTON (Switch to Video - MainActivity10)
        cameraButton.setOnClickListener {
            // Check Camera permission before switching
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission required to switch to video.", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQ_ID)
                return@setOnClickListener
            }

            // Leave the current audio channel
            agoraEngine?.leaveChannel()

            // Start the dedicated video call activity (MainActivity10)
            val intent = Intent(this, MainActivity10::class.java).apply {
                // Pass the channel name so MainActivity10 can rejoin the same room
                putExtra("CHANNEL_NAME", channelName)
            }
            startActivity(intent)
            finish()
        }

        // 3. VOLUME IMAGE (Mute/Speaker Toggle)
        volumeButton.setOnClickListener {
            isAudioMuted = !isAudioMuted
            agoraEngine?.muteLocalAudioStream(isAudioMuted)
            Toast.makeText(this, if (isAudioMuted) "Audio Muted" else "Audio Unmuted", Toast.LENGTH_SHORT).show()
        }

        // 4. MESSAGE Button
        messageButton.setOnClickListener {
            Toast.makeText(this, "Messaging during call...", Toast.LENGTH_SHORT).show()
        }

        // Initialize Agora Engine
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        } else {
            setupVideoSDKEngine()
        }

        findViewById<ImageView>(R.id.message).setOnClickListener {
            val intent = Intent(this, MainActivity10::class.java)
            startActivity(intent)
        }

    }

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

            // [AUDIO ONLY]: Disable video
            agoraEngine?.disableVideo()

            joinChannel()
        } catch (e: Exception) {
            Toast.makeText(this, "Agora init failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun joinChannel() {
        if (!checkSelfPermission()) return

        isAudioMuted = false
        agoraEngine?.muteLocalAudioStream(false)

        val options = io.agora.rtc2.ChannelMediaOptions()
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION // Set to Voice Call

        agoraEngine?.joinChannel(token, channelName, 0, options)
    }

    private fun leaveChannelAndNavigate() {
        agoraEngine?.leaveChannel()
        Toast.makeText(this, "Call Ended.", Toast.LENGTH_SHORT).show()

        // Navigate back to the chat screen (MainActivity9)
        val intent = Intent(this, MainActivity9::class.java)
        startActivity(intent)
        finish()
    }

    // --- Agora Event Handler ---
    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                // Update status when the remote user connects
                callStatusText.text = "Connected"
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity23, "Joined Audio Channel", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity23, "Remote user hung up.", Toast.LENGTH_SHORT).show()
                leaveChannelAndNavigate()
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity23, "Agora Error: $err (Token or Setup Issue)", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Permission Handling ---
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupVideoSDKEngine()
            } else {
                Toast.makeText(this, "Audio permission required for calling.", Toast.LENGTH_LONG).show()
                leaveChannelAndNavigate()
            }
        }
    }
}