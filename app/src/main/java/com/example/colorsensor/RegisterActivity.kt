package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_screen)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Find views
        val usernameField = findViewById<EditText>(R.id.usernameField)
        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val confirmPasswordField = findViewById<EditText>(R.id.confirmPasswordField)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginLink = findViewById<TextView>(R.id.loginLink)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        // Handle Register button click
        registerButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            // Validate fields
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$"
            val regex = Regex(passwordRegex)
            if(!regex.matches(password)){
                Toast.makeText(this, "Password must be at least 8 characters and contains one of each lowercase,Uppercase, and a number .", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Add user to Firestore
            val user = hashMapOf(
                "username" to username,
                "email" to email,
                "password" to password
            )

            firestore.collection("users")
                .add(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                    // Navigate to HomeActivity on successful registration
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish() // Close RegisterActivity
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to register. Try again.", Toast.LENGTH_SHORT).show()
                }
        }

        // Handle Login link click
        loginLink.setOnClickListener {
            val intent = Intent(this, LoginScreen::class.java)
            startActivity(intent)
        }

        // Handle Back button click
        backButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
