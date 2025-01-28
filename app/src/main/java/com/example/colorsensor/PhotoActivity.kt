package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class PhotoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.find_color) // Ensure the correct layout file is referenced here

        val backButton = findViewById<ImageButton>(R.id.backButton2)

        // Handle Back button click
        backButton.setOnClickListener {
            finish() // Go back to the previous activity
        }
    }
}