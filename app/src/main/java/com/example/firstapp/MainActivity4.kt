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

class MainActivity4 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
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

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get UI elements from your XML
        val backArrow = findViewById<ImageView>(R.id.backArrow)
        val loginBtn = findViewById<TextView>(R.id.loginBtn)
        val signUpBtn = findViewById<TextView>(R.id.signUp)

        // Input fields - using your XML IDs
        val emailInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.usernameInput2)

        // Back button
        backArrow.setOnClickListener {
            val intent = Intent(this, MainActivity3::class.java)
            startActivity(intent)
            finish()
        }

        // Login button
        loginBtn.setOnClickListener {
            Log.d(TAG, "Login button clicked")

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validation
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent multiple clicks
            loginBtn.isEnabled = false
            Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Attempting login for: $email")

            // Sign in with Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        Log.d(TAG, "✓ Login successful! UID: ${user?.uid}")
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                        // Navigate to MainActivity5 (home screen or dashboard)
                        Log.d(TAG, "→ Navigating to MainActivity5")
                        val intent = Intent(this@MainActivity4, MainActivity5::class.java)
                        startActivity(intent)
                        finish()
                        Log.d(TAG, "✓ Navigation complete")

                    } else {
                        val error = task.exception
                        Log.e(TAG, "✗ Login failed: ${error?.message}")

                        // User-friendly error messages
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
                .addOnFailureListener { e ->
                    Log.e(TAG, "✗ Login error: ${e.message}")
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    loginBtn.isEnabled = true
                }
        }

        // Sign up button - navigate to signup screen (MainActivity2)
        signUpBtn.setOnClickListener {
            Log.d(TAG, "Sign up button clicked")
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: ${currentUser.uid}")
            // Optional: Auto-navigate to MainActivity5 if user is already logged in
            // Uncomment the lines below if you want this behavior
            // startActivity(Intent(this, MainActivity5::class.java))
            // finish()
        }
    }
}