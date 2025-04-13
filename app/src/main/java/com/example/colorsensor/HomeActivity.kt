package com.example.colorsensor

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.ByteArrayOutputStream


class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_screen)
        navigationBar()

        // Sets the scroll view to the bottom
        //val sv = findViewById<View>(R.id.scrollView3) as ScrollView
        //sv.scrollTo(0, 100)

        // Find the button by its ID
        val profileButton = findViewById<Button>(R.id.btnProfile) // profile
        val photoButton = findViewById<Button>(R.id.btnFindColor) // find color
        val colorButtonTest = findViewById<Button>(R.id.btnVideo) // live color
        val popularColorButton = findViewById<Button>(R.id.btnPopularColor) // popular color
        val colorBlendingButton = findViewById<Button>(R.id.btnColorBlending) // color blending
        val shopButton = findViewById<Button>(R.id.btnShop)
        // Set a click listener for the Photo button
        photoButton.setOnClickListener {
            val intent = Intent(this, PhotoActivity::class.java)
            startActivity(intent)
        }
        shopButton.setOnClickListener{
            val intent = Intent(this, ShopActivity::class.java)
            startActivity(intent)
        }
        // Set a click listener for the Profile button
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Set a click listener for the Video button
        colorButtonTest.setOnClickListener {
            val intent = Intent(this, LiveFeedActivity::class.java)
            startActivity(intent)
        }

        popularColorButton.setOnClickListener {
            val intent = Intent(this, PopularColor::class.java)
            startActivity(intent)
        }

        colorBlendingButton.setOnClickListener {
            val intent = Intent(this, ColorBlendingActivity::class.java)
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

    private fun navigationBar() {
        // Navigation bar
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView3)

        // Map default and selected icons
        val iconMap = mapOf(
            R.id.profile to Pair(R.drawable.account_outline, R.drawable.account),
            R.id.home to Pair(R.drawable.home_outline, R.drawable.home),
            R.id.settings to Pair(R.drawable.cog_outline, R.drawable.cog)
        )

        // Track currently selected item
        var selectedItemId: Int? = null

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->

            // Reset previous selection
            selectedItemId?.let { prevId ->
                bottomNavigationView.menu.findItem(prevId).setIcon(iconMap[prevId]?.first ?: R.drawable.home)
            }

            // Change selected icon
            item.setIcon(iconMap[item.itemId]?.second ?: R.drawable.home)
            selectedItemId = item.itemId

            when (item.itemId) {
                R.id.profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.settings -> {
                    // Handle Settings button click
                    val intent = Intent(this, SettingActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
}
