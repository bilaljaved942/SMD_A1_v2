package com.example.firstapp

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity5 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // clearing the stack because the user gets activity after account login, so remove those
        if (intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK == 0) {
            val restartIntent = Intent(this, MainActivity5::class.java)
            restartIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(restartIntent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main5)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Click on an ImageView → go to MainActivity6
        findViewById<ImageView>(R.id.homeIcon2).setOnClickListener {
            val intent = Intent(this, MainActivity6::class.java)
            startActivity(intent)
        }

        // Click on an ImageView → go to MainActivity11
        findViewById<ImageView>(R.id.homeIcon4).setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            startActivity(intent)
        }

        // Click on an ImageView → go to MainActivity8
        findViewById<ImageView>(R.id.forward).setOnClickListener {
            val intent = Intent(this, MainActivity8::class.java)
            startActivity(intent)
        }

        //opening the story
        var frame1=findViewById<FrameLayout>(R.id.frame1)
        frame1.setOnClickListener {
            val intent = Intent(this, MainActivity18::class.java)
            startActivity(intent)
        }

        // going to profile screen
        val profileImage = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImage3)
        profileImage.setOnClickListener {
            val intent = Intent(this, MainActivity13::class.java)
            startActivity(intent)
        }
    }
}
