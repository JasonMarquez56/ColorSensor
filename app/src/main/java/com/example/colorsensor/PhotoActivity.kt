package com.example.colorsensor

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class PhotoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.find_color) // Ensure the correct layout file is referenced here

        val backButton = findViewById<ImageButton>(R.id.backButton)

        // Handle Back button click
        backButton.setOnClickListener {
            finish() // Go back to the previous activity
        }
    }
}