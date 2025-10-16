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
import java.io.ByteArrayOutputStream
import java.io.InputStream

class MainActivity16 : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase

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

        database = FirebaseDatabase.getInstance()

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
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

        try {
            val imageBase64 = uriToBase64(uri)
            val imageRef = database.getReference("images").push()

            // CRITICAL: Use "base64Image" to match the Post data class field
            val imageData = mapOf(
                "base64Image" to imageBase64,
                "timestamp" to System.currentTimeMillis()
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
        // This function efficiently reads the file and converts it to a Base64 string.
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val byteArrayOutputStream = ByteArrayOutputStream()
        inputStream?.use { input ->
            input.copyTo(byteArrayOutputStream)
        }
        val byteArray = byteArrayOutputStream.toByteArray()
        // CRITICAL: Use Base64.NO_WRAP to prevent line breaks that corrupt decoding
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}