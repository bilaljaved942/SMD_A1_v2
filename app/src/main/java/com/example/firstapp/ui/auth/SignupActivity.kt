package com.example.firstapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.firstapp.MainActivity5
import com.example.firstapp.R
import com.example.firstapp.data.local.SociallyDatabase
import com.example.firstapp.data.local.entities.UserEntity
import com.example.firstapp.data.remote.RetrofitClient
import com.example.firstapp.data.remote.models.SignupRequest
import com.example.firstapp.utils.ImageUtils
import com.example.firstapp.ui.auth.LoginActivity
import com.example.firstapp.utils.NetworkUtils
import com.example.firstapp.utils.SecurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupActivity : AppCompatActivity() {

    private lateinit var securePrefs: SecurePreferences
    private lateinit var database: SociallyDatabase
    private val TAG = "SignupActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        securePrefs = SecurePreferences(this)
        database = SociallyDatabase.getDatabase(this)

        val backArrow = findViewById<ImageView>(R.id.backArrow)
        val createAccountBtn = findViewById<TextView>(R.id.createAccountBtn)
        val nameInput = findViewById<EditText>(R.id.etName)
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)

        backArrow.setOnClickListener {
            finish()
        }

        createAccountBtn.setOnClickListener {
            Log.d(TAG, "Create Account button clicked")

            val nameText = nameInput.text.toString().trim()
            val emailText = emailInput.text.toString().trim()
            val passwordText = passwordInput.text.toString().trim()

            // Validation
            if (nameText.isEmpty() || emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordText.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createAccountBtn.isEnabled = false
            performSignup(nameText, emailText, passwordText, createAccountBtn)
        }
    }

    private fun performSignup(name: String, email: String, password: String, button: TextView) {
        lifecycleScope.launch {
            try {
                if (!NetworkUtils.isNetworkAvailable(this@SignupActivity)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SignupActivity,
                            "No internet connection",
                            Toast.LENGTH_SHORT
                        ).show()
                        button.isEnabled = true
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SignupActivity, "Creating account...", Toast.LENGTH_SHORT).show()
                }

                val signupRequest = SignupRequest(name, email, password)
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.authApi.signup(signupRequest)
                }

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val authResponse = response.body()!!
                        val user = authResponse.user
                        val token = authResponse.token

                        if (user != null && token != null) {
                            // Save session
                            // Save session AND password for offline validation
                            securePrefs.saveUserSession(user.uid, user.email, user.name, token, password)
                            securePrefs.saveUserPassword(password)

                            // Save user to local database
                            lifecycleScope.launch(Dispatchers.IO) {
                                database.userDao().insertUser(
                                    UserEntity(
                                        uid = user.uid.toIntOrNull() ?: 0,
                                        name = user.name,
                                        email = user.email,
                                        profilePictureBase64 = user.profilePicture,
                                        online = true
                                    )
                                )
                            }

                            Log.d(TAG, "Account created successfully! UID: ${user.uid}")
                            Toast.makeText(
                                this@SignupActivity,
                                "Account created successfully!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Navigate to LoginActivity after successful signup
                            val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@SignupActivity,
                                "Signup failed: Invalid response",
                                Toast.LENGTH_LONG
                            ).show()
                            button.isEnabled = true
                        }
                    } else {
                        val errorMsg = response.body()?.message ?: response.message()
                        Log.e(TAG, "Signup failed: $errorMsg")
                        Toast.makeText(
                            this@SignupActivity,
                            "Signup failed: $errorMsg",
                            Toast.LENGTH_LONG
                        ).show()
                        button.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Signup error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SignupActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    button.isEnabled = true
                }
            }
        }
    }
}
