package com.example.colorsensor

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.ByteArrayOutputStream
import androidx.core.content.edit

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get username from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("UserSession", MODE_PRIVATE)
        val profileUsername = view.findViewById<TextView>(R.id.profileUsername)
        val username = sharedPreferences.getString("username", "Guest") ?: "Guest"

        // Set the username
        profileUsername.text = username

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val listView = view.findViewById<ListView>(R.id.listView)

        // 1. Sample data
        val items = listOf("Favorite Colors", "Friends", "Logout")

        // 2. Adapter connects data to ListView
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)

        // 3. Attach adapter to ListView
        listView.adapter = adapter

        // 4. Optional: Handle item clicks
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = items[position]
            if (selectedItem == "Logout") {
                showLogoutConfirmation(sharedPreferences)
            }
        }
    }

    private fun showLogoutConfirmation(sharedPreferences: SharedPreferences) {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                performLogout(sharedPreferences)  // your logout logic here
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun performLogout(sharedPreferences: SharedPreferences) {
        // Log out from Firebase Auth
        auth.signOut()

        // Clear stored session data
        sharedPreferences.edit() { clear() }
        val sharedPref = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isLoggedIn", false)
            remove("loggedInUserEmail")
            apply()
        }

        // Redirect to LoginActivity
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LandingFragment())
            .addToBackStack(null)
            .commit()

        // Logout Toast
        Toast.makeText(requireContext(), "You've been logged out", Toast.LENGTH_SHORT)
            .show()
    }
}