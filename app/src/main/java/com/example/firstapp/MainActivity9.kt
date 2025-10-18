package com.example.firstapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class MainActivity9 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var galleryButton: ImageView
    private lateinit var videoCallButton: ImageView

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    // --- Core Fix State ---
    // These must be lateinit as they are set by Intent data in onCreate
    private lateinit var RECIPIENT_USER_ID: String
    private lateinit var CHAT_PATH: String
    private val CURRENT_USER_ID: String by lazy { auth.currentUser?.uid ?: "" }
    // ----------------------

    // State management
    private var messageBeingEdited: Message? = null
    private val EDIT_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(5)

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            sendImage(uri)
        } else {
            Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main9)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // --- CORE FIX: Get Recipient ID from Intent and calculate unique path ---
        RECIPIENT_USER_ID = intent.getStringExtra("RECIPIENT_USER_ID") ?: run {
            Toast.makeText(this, "Error: No recipient specified. Returning to chat list.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // This check ensures the current user is logged in before proceeding
        if (CURRENT_USER_ID.isEmpty()) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Use the function to calculate the guaranteed unique chat path
        CHAT_PATH = getUniqueChatPath(CURRENT_USER_ID, RECIPIENT_USER_ID)
        Log.d("MainActivity9", "Unique Chat Path: $CHAT_PATH")
        // -----------------------------------------------------------------------


        var back=findViewById<ImageView>(R.id.image1)
        back.setOnClickListener {
            // Changed from starting MainActivity5 to finishing, so user returns to MainActivity8 (Chat List)
            finish()
        }

        // Get View References
        messageInput = findViewById(R.id.searchInput3)
        sendButton = findViewById(R.id.circle21)
        galleryButton = findViewById(R.id.circle31)
        videoCallButton = findViewById(R.id.image2)

        messagesRecyclerView = findViewById(R.id.messages_recycler_view)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        messageAdapter = MessageAdapter(
            messageList,
            CURRENT_USER_ID,
            ::handleMessageDeleteClick,
            ::handleMessageEditLongPress
        )
        messagesRecyclerView.adapter = messageAdapter

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Core Chat Functionality ---

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


        findViewById<ImageView>(R.id.image2).setOnClickListener {
            val intent = Intent(this, MainActivity23::class.java)
            startActivity(intent)
        }

        fetchMessages()
    }

    /**
     * Calculates a unique chat path for any two user IDs by sorting them.
     * This is the core fix for message overlapping: A conversation between A and B
     * will always be stored under the same path (e.g., chats/A_B).
     */
    private fun getUniqueChatPath(user1: String, user2: String): String {
        val sortedUsers = listOf(user1, user2).sorted()
        return "chats/${sortedUsers[0]}_${sortedUsers[1]}"
    }


    // --- Message Management Functions ---

    private fun handleMessageDeleteClick(message: Message) { deleteMessage(message) }

    private fun handleMessageEditLongPress(message: Message) {
        if (message.type != "text") {
            Toast.makeText(this, "Only text messages can be edited.", Toast.LENGTH_SHORT).show()
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
        // Hide soft keyboard and clear focus
        messageInput.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(messageInput.windowToken, 0)
    }

    private fun updateMessage() {
        val messageToEdit = messageBeingEdited ?: return
        val newContent = messageInput.text.toString().trim()
        if (newContent.isEmpty()) {
            Toast.makeText(this, "Edited message cannot be empty. Delete if needed.", Toast.LENGTH_SHORT).show()
            return
        }

        // Uses the calculated CHAT_PATH
        val messageNodeRef = database.getReference(CHAT_PATH).child(messageToEdit.id)
        val updates = mapOf("content" to newContent, "isEdited" to true)
        messageNodeRef.updateChildren(updates)
            .addOnSuccessListener { cancelEditMode(); Toast.makeText(this, "Message updated!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun deleteMessage(message: Message) {
        // Uses the calculated CHAT_PATH
        database.getReference(CHAT_PATH).child(message.id).removeValue()
            .addOnSuccessListener { Toast.makeText(this, "Message deleted.", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(this, "Deletion failed: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    // --- Sending Functions ---

    private fun sendMessage() {
        val messageContent = messageInput.text.toString().trim()
        val senderId = CURRENT_USER_ID
        if (senderId.isEmpty() || messageContent.isEmpty()) return

        // Uses the calculated CHAT_PATH
        val messageRef = database.getReference(CHAT_PATH).push()
        val messageId = messageRef.key ?: return

        val message = Message(
            id = messageId,
            senderId = senderId,
            receiverId = RECIPIENT_USER_ID,
            content = messageContent,
            timestamp = System.currentTimeMillis(),
            type = "text"
        )

        messageRef.setValue(message).addOnSuccessListener {
            messageInput.setText("")
        }.addOnFailureListener { e -> Log.e("Chat", "Error sending message.", e) }
    }

    private fun sendImage(uri: Uri) {
        val senderId = CURRENT_USER_ID
        if (senderId.isEmpty()) { Toast.makeText(this, "Error: You must be logged in to send media.", Toast.LENGTH_SHORT).show(); return }
        Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show()
        try {
            val imageBase64 = uriToBase64(uri)

            // Uses the calculated CHAT_PATH
            val messageRef = database.getReference(CHAT_PATH).push()
            val messageId = messageRef.key ?: return

            val message = Message(
                id = messageId,
                senderId = senderId,
                receiverId = RECIPIENT_USER_ID,
                content = messageInput.text.toString().trim(),
                timestamp = System.currentTimeMillis(),
                mediaBase64 = imageBase64,
                type = "image"
            )

            messageRef.setValue(message).addOnSuccessListener {
                messageInput.setText("")
                Toast.makeText(this, "Image sent!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Log.e("Chat", "Error sending image.", e); Toast.makeText(this, "Failed to send image: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("Chat", "Image processing failed: ${e.message}"); Toast.makeText(this, "Failed to process image.", Toast.LENGTH_LONG).show()
        }
    }

    private fun uriToBase64(uri: Uri): String {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val byteArrayOutputStream = ByteArrayOutputStream()
        inputStream?.use { input -> input.copyTo(byteArrayOutputStream) }
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun fetchMessages() {
        // Uses the calculated CHAT_PATH
        val chatRef = database.getReference(CHAT_PATH)
        val allMessagesQuery = chatRef.orderByChild("timestamp")

        allMessagesQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)?.copy(id = messageSnapshot.key ?: "")
                    message?.let { messageList.add(it) }
                }
                messageAdapter.notifyDataSetChanged()
                if (messageList.isNotEmpty()) { messagesRecyclerView.scrollToPosition(messageList.size - 1) }
            }
            override fun onCancelled(error: DatabaseError) { Log.e("Chat", "Failed to fetch messages: ${error.message}") }
        })
    }
}
