package com.example.firstapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.io.InputStream

class MainActivity15 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var profileImageView: CircleImageView
    private var selectedImageUri: Uri? = null

    // Launcher to handle the result from the gallery image picker
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                selectedImageUri = imageUri
                profileImageView.setImageURI(imageUri)
                Toast.makeText(this, "New photo selected. Click 'Done' to save.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main15)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        val cancelButton = findViewById<TextView>(R.id.text1)
        val doneButton = findViewById<TextView>(R.id.text3)
        val changePhotoText = findViewById<TextView>(R.id.text2_1)
        // CRITICAL: ID for the profile image in activity_main15.xml is 'image'
        profileImageView = findViewById(R.id.image)

        // 1. Load current profile photo on startup
        loadProfilePicture()

        // 2. Click listener for "Change Profile Photo"
        changePhotoText.setOnClickListener {
            openGallery()
        }

        // 3. Click listener for "Cancel"
        cancelButton.setOnClickListener {
            // Simply finish the activity, no result needed
            finish()
        }

        // 4. Click listener for "Done"
        doneButton.setOnClickListener {
            if (selectedImageUri != null) {
                uploadProfilePicture(selectedImageUri!!)
            } else {
                // If only other profile details (name, bio, etc.) were edited, save those
                // ... (your existing logic for saving other fields goes here)
                setResult(RESULT_OK) // Indicate success even if only other fields were saved
                finish()
            }
        }
    }

    // Function to open the device gallery
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    // Function to convert URI to Base64 string and save to Firebase
    private fun uploadProfilePicture(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return

        try {
            // Get InputStream from URI
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()

            if (bytes == null) {
                Toast.makeText(this, "Failed to read image data.", Toast.LENGTH_SHORT).show()
                return
            }

            // Convert to Base64 string
            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)

            // Save to Firebase Realtime Database
            val userRef = database.getReference("users").child(userId)
            userRef.child("profilePicture").setValue(base64Image)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile photo updated successfully!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK) // Send success back to MainActivity13
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update photo: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("MainActivity15", "Error uploading picture", e)
                }

        } catch (e: Exception) {
            Log.e("MainActivity15", "Error converting image to Base64", e)
            Toast.makeText(this, "Error processing image.", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to load the current profile picture (similar to MainActivity13)
    private fun loadProfilePicture() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId)

        userRef.child("profilePicture").get().addOnSuccessListener { snapshot ->
            val base64Pic = snapshot.getValue(String::class.java)
            if (base64Pic != null && base64Pic.isNotBlank()) {
                try {
                    val imageBytes = Base64.decode(base64Pic, Base64.NO_WRAP)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    profileImageView.setImageBitmap(bitmap)
                } catch (e: IllegalArgumentException) {
                    Log.e("MainActivity15", "Invalid Base64 for profile picture: ${e.message}")
                    profileImageView.setImageResource(R.drawable.person2) // Default image
                }
            } else {
                profileImageView.setImageResource(R.drawable.person2) // Default image
            }
        }.addOnFailureListener {
            Log.e("MainActivity15", "Failed to read profile picture.", it)
            profileImageView.setImageResource(R.drawable.person2) // Default image
        }
    }
}