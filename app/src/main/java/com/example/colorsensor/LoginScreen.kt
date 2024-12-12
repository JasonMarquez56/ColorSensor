package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginScreen : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Find views
        val emailField = findViewById<EditText>(R.id.editTextTextEmailAddress)
        val passwordField = findViewById<EditText>(R.id.editTextTextPassword)
        val loginButton = findViewById<Button>(R.id.login_button2)
        val registerLink = findViewById<TextView>(R.id.textView5)

        // Handle Login button click
        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            // Validate fields
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Query Firestore for the user
            firestore.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Invalid credentials
                        Toast.makeText(
                            this,
                            "Invalid email or password. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Successful login, store user data
                        for (document in documents) {
                            val username = document.getString("username")

                            // Save username to SharedPreferences
                            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putString("username", username)
                                apply()
                            }

                            Toast.makeText(this, "Welcome back, $username!", Toast.LENGTH_SHORT).show()

                            // Navigate to HomeActivity
                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                            finish() // Close LoginScreen
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
        }

        // Handle Register link click
        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
