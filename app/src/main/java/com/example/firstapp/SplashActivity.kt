package com.example.firstapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ImageView
import com.example.firstapp.utils.SecurePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    private lateinit var securePrefs: SecurePreferences
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        securePrefs = SecurePreferences(this)

        // Fetching image from XML
        val logoImageView = findViewById<ImageView>(R.id.imageView3)

        // Optional: manual click to skip splash
        logoImageView.setOnClickListener {
            checkSessionAndNavigate()
        }

        // Auto check session after 5 seconds (5000 ms)
        Handler(Looper.getMainLooper()).postDelayed({
            checkSessionAndNavigate()
        }, 5000)
    }

    private fun checkSessionAndNavigate() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val isLoggedIn = securePrefs.isLoggedIn()
                val userId = securePrefs.getUserId()
                val authToken = securePrefs.getAuthToken()
                
                Log.d(TAG, "Session check: isLoggedIn=$isLoggedIn, userId=$userId")
                
                withContext(Dispatchers.Main) {
                    when {
                        isLoggedIn && !userId.isNullOrEmpty() && !authToken.isNullOrEmpty() -> {
                            // User is logged in, go to home/feed
                            Log.d(TAG, "User is logged in, navigating to MainActivity5 (Home)")
                            val intent = Intent(this@SplashActivity, MainActivity5::class.java)
                            startActivity(intent)
                            finish()
                        }
                        securePrefs.isFirstTime() -> {
                            // First time user, go to profile setup or welcome screen
                            Log.d(TAG, "First time user, navigating to MainActivity3 (Welcome)")
                            val intent = Intent(this@SplashActivity, MainActivity3::class.java)
                            startActivity(intent)
                            finish()
                        }
                        else -> {
                            // Not logged in, go to login/signup screen
                            Log.d(TAG, "User not logged in, navigating to MainActivity3 (Login/Signup)")
                            val intent = Intent(this@SplashActivity, MainActivity3::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking session: ${e.message}")
                // On error, navigate to login screen
                val intent = Intent(this@SplashActivity, MainActivity3::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}
