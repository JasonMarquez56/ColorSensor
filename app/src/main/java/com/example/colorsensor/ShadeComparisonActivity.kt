package com.example.colorsensor

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.colorsensor.utils.PaintFinder
import com.google.android.material.bottomnavigation.BottomNavigationView

class ShadeCompareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.shade_comparison)

        navigationBar()

        // Retrieve the passed color information from the Intent
        val selectedColor = intent.getIntExtra("selected_color", Color.WHITE)
        val colorName = intent.getStringExtra("color_name") ?: "No name available"

        val colorNameText = findViewById<TextView>(R.id.colorNameText)
        val colorNameText2 = findViewById<TextView>(R.id.colorNameText2)
        colorNameText.text = colorName

        val colorBlock = findViewById<View>(R.id.viewColor)
        val targetColorBlock = findViewById<View>(R.id.viewColor12)

        // Get the LayerDrawable from the ImageView's background
        val colorBlockLayerDrawable = colorBlock.background as LayerDrawable
        val targetColorBlockLayerDrawable = targetColorBlock.background as LayerDrawable

        // Get the GradientDrawable for the colorOverlay item
        val colorOverlay = colorBlockLayerDrawable.findDrawableByLayerId(R.id.colorOverlay) as GradientDrawable
        val targetColorOverlay = targetColorBlockLayerDrawable.findDrawableByLayerId(R.id.colorOverlay) as GradientDrawable

        // Set the color of the GradientDrawable
        colorOverlay.setColor(selectedColor)

        // Create gradient effect
        val gradientLayout = findViewById<LinearLayout>(R.id.gradientLayout)
        val steps = 10
        val alpha = Color.alpha(selectedColor)
        val red = Color.red(selectedColor)
        val green = Color.green(selectedColor)
        val blue = Color.blue(selectedColor)

        for (i in 0 until steps) {
            val factor = (i - steps / 2).toFloat() / (steps / 2)
            val newRed = (red + factor * (255 - red)).toInt().coerceIn(0, 255)
            val newGreen = (green + factor * (255 - green)).toInt().coerceIn(0, 255)
            val newBlue = (blue + factor * (255 - blue)).toInt().coerceIn(0, 255)
            val color = Color.argb(alpha, newRed, newGreen, newBlue)
            val colorName2 = searchClosestColor(newRed, newGreen, newBlue, colorNameText2)

            val imageView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                setBackgroundColor(color)
                setOnClickListener {
                    // Update the targetColorBlock with the selected color
                    targetColorOverlay.setColor(color)
                    colorNameText2.text = colorName2
                }
            }
            gradientLayout.addView(imageView)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun searchClosestColor(targetRed: Int, targetGreen: Int, targetBlue: Int, textName: TextView): String {
        val targetColor = PaintFinder.PaintColor("", "", targetRed, targetGreen, targetBlue)
        val closestPaint = PaintFinder.findClosestPaint(targetColor, this)
        // Setting XML values to correct paint and RGB when found
        if (closestPaint != null) {
            val closestRGB = "(${closestPaint.r}, ${closestPaint.g}, ${closestPaint.b})"
            val closestHex = rgbToHex(closestPaint.r, closestPaint.g, closestPaint.b)
            return closestPaint.name
        } else {
            return "No matching paint found"
        }
    }

    private fun rgbToHex(red: Int, green: Int, blue: Int): String {
        return String.format("#%02X%02X%02X", red, green, blue)
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
                    true
                }
                else -> false
            }
        }
    }
}