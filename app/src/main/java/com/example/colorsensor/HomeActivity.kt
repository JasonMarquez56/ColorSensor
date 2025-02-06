package com.example.colorsensor

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_screen)

        // Find the button by its ID
        val profileButton = findViewById<Button>(R.id.profileButton) // profile
        val photoButton = findViewById<Button>(R.id.photoButton) // find color
        val colorButtonTest = findViewById<Button>(R.id.videoButton) // live color

        // Set a click listener for the Photo button
        photoButton.setOnClickListener {
            val intent = Intent(this, PhotoActivity::class.java)
            startActivity(intent)
        }

        // Set a click listener for the Profile button
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Set a click listener for the Video button
        colorButtonTest.setOnClickListener {
            val intent = Intent(this, FindColorActivity::class.java) // Renamed CameraActivity
            startActivity(intent)
        }
    }

    // Convert Bitmap to ByteArray and send to FindColorActivity
    private fun sendImageToFindColor(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()

        val intent = Intent(this, FindColorActivity::class.java)
        intent.putExtra("image_bitmap", byteArray)
        startActivity(intent)
    }

    // Send URI to FindColorActivity
    private fun sendUriToFindColor(uri: Uri) {
        val intent = Intent(this, FindColorActivity::class.java)
        intent.putExtra("image_uri", uri.toString())
        startActivity(intent)
    }
}
