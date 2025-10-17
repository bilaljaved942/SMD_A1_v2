package com.example.firstapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
// FIX: We rely on the single definition of 'Story' from the project package,
// eliminating the redeclaration error.

class MainActivity19 : AppCompatActivity() {

    private var mediaUri: Uri? = null
    private var isVideo: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main19)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val previewImageView = findViewById<ImageView>(R.id.preview_image_view)

        // 1. Get media URI and IS_VIDEO flag
        val uriString = intent.getStringExtra("MEDIA_URI")
        mediaUri = uriString?.let { Uri.parse(it) }
        isVideo = intent.getBooleanExtra("IS_VIDEO", false)

        // 2. Display the media preview
        if (mediaUri != null) {
            Glide.with(this)
                .load(mediaUri)
                .centerCrop()
                .into(previewImageView)
        } else {
            Toast.makeText(this, "Error: No media selected.", Toast.LENGTH_LONG).show()
        }

        val shareToStoryButton = findViewById<ImageView>(R.id.your_stories_icon)
        shareToStoryButton.setOnClickListener {
            val currentFirebaseUser = FirebaseAuth.getInstance().currentUser
            if (currentFirebaseUser == null) { Toast.makeText(this, "Please log in.", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            if (mediaUri != null) {
                val base64String = encodeMediaToBase64(mediaUri!!, isVideo)

                if (base64String != null) {
                    saveStoryToDatabase(base64String, currentFirebaseUser.uid, isVideo)
                } else {
                    val failureMsg = if (isVideo) "Video file is too large for Base64." else "Failed to encode image."
                    Toast.makeText(this, failureMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
        // ... (Existing navigation/cleanup) ...
    }

    private fun encodeMediaToBase64(uri: Uri, isVideo: Boolean): String? {
        // Encoding logic (unchanged)
        if (isVideo) {
            // Video (warning: large files will fail)
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val byteArray = inputStream.readBytes()
                    // CRITICAL: Ensure NO_WRAP is used consistently, although DEFAULT is often fine here.
                    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
                }
            } catch (e: Exception) { return null }
            return null
        } else {
            // Image
            val bitmap: Bitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
            } catch (e: Exception) { return null }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        }
    }

    private fun saveStoryToDatabase(base64String: String, userId: String, isVideo: Boolean) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("stories")
        val storyId = databaseRef.push().key ?: return

        // This line now correctly uses the Story class defined elsewhere in the package.
        val story = Story(
            id = storyId,
            userId = userId,
            imageUrl = base64String,
            timestamp = System.currentTimeMillis(),
            isVideo = isVideo
        )

        databaseRef.child(storyId).setValue(story)
            .addOnSuccessListener {
                Toast.makeText(this, "Story shared successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity5::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Database error: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
