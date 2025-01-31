package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.home_screen) // Ensure the correct layout file is referenced here
        setContentView(R.layout.home_screen)

        // Find the button by its ID
        val profileButton = findViewById<Button>(R.id.profileButton) // profile
        val photobutton = findViewById<Button>(R.id.photoButton) // find color

        // Set a click listener for the Profile button
        photobutton.setOnClickListener {
            val intent = Intent(this, PhotoActivity::class.java)
            startActivity(intent)
        }

        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        val colorbuttonTest = findViewById<Button>(R.id.videoButton) // find color
        colorbuttonTest.setOnClickListener {
            val intent = Intent(this, FindColorActivity::class.java) // Renamed CameraActivity
            startActivity(intent)
        }

    }
}
