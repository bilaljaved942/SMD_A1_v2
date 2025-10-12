package com.example.firstapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity2 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val TAG = "SignupActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val backArrow = findViewById<ImageView>(R.id.backArrow)
        val createAccountBtn = findViewById<TextView>(R.id.createAccountBtn)

        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)

        backArrow.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        createAccountBtn.setOnClickListener {
            Log.d(TAG, "Create Account button clicked")

            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            // Validation
            if (emailText.isEmpty() || passwordText.isEmpty()) {
                Log.w(TAG, "Email or password is empty")
                Toast.makeText(this, "Please fill email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordText.length < 6) {
                Log.w(TAG, "Password too short")
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent multiple clicks
            createAccountBtn.isEnabled = false
            Log.d(TAG, "Starting account creation...")

            Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()

            // Only create authentication account
            auth.createUserWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        Log.d(TAG, "✓ Account created successfully! UID: $userId")

                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

                        // Navigate to MainActivity4
                        Log.d(TAG, "→ Navigating to MainActivity4")
                        val intent = Intent(this@MainActivity2, MainActivity4::class.java)
                        startActivity(intent)
                        finish()
                        Log.d(TAG, "✓ Navigation complete")

                    } else {
                        val error = task.exception
                        Log.e(TAG, "✗ Account creation failed: ${error?.message}")
                        Toast.makeText(this, "Signup failed: ${error?.message}", Toast.LENGTH_LONG).show()
                        createAccountBtn.isEnabled = true
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "✗ Authentication error: ${e.message}")
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    createAccountBtn.isEnabled = true
                }
        }
    }
}