package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    private lateinit var firestore: FirebaseFirestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Find views
        val emailField = view.findViewById<EditText>(R.id.editTextTextEmailAddress)
        val passwordField = view.findViewById<EditText>(R.id.editTextTextPassword)
        val loginButton = view.findViewById<Button>(R.id.login_button2)
        val registerLink = view.findViewById<TextView>(R.id.textView5)
        val backButton = view.findViewById<ImageButton>(R.id.backButton)

        // Handle Login button click
        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            // Validate fields
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("isLoggedIn", true)
                putString("loggedInUserEmail", email) // or user ID
                apply()
            }

            // Query Firestore for the user
            firestore.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("password", RegisterFragment.hashPassword(password))//calls .hashpsasword() in registeractivity
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Invalid credentials
                        Toast.makeText(
                            requireContext(),
                            "Invalid email or password. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Successful login, store user data
                        for (document in documents) {
                            val username = document.getString("username")

                            // Save username to SharedPreferences
                            val sharedPreferences = requireContext().getSharedPreferences("UserSession", MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putString("username", username)
                                apply()
                            }

                            Toast.makeText(requireContext(), "Welcome back, $username!", Toast.LENGTH_SHORT).show()

                            // Navigate to HomeActivity
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, HomeFragment())
                                .addToBackStack(null)
                                .commit()
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Login failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

        // Handle Register link click
        registerLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}