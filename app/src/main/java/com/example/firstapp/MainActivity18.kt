package com.example.firstapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity18 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main18)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Cross icon — user can manually close before 5 seconds
        val crossIcon = findViewById<ImageView>(R.id.imageCross)
        crossIcon.setOnClickListener {
            moveToPreviousScreen()
        }

        // Automatically close after 5 seconds (like an Instagram story)
        Handler(Looper.getMainLooper()).postDelayed({
            moveToPreviousScreen()
        }, 5000)
    }

    private fun moveToPreviousScreen() {
        val intent = Intent(this, MainActivity5::class.java)
        startActivity(intent)
        finish() // remove this activity from the back stack
    }
}
