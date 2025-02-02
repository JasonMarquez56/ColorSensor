package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class LandingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.landing_screen) // Ensure the correct layout file is referenced here

        // Find the buttons by their IDs
        val btnGoToHome = findViewById<Button>(R.id.guest_button)
        val btnGoToLogin = findViewById<Button>(R.id.login_button)
        val btnGoToRegister = findViewById<Button>(R.id.register_button)

        // Set a click listener for the Login button
        btnGoToLogin.setOnClickListener {
            // Navigate to LoginScreen
            val intent = Intent(this, LoginScreen::class.java)
            startActivity(intent)
        }

        // Set a click listener for the Guest button
        btnGoToHome.setOnClickListener {
            // Navigate to HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // Set a click listener for the Register button
        btnGoToRegister.setOnClickListener {
            // Navigate to RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // Prevents the user from going back further than the landing page
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Show a confirmation dialog before exiting
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                if (isTaskRoot) {
                    finishAffinity()
                } else {
                    super.onBackPressed()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
