package com.example.firstapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firstapp.data.local.entities.MessageEntity
import com.example.firstapp.repository.MessageRepository
import com.example.firstapp.repository.UserRepository
import com.example.firstapp.utils.SecurePreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity9 : AppCompatActivity() {

    private lateinit var messageRepository: MessageRepository
    private lateinit var userRepository: UserRepository
    private lateinit var securePreferences: SecurePreferences

    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var galleryButton: ImageView
    private lateinit var voiceCallButton: ImageView
    private lateinit var videoCallButton: ImageView
    private lateinit var backButton: ImageView
    private lateinit var recipientNameText: TextView
    private lateinit var recipientStatusText: TextView

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    private lateinit var RECIPIENT_USER_ID: String
    private lateinit var RECIPIENT_NAME: String
    private var CURRENT_USER_ID: Int = -1

    private var messageBeingEdited: Message? = null
    private val EDIT_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(5)

    private val pollingHandler = Handler(Looper.getMainLooper())
    private var isPolling = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            sendImage(uri)
        } else {
            Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    )
    private val PERMISSION_REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main9)

        // Initialize repositories and preferences
        messageRepository = MessageRepository(this)
        userRepository = UserRepository(this)
        securePreferences = SecurePreferences(this)

        // Get current user ID from session
        CURRENT_USER_ID = securePreferences.getUserId()?.toIntOrNull() ?: -1
        if (CURRENT_USER_ID == -1) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Get recipient data from intent
        RECIPIENT_USER_ID = intent.getStringExtra("RECIPIENT_USER_ID") ?: run {
            Toast.makeText(this, "Error: No recipient specified.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        RECIPIENT_NAME = intent.getStringExtra("RECIPIENT_NAME") ?: "Unknown User"

        Log.d("MainActivity9", "Chat with user: $RECIPIENT_USER_ID (Current: $CURRENT_USER_ID)")

        // Initialize UI elements
        backButton = findViewById(R.id.image1)
        recipientNameText = findViewById(R.id.recipient_name_text)
        recipientStatusText = findViewById(R.id.online_status_text)
        messageInput = findViewById(R.id.searchInput3)
        sendButton = findViewById(R.id.circle21)
        galleryButton = findViewById(R.id.circle31)
        voiceCallButton = findViewById(R.id.voice_call_button)
        videoCallButton = findViewById(R.id.image2)
        messagesRecyclerView = findViewById(R.id.messages_recycler_view)

        recipientNameText.text = RECIPIENT_NAME

        messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        messageAdapter = MessageAdapter(
            messageList,
            CURRENT_USER_ID.toString(),
            ::handleMessageDeleteClick,
            ::handleMessageEditLongPress,
            ::handleJoinCallClick
        )
        messagesRecyclerView.adapter = messageAdapter

        // Click listeners
        backButton.setOnClickListener { finish() }

        sendButton.setOnClickListener {
            if (messageBeingEdited != null) {
                updateMessage()
            } else {
                sendMessage()
            }
        }

        galleryButton.setOnClickListener {
            cancelEditMode()
            pickImageLauncher.launch("image/*")
        }

        voiceCallButton.setOnClickListener {
            initiateVoiceCall()
        }

        videoCallButton.setOnClickListener {
            initiateVideoCall()
        }

        // Fetch messages using Repository with reactive Flow
        observeMessages()

        // Fetch recipient online status
        fetchRecipientStatus()

        // Start polling for new messages
        startMessagePolling()
    }

    private fun observeMessages() {
        lifecycleScope.launch {
            val result = messageRepository.fetchMessages(RECIPIENT_USER_ID)
            result.onSuccess { messages ->
                messageList.clear()
                messageList.addAll(messages.map { it.toMessage() })
                messageAdapter.notifyDataSetChanged()
                if (messageList.isNotEmpty()) {
                    messagesRecyclerView.scrollToPosition(messageList.size - 1)
                }
            }
        }
    }

    private fun MessageEntity.toMessage(): Message {
        return Message(
            id = this.id.toString(),
            senderId = this.senderId.toString(),
            receiverId = this.receiverId.toString(),
            content = this.content,
            timestamp = this.timestamp,
            mediaBase64 = this.mediaBase64,
            type = this.type,
            isEdited = this.isEdited,
            callType = this.callType,
            channelName = this.channelName
        )
    }

    private fun startMessagePolling() {
        isPolling = true
        pollingHandler.post(object : Runnable {
            override fun run() {
                if (isPolling) {
                    lifecycleScope.launch {
                        // Fetch messages from API and update cache
                        messageRepository.fetchMessages(RECIPIENT_USER_ID)
                        observeMessages()
                    }
                    pollingHandler.postDelayed(this, 3000) // Poll every 3 seconds
                }
            }
        })
    }

    private fun stopMessagePolling() {
        isPolling = false
        pollingHandler.removeCallbacksAndMessages(null)
    }

    private fun fetchRecipientStatus() {
        lifecycleScope.launch {
            val result = userRepository.getUserById(RECIPIENT_USER_ID)
            result.onSuccess { user ->
                updateStatusUI(user.online, user.lastOnline)
            }.onFailure {
                recipientStatusText.text = "Offline"
                recipientStatusText.setTextColor(
                    ContextCompat.getColor(this@MainActivity9, android.R.color.darker_gray)
                )
            }

            // Keep updating status every 10 seconds
            while (true) {
                delay(10000)
                val statusResult = userRepository.getUserById(RECIPIENT_USER_ID)
                statusResult.onSuccess { user ->
                    updateStatusUI(user.online, user.lastOnline)
                }
            }
        }
    }

    private fun updateStatusUI(isOnline: Boolean, lastOnline: Long?) {
        if (isOnline) {
            recipientStatusText.text = "Online"
            recipientStatusText.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            )
        } else {
            val lastSeenText = if (lastOnline != null && lastOnline > 0) {
                "Last seen: ${formatTimestamp(lastOnline)}"
            } else {
                "Offline"
            }
            recipientStatusText.text = lastSeenText
            recipientStatusText.setTextColor(
                ContextCompat.getColor(this, android.R.color.darker_gray)
            )
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun handleMessageDeleteClick(message: Message) {
        val messageTimestamp = message.timestamp
        val now = System.currentTimeMillis()
        if (now - messageTimestamp > EDIT_WINDOW_MILLIS) {
            Toast.makeText(this, "Can only delete messages within 5 minutes.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                deleteMessage(message)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleMessageEditLongPress(message: Message) {
        if (message.type != "text") {
            Toast.makeText(this, "Only text messages can be edited.", Toast.LENGTH_SHORT).show()
            return
        }

        val messageTimestamp = message.timestamp
        val now = System.currentTimeMillis()
        if (now - messageTimestamp > EDIT_WINDOW_MILLIS) {
            Toast.makeText(this, "Can only edit messages within 5 minutes.", Toast.LENGTH_SHORT).show()
            return
        }

        messageBeingEdited = message
        messageInput.setText(message.content)
        messageInput.setSelection(message.content.length)
        messageInput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT)
        Toast.makeText(this, "Editing Mode (Tap Send to update)", Toast.LENGTH_LONG).show()
    }

    private fun cancelEditMode() {
        messageBeingEdited = null
        messageInput.setText("")
        messageInput.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(messageInput.windowToken, 0)
    }

    private fun updateMessage() {
        val messageToEdit = messageBeingEdited ?: return
        val newContent = messageInput.text.toString().trim()
        if (newContent.isEmpty()) {
            Toast.makeText(this, "Edited message cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val result = messageRepository.editMessage(
                messageId = messageToEdit.id,
                newContent = newContent
            )
            result.onSuccess {
                cancelEditMode()
                Toast.makeText(this@MainActivity9, "Message updated!", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(
                    this@MainActivity9,
                    "Update failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun deleteMessage(message: Message) {
        lifecycleScope.launch {
            val result = messageRepository.deleteMessage(message.id)
            result.onSuccess {
                Toast.makeText(this@MainActivity9, "Message deleted.", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(
                    this@MainActivity9,
                    "Deletion failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun sendMessage() {
        val messageContent = messageInput.text.toString().trim()
        if (messageContent.isEmpty()) return

        lifecycleScope.launch {
            val result = messageRepository.sendMessage(
                receiverId = RECIPIENT_USER_ID,
                content = messageContent,
                type = "text"
            )
            result.onSuccess {
                messageInput.setText("")
                observeMessages()
            }.onFailure { error ->
                Log.e("MainActivity9", "Error sending message: ${error.message}")
                Toast.makeText(
                    this@MainActivity9,
                    "Failed to send: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun sendImage(uri: Uri) {
        Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val imageBase64 = uriToBase64(uri)
                val result = messageRepository.sendMessage(
                    receiverId = RECIPIENT_USER_ID,
                    content = "",
                    type = "image",
                    mediaBase64 = imageBase64
                )
                result.onSuccess {
                    Toast.makeText(this@MainActivity9, "Image sent!", Toast.LENGTH_SHORT).show()
                    observeMessages()
                }.onFailure { error ->
                    Log.e("MainActivity9", "Error sending image: ${error.message}")
                    Toast.makeText(
                        this@MainActivity9,
                        "Failed to send image: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity9", "Image processing failed: ${e.message}")
                Toast.makeText(this@MainActivity9, "Failed to process image.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uriToBase64(uri: Uri): String {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val byteArrayOutputStream = ByteArrayOutputStream()
        inputStream?.use { input -> input.copyTo(byteArrayOutputStream) }
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun initiateVoiceCall() {
        if (!checkCallPermissions()) {
            requestCallPermissions()
            return
        }

        val channelName = "voice_${CURRENT_USER_ID}_${RECIPIENT_USER_ID}_${System.currentTimeMillis()}"

        // Send call invite message
        lifecycleScope.launch {
            val result = messageRepository.sendMessage(
                receiverId = RECIPIENT_USER_ID,
                content = "Voice Call",
                type = "call_invite",
                callType = "voice",
                channelName = channelName
            )
            result.onSuccess {
                // Launch VoiceCallActivity
                val intent = Intent(this@MainActivity9, VoiceCallActivity::class.java).apply {
                    putExtra("EXTRA_CHANNEL_NAME", channelName)
                    putExtra("EXTRA_RECIPIENT_NAME", RECIPIENT_NAME)
                    putExtra("EXTRA_RECIPIENT_ID", RECIPIENT_USER_ID)
                    putExtra("EXTRA_IS_CALLER", true)
                }
                startActivity(intent)
            }.onFailure { error ->
                Toast.makeText(
                    this@MainActivity9,
                    "Failed to initiate call: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleJoinCallClick(message: Message) {
        if (!checkCallPermissions()) {
            requestCallPermissions()
            Toast.makeText(this, "Please grant permissions and try again.", Toast.LENGTH_SHORT).show()
            return
        }

        val channelName = message.channelName
        if (channelName.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid call invite.", Toast.LENGTH_SHORT).show()
            return
        }

        // Launch appropriate call activity based on call type
        val intent = if (message.callType == "video") {
            Intent(this, VideoCallActivity::class.java)
        } else {
            Intent(this, VoiceCallActivity::class.java)
        }

        intent.apply {
            putExtra("EXTRA_CHANNEL_NAME", channelName)
            putExtra("EXTRA_RECIPIENT_NAME", RECIPIENT_NAME)
            putExtra("EXTRA_RECIPIENT_ID", RECIPIENT_USER_ID)
            putExtra("EXTRA_IS_CALLER", false) // Receiver joining the call
        }
        startActivity(intent)
    }

    private fun initiateVideoCall() {
        if (!checkCallPermissions()) {
            requestCallPermissions()
            return
        }

        val channelName = "video_${CURRENT_USER_ID}_${RECIPIENT_USER_ID}_${System.currentTimeMillis()}"

        // Send call invite message
        lifecycleScope.launch {
            val result = messageRepository.sendMessage(
                receiverId = RECIPIENT_USER_ID,
                content = "Video Call",
                type = "call_invite",
                callType = "video",
                channelName = channelName
            )
            result.onSuccess {
                // Launch VideoCallActivity
                val intent = Intent(this@MainActivity9, VideoCallActivity::class.java).apply {
                    putExtra("EXTRA_CHANNEL_NAME", channelName)
                    putExtra("EXTRA_RECIPIENT_NAME", RECIPIENT_NAME)
                    putExtra("EXTRA_RECIPIENT_ID", RECIPIENT_USER_ID)
                    putExtra("EXTRA_IS_CALLER", true)
                }
                startActivity(intent)
            }.onFailure { error ->
                Toast.makeText(
                    this@MainActivity9,
                    "Failed to initiate call: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkCallPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestCallPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted. You can now make calls.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions denied. Cannot make calls.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMessagePolling()
    }
}
