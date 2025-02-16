package com.example.colorsensor

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils

class PaintInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.paint_info)

        // Retrieve the passed color information from the Intent
        val selectedColor = intent.getIntExtra("selected_color", Color.WHITE)  // Default to white if no color is passed
        val colorName = intent.getStringExtra("color_name") ?: "No name available"

        // Set the background color of the top box (where you want to show the closest color)
        val colorBox = findViewById<View>(R.id.colorBox)  // Replace with actual ID of the box
        colorBox.setBackgroundColor(selectedColor)

        // Optionally display the color's name and hex
        val colorNameTextView = findViewById<TextView>(R.id.colorNameTextView)  // Replace with actual ID
        colorNameTextView.text = "Closest Color: $colorName"

        // Find the complementary color
        val complementaryColor = getComplementaryColor(selectedColor)

        // Set the complementary color background in another view (you can customize this part)
        val complementaryColorBox = findViewById<View>(R.id.complementaryColorBox)  // Replace with actual ID
        complementaryColorBox.setBackgroundColor(complementaryColor)

        // Optionally display a back button
        val backButton = findViewById<ImageView>(R.id.backButton) // Assuming you have a back button
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    // Function to find the complementary color
    private fun getComplementaryColor(color: Int): Int {
        // Convert RGB to HSL
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        // Convert to HSL
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)

        // Shift the hue by 180 degrees to find the complementary color
        hsl[0] = (hsl[0] + 180) % 360

        // Convert back to RGB from the modified HSL
        return ColorUtils.HSLToColor(hsl)
    }
}
