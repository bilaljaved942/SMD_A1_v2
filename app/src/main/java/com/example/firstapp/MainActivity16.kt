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
import com.google.firebase.auth.FirebaseAuth // <-- NEW IMPORT
import java.io.ByteArrayOutputStream
import java.io.InputStream

class MainActivity16 : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth // <-- INITIALIZE FIREBASE AUTH

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
        auth = FirebaseAuth.getInstance() // <-- INITIALIZED

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
        // Get the current logged-in user's ID
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_LONG).show()
            return // Prevent posting if no user is found
        }

        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

        try {
            val imageBase64 = uriToBase64(uri)
            val imageRef = database.getReference("images").push()

            // Save post data including the crucial userId
            val imageData = mapOf(
                "base64Image" to imageBase64,
                "timestamp" to System.currentTimeMillis(),
                "userId" to currentUserId // <-- SAVING USER ID
            )

            imageRef.setValue(imageData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Image uploaded successfully! 🎉", Toast.LENGTH_LONG).show()
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