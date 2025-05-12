package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

class RegisterFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    private lateinit var firestore: FirebaseFirestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Find views
        val usernameField = view.findViewById<EditText>(R.id.usernameField)
        val emailField = view.findViewById<EditText>(R.id.emailField)
        val passwordField = view.findViewById<EditText>(R.id.passwordField)
        val confirmPasswordField = view.findViewById<EditText>(R.id.confirmPasswordField)
        val registerButton = view.findViewById<Button>(R.id.registerButton)
        val loginLink = view.findViewById<TextView>(R.id.loginLink)
        val backButton = view.findViewById<ImageButton>(R.id.backButton)

        // Handle Register button click
        registerButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            // Validate fields
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "All fields are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Invalid email format.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            //validates password Complexity
            val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,}$"
            val regex = Regex(passwordRegex)
            if (!regex.matches(password)) {
                Toast.makeText(
                    requireContext(),
                    "Password must be at least 8 characters and contains one of each lowercase,Uppercase, and a number .",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Check if email is already in use
            firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        Toast.makeText(requireContext(), "Email is already registered.", Toast.LENGTH_SHORT)
                            .show()
                    } else {

                        // Hashing user password
                        val hashedPassword = hashPassword(password)

                        // Add user to Firestore
                        val user = hashMapOf(
                            "username" to username,
                            "email" to email,
                            "password" to hashedPassword,
                            "favoriteColors" to mutableListOf<favColor>(),
                            "friends" to mutableListOf<String>(),
                            "requests" to mutableListOf<String>()
                        )

                        firestore.collection("users")
                            .add(user)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT)
                                    .show()

                                // Navigate to HomeActivity on successful registration
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.fragment_container, HomeFragment())
                                    .addToBackStack(null)
                                    .commit()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to register. Try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to register. Try again.", Toast.LENGTH_SHORT)
                        .show()
                }
        }
        // Handle Login link click
        loginLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .addToBackStack(null)
                .commit()
        }
    }
    data class RGB(val r: Int, val g: Int, val b: Int)
    data class favColor(val name: String?, val rgb: RGB)
    // Function to implement a SHA-256 hashing stored as companion object for login class to call function
    companion object{
        fun hashPassword(password: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray())
            return hashBytes.joinToString("") {"%02x".format(it)  }
        }
    }
}