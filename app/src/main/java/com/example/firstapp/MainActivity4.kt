package com.example.firstapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity4 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase // Added database reference
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main4)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance() // Initialize the database

        // Get UI elements
        val backArrow = findViewById<ImageView>(R.id.backArrow)
        val loginBtn = findViewById<TextView>(R.id.loginBtn)
        val signUpBtn = findViewById<TextView>(R.id.signUp)
        val emailInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.usernameInput2)

        backArrow.setOnClickListener {
            val intent = Intent(this, MainActivity3::class.java)
            startActivity(intent)
            finish()
        }

        loginBtn.setOnClickListener {
            Log.d(TAG, "Login button clicked")

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginBtn.isEnabled = false
            Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            Log.d(TAG, "✓ Login successful! UID: ${user.uid}")

                            // --- THIS IS THE CRITICAL FIX ---
                            // On successful login, forcefully update the user's status in the database.
                            val userStatusRef = database.getReference("users").child(user.uid)
                            val statusUpdate = mapOf<String, Any>(
                                "online" to true
                            )
                            userStatusRef.updateChildren(statusUpdate).addOnCompleteListener { statusTask ->
                                // This ensures that we navigate only after the status update is initiated.
                                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                                Log.d(TAG, "→ Navigating to MainActivity5")
                                val intent = Intent(this@MainActivity4, MainActivity5::class.java)
                                startActivity(intent)
                                finish()
                                Log.d(TAG, "✓ Navigation complete")
                            }
                            // --- END OF FIX ---

                        } else {
                            // This case is unlikely but good to handle
                            Toast.makeText(this, "Login succeeded but user data is null.", Toast.LENGTH_LONG).show()
                            loginBtn.isEnabled = true
                        }
                    } else {
                        val error = task.exception
                        Log.e(TAG, "✗ Login failed: ${error?.message}")

                        val errorMessage = when {
                            error?.message?.contains("password") == true -> "Incorrect password"
                            error?.message?.contains("no user record") == true -> "No account found with this email"
                            error?.message?.contains("badly formatted") == true -> "Invalid email format"
                            error?.message?.contains("network") == true -> "Network error. Check your internet"
                            else -> "Login failed: ${error?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        loginBtn.isEnabled = true
                    }
                }
        }

        signUpBtn.setOnClickListener {
            Log.d(TAG, "Sign up button clicked")
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: ${currentUser.uid}")
        }
    }
}