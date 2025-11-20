package com.example.firstapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.firstapp.MainActivity5
import com.example.firstapp.R
import com.example.firstapp.repository.AuthRepository
import com.example.firstapp.utils.NetworkUtils
import com.example.firstapp.utils.SecurePreferences
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var prefs: SecurePreferences
    
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: TextView
    private lateinit var signUpText: TextView
    private lateinit var forgotPasswordText: TextView
    private lateinit var backArrow: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main4)
        
        // Initialize repository and preferences
        authRepository = AuthRepository(this)
        prefs = SecurePreferences(this)
        
        // Check if already logged in
        if (prefs.isLoggedIn()) {
            navigateToHome()
            return
        }
        
        // Initialize views
        initViews()
        
        // Set up click listeners
        setupClickListeners()
    }

    private fun initViews() {
        emailInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.usernameInput2)
        loginBtn = findViewById(R.id.loginBtn)
        signUpText = findViewById(R.id.signUp)
        forgotPasswordText = findViewById(R.id.forgotPassword)
        backArrow = findViewById(R.id.backArrow)
        
        // Set hints
        emailInput.hint = "Email"
        passwordInput.hint = "Password"
        passwordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or 
                                   android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun setupClickListeners() {
        loginBtn.setOnClickListener {
            attemptLogin()
        }
        
        signUpText.setOnClickListener {
            // Navigate to SignupActivity
            val intent = Intent(this, com.example.firstapp.ui.auth.SignupActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        forgotPasswordText.setOnClickListener {
            Toast.makeText(this, "Password recovery not implemented yet", Toast.LENGTH_SHORT).show()
            // TODO: Implement forgot password functionality
        }
        
        backArrow.setOnClickListener {
            finish()
        }
    }

    private fun attemptLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        
        // Validate inputs
        if (!validateInputs(email, password)) {
            return
        }
        
        // Check network connectivity
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Disable button to prevent double clicks
        loginBtn.isEnabled = false
        loginBtn.text = "Logging in..."
        
        // Perform login
        lifecycleScope.launch {
            val result = authRepository.login(email, password)
            
            result.onSuccess { authResponse ->
                // Login successful
                Toast.makeText(
                    this@LoginActivity,
                    "Welcome ${authResponse.user?.name ?: ""}!",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Navigate to home screen
                navigateToHome()
            }
            
            result.onFailure { exception ->
                // Remote login failed - attempt offline fallback using stored credentials
                val storedEmail = prefs.getUserEmail()
                val storedPassword = prefs.getUserPassword() // Plain for dev only
                if (storedEmail != null && storedPassword != null &&
                    storedEmail.equals(email, ignoreCase = true) && storedPassword == password) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Offline login successful (cached)",
                        Toast.LENGTH_SHORT
                    ).show()
                    prefs.setLoggedIn(true)
                    navigateToHome()
                } else {
                    // Login failed
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    // Re-enable button
                    loginBtn.isEnabled = true
                    loginBtn.text = "Login"
                }
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        // Validate email
        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            emailInput.requestFocus()
            return false
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Please enter a valid email"
            emailInput.requestFocus()
            return false
        }
        
        // Validate password
        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            passwordInput.requestFocus()
            return false
        }
        
        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            passwordInput.requestFocus()
            return false
        }
        
        return true
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity5::class.java)
        startActivity(intent)
        finish() // Prevent going back to login screen
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
