package com.example.firstapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.io.InputStream
import android.util.Log // Added for logging

class MainActivity16 : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadImageToFirebase(uri)
        } else {
            Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main16)

        // Initialize Firebase services
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<TextView>(R.id.text1).setOnClickListener {
            startActivity(Intent(this, MainActivity5::class.java))
            finish()
        }

        findViewById<TextView>(R.id.text2_2).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

        try {
            val imageBase64 = uriToBase64(uri)

            // --- FIX 1: Generate the unique key FIRST ---
            val imageRef = database.getReference("images").push()
            val postKey = imageRef.key

            if (postKey.isNullOrBlank()) {
                Toast.makeText(this, "Upload failed: Could not generate key.", Toast.LENGTH_LONG).show()
                return
            }

            // --- FIX 2: Include the unique postKey in the data map ---
            val imageData = mapOf(
                "postId" to postKey, // <-- CRITICAL: Now the post object contains its unique ID
                "base64Image" to imageBase64,
                "timestamp" to System.currentTimeMillis(),
                "userId" to currentUserId
            )

            imageRef.setValue(imageData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Image uploaded successfully! 🎉", Toast.LENGTH_LONG).show()
                    Log.d("Upload", "Post uploaded with unique ID: $postKey")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to process image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun uriToBase64(uri: Uri): String {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val byteArrayOutputStream = ByteArrayOutputStream()
        inputStream?.use { input ->
            input.copyTo(byteArrayOutputStream)
        }
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
