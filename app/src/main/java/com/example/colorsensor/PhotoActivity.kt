package com.example.colorsensor

import android.content.Intent
import android.graphics.Camera
import android.os.Bundle
import android.widget.Button
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

        val canerabutton = findViewById<Button>(R.id.button) // find color
        canerabutton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java) // Renamed CameraActivity
            startActivity(intent)
        }
    }
}