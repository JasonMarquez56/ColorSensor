package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Ensure the correct layout file is referenced here

        // Find the button by its ID
        val btnGoToHome = findViewById<Button>(R.id.guest_button)
        val btnGoToLogin = findViewById<Button>(R.id.login_button)

        // Set a click listener for the Login button
        btnGoToLogin.setOnClickListener {
            // Navigate to LoginScreen
            val intent = Intent(this, LoginScreen::class.java)
            startActivity(intent)
//            finish() // Finish SplashActivity to prevent going back
        }

        // Set a click listener for the Guest button
        btnGoToHome.setOnClickListener {
            // Navigate to HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
//            finish() // Finish SplashActivity to prevent going back
        }
    }
}
