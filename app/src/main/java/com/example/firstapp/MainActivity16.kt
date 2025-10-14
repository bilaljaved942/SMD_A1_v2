package com.example.firstapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity16 : AppCompatActivity() {

    // Activity Result Launcher for selecting an image OR video from the gallery
    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        // Note: The intentType flag for POST_UPLOAD is ignored here to keep the story flow simple

        if (uri != null) {
            val mimeType = contentResolver.getType(uri)
            val isVideo = mimeType?.startsWith("video/") == true

            // Send media to the Story Preview/Encoding screen
            val intent = Intent(this, MainActivity19::class.java).apply {
                putExtra("MEDIA_URI", uri.toString())
                putExtra("IS_VIDEO", isVideo) // Passes the media type
            }
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Media selection cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main16)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Assuming this is the main trigger area to open the gallery/media picker
        var galleryOpener = findViewById<TextView>(R.id.text1)

        galleryOpener.setOnClickListener {
            pickMediaLauncher.launch("image/*") // Launching with "image/*" for simplicity
        }

        // Note: The rest of the navigation logic (like the Cancel/Back button)
        // is assumed to be handled by other click listeners in your original code.
    }
}