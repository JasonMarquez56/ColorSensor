package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract.Contacts.Photo
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_screen) // Ensure the correct layout file is referenced here

        // Find the button by its ID
        val profileButton = findViewById<Button>(R.id.button4) // profile
        val photobutton = findViewById<Button>(R.id.button6) // find color

        // Set a click listener for the Profile button
        photobutton.setOnClickListener {
            val intent = Intent(this, PhotoActivity::class.java)
            startActivity(intent)
        }

        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

    }
}
