package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_screen)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get username from SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "Guest")

        // Find views
        val profileUsername = findViewById<TextView>(R.id.profileUsername)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        // Set the username
        profileUsername.text = username

        // Handle Logout button click
        logoutButton.setOnClickListener {
            // Log out from Firebase Auth
            auth.signOut()

            // Clear stored session data
            sharedPreferences.edit().clear().apply()

            // Redirect to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close ProfileActivity
        }

        // Handle Back button click
        backButton.setOnClickListener {
            finish() // Go back to the previous activity
        }
    }
}
