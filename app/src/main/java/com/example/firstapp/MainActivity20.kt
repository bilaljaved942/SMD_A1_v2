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

class MainActivity20 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main20)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Image that goes back to MainActivity13
        val image1 = findViewById<ImageView>(R.id.image1)
        image1.setOnClickListener {
            moveToMainActivity13()
        }

        // Image that goes to MainActivity21
        val image2 = findViewById<ImageView>(R.id.imageView1)
        image2.setOnClickListener {
            val intent = Intent(this, MainActivity21::class.java)
            startActivity(intent)
            finish() // remove from back stack
        }

        // Automatically move back after 5 seconds (like an Instagram story)
        Handler(Looper.getMainLooper()).postDelayed({
            moveToMainActivity13()
        }, 5000)
    }

    private fun moveToMainActivity13() {
        val intent = Intent(this, MainActivity13::class.java)
        startActivity(intent)
        finish() // remove this screen from back stack
    }
}
