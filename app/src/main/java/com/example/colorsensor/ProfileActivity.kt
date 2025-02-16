package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.graphics.Color
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.LinearLayout
import android.widget.Toast

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_screen)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get username from SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        var username = sharedPreferences.getString("username", "Guest")

        // Find views
        val profileUsername = findViewById<TextView>(R.id.profileUsername)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val favColorContainer: LinearLayout = findViewById(R.id.colors)

        firestore.collection("users")
            .whereEqualTo("username", username)  // Query by username
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
//                        Retrieve the user's favorite colors
                        val favColors = document.get("favoriteColors") as? MutableList<String> ?: mutableListOf()

                        for (color in favColors){
                            val textView = TextView(this)
                            textView.text = color
                            textView.textSize = 20f
                            textView.setTextColor(Color.BLACK)
                            textView.setPadding(16, 8, 16, 8)

                            // Add to LinearLayout
                            favColorContainer.addView(textView)
                        }
                    }
                } else {
                    Log.d("Firestore", "No user found with username: $username")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents: ", exception)
            }


        // Set the username
        profileUsername.text = username

        // Handle Logout button click
        logoutButton.setOnClickListener {
            // Log out from Firebase Auth
            auth.signOut()

            // Clear stored session data
            sharedPreferences.edit().clear().apply()

            // Redirect to LoginActivity
            val intent = Intent(this, LandingActivity::class.java)
            startActivity(intent)
            finish() // Close ProfileActivity
        }

        // Handle Back button click
        backButton.setOnClickListener {
            finish() // Go back to the previous activity
        }
    }
}
