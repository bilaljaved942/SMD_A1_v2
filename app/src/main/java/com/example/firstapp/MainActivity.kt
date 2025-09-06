package com.example.firstapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // Splash screen timeout duration in milliseconds (3 seconds = 3000ms)
    private companion object {
        const val SPLASH_TIMEOUT = 3000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handler to delay the transition to MainActivity2
        Handler(Looper.getMainLooper()).postDelayed({
            // Intent to start the second activity
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)

            // Close the splash activity so user can't go back to it
            finish()
        }, SPLASH_TIMEOUT)
    }
}