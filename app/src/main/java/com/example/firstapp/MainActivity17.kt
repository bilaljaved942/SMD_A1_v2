package com.example.firstapp

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.FrameLayout // Import FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.*

class MainActivity17 : AppCompatActivity() {

    private var imageUri: Uri? = null

    // Launcher for taking the picture and saving it to imageUri
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            if (imageUri != null) {
                // Photo saved successfully, proceed to the preview screen
                val intent = Intent(this, MainActivity19::class.java).apply {
                    putExtra("MEDIA_URI", imageUri.toString())
                }
                startActivity(intent)
                finish()
            }
        } else {
            // User cancelled or capture failed. Clean up the temporary URI entry.
            imageUri?.let { contentResolver.delete(it, null, null) }
            showToast("Photo capture cancelled or failed.")
        }
    }

    // Launcher for requesting Camera Permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            showToast("Camera permission is required.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main17)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- CLICK LISTENERS FOR PAGE 17 UI ---

        // 1. Shutter/Capture Button Click Listener (Center button)
        // Now correctly references the FrameLayout ID: capture_button
        val shutterButton = findViewById<FrameLayout>(R.id.capture_button)
        shutterButton.setOnClickListener {
            // Start the process: check permission, then launch the camera app
            checkPermissionsAndLaunchCamera()
        }

        // 2. Gallery Button Click Listener (Bottom-left icon)
        // This button launches the gallery selection screen
        findViewById<ImageView>(R.id.image1).setOnClickListener {
            startActivity(Intent(this, MainActivity5::class.java))
            finish()
        }
    }

    // Function to handle permission check before launching the camera
    private fun checkPermissionsAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, proceed to launch camera
            launchCamera()
        } else {
            // Request permission
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        imageUri = createImageFileUri()
        imageUri?.let { uri ->
            // Launch the external camera application
            takePictureLauncher.launch(uri)
        } ?: showToast("Error preparing camera file.")
    }

    private fun createImageFileUri(): Uri? {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val imageFileName = "JPEG_STORY_${formatter.format(Date())}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SociallyStories")
            }
        }

        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}