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
import com.google.firebase.database.FirebaseDatabase

class MainActivity2 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
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
        database = FirebaseDatabase.getInstance() // Initialize Database

        val backArrow = findViewById<ImageView>(R.id.backArrow)
        val createAccountBtn = findViewById<TextView>(R.id.createAccountBtn)

        val nameInput = findViewById<EditText>(R.id.etName) // Assuming ID for Name Input
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)

        backArrow.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        createAccountBtn.setOnClickListener {
            Log.d(TAG, "Create Account button clicked")

            val nameText = nameInput.text.toString().trim()
            val emailText = emailInput.text.toString().trim()
            val passwordText = passwordInput.text.toString().trim()

            // Validation
            if (nameText.isEmpty() || emailText.isEmpty() || passwordText.isEmpty()) {
                Log.w(TAG, "A field is empty")
                Toast.makeText(this, "Please fill all fields (Name, Email, Password)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordText.length < 6) {
                Log.w(TAG, "Password too short")
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createAccountBtn.isEnabled = false
            Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()

            auth.createUserWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid

                        if (userId != null) {
                            // ** CRITICAL STEP: SAVE PROFILE DATA **
                            saveUserProfileToDatabase(userId, nameText, emailText)
                        }

                        Log.d(TAG, "✓ Account created successfully! UID: $userId")
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

                        // Navigate to the next screen (Login or Home)
                        startActivity(Intent(this@MainActivity2, MainActivity4::class.java))
                        finish()
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

    /**
     * Saves the user's name to the /users/{uid} node in Realtime Database.
     */
    private fun saveUserProfileToDatabase(userId: String, name: String, email: String) {
        val userRef = database.getReference("users").child(userId)

        val profileData = mapOf(
            "name" to name,
            "email" to email,
            "createdAt" to System.currentTimeMillis()
        )

        userRef.setValue(profileData)
            .addOnSuccessListener {
                Log.d(TAG, "Profile data saved to DB successfully.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save profile data: ${e.message}")
            }
    }
}
