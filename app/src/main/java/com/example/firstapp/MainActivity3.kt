package com.example.firstapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity3 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main3)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Sign Up button -> SignupActivity (MainActivity2 layout)
        findViewById<TextView>(R.id.signUp).setOnClickListener {
            startActivity(Intent(this, com.example.firstapp.ui.auth.SignupActivity::class.java))
        }

        // Login button -> unified LoginActivity
        findViewById<TextView>(R.id.loginBtn).setOnClickListener {
            startActivity(Intent(this, com.example.firstapp.ui.auth.LoginActivity::class.java))
        }

        // Optional: Switch account link also goes to LoginActivity
        findViewById<TextView>(R.id.switchAccount)?.setOnClickListener {
            startActivity(Intent(this, com.example.firstapp.ui.auth.LoginActivity::class.java))
        }
    }
}