package com.example.colorsensor

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.example.colorsensor.utils.PaintFinder

class PaintInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.paint_info)

        // Retrieve the passed color information from the Intent
        val selectedColor = intent.getIntExtra("selected_color", Color.WHITE)
        val colorName = intent.getStringExtra("color_name") ?: "No name available"

        // Set the background color of the top box (where you want to show the closest color)
        val colorBox = findViewById<View>(R.id.colorBox)
        colorBox.setBackgroundColor(selectedColor)

        // Display the color's name
        val colorNameTextView = findViewById<TextView>(R.id.colorNameTextView)
        colorNameTextView.text = "Closest Color: $colorName"

        // Textview and box for complementary
        val complementaryTextView: TextView = findViewById(R.id.complementaryTextView)
        val complementaryColorBox = findViewById<View>(R.id.complementaryColorBox)

        // Find the complementary color
        val complementaryColor = getComplementaryColor(selectedColor)

        // Finding the RGB Values of the complementary color
        val red = Color.red(complementaryColor)
        val green = Color.green(complementaryColor)
        val blue = Color.blue(complementaryColor)

        // Using the above values to search the database
        val targetColor = PaintFinder.PaintColor("", "", red, green, blue)
        val closestPaint = PaintFinder.findClosestPaint(targetColor, this)
        // Setting XML values to correct paint and RGB when found
        if (closestPaint != null) {
            complementaryTextView.text = "Complementary Paint: ${closestPaint.name}"
            val closestPaintColor = Color.rgb(closestPaint.r, closestPaint.g, closestPaint.b)
            complementaryColorBox.setBackgroundColor(closestPaintColor)
        } else {
            complementaryTextView.text = "No matching paint found"
        }

        // Display a back button
        val backButton = findViewById<ImageView>(R.id.backButton) // Assuming you have a back button
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    // Function to find the complementary color
    private fun getComplementaryColor(color: Int): Int {
        // Convert to HSL
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)

        // Shift the hue by 180 degrees to find the complementary color
        hsl[0] = (hsl[0] + 180) % 360

        // Convert back to RGB from the modified HSL
        return ColorUtils.HSLToColor(hsl)
    }
}
