package com.example.firstapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ImageView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Fetching image from XML
        val logoImageView = findViewById<ImageView>(R.id.imageView3)

        // Optional: manual click to skip splash
        logoImageView.setOnClickListener {
            moveToNextActivity()
        }

        // Auto move after 5 seconds (5000 ms)
        Handler(Looper.getMainLooper()).postDelayed({
            moveToNextActivity()
        }, 5000)
    }

    private fun moveToNextActivity() {
        val intent = Intent(this, MainActivity3::class.java)
        startActivity(intent)
        finish() // Close splash so user can’t go back to it
    }
}
