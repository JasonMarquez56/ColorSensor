package com.example.colorsensor

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.example.colorsensor.utils.PaintFinder
import android.content.Intent

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

        // Using helper function to set update text and color box
        updateColorInfo(complementaryColor, complementaryTextView, complementaryColorBox)

        // Split Complementary Colors boxes
        val splitComplementaryTextView1: TextView = findViewById(R.id.splitComplementaryTextView1)
        val splitComplementaryColorBox1 = findViewById<View>(R.id.splitComplementaryColorBox1)

        val splitComplementaryTextView2: TextView = findViewById(R.id.splitComplementaryTextView2)
        val splitComplementaryColorBox2 = findViewById<View>(R.id.splitComplementaryColorBox2)

        // Finding split complimentary colors
        val (splitComp1, splitComp2) = getSplitComplementaryColors(selectedColor)

        // Updating text and color boxes for split complimentary
        updateColorInfo(splitComp1, splitComplementaryTextView1, splitComplementaryColorBox1)
        updateColorInfo(splitComp2, splitComplementaryTextView2, splitComplementaryColorBox2)


        // Display a back button
        val backButton = findViewById<ImageView>(R.id.backButton)
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

    private fun getSplitComplementaryColors(color: Int): Pair<Int, Int> {
        // Convert to HSL
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)

        // Calculate split complementary colors (±150 degrees)
        val splitComp1 = FloatArray(3)
        val splitComp2 = FloatArray(3)

        splitComp1[0] = (hsl[0] + 150) % 360
        splitComp2[0] = (hsl[0] - 150 + 360) % 360  // Ensure it's positive

        splitComp1[1] = hsl[1] // Keep saturation
        splitComp1[2] = hsl[2] // Keep lightness

        splitComp2[1] = hsl[1]
        splitComp2[2] = hsl[2]

        // Convert back to RGB
        val color1 = ColorUtils.HSLToColor(splitComp1)
        val color2 = ColorUtils.HSLToColor(splitComp2)

        return Pair(color1, color2)
    }

    private fun updateColorInfo(
        color: Int,
        textView: TextView,
        colorBox: View
    ) {
        // Extract RGB values from the color
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        // Using the RGB values to search the database
        val targetColor = PaintFinder.PaintColor("", "", red, green, blue)
        val closestPaint = PaintFinder.findClosestPaint(targetColor, this)

        // Set the XML values to the correct paint and RGB when found
        if (closestPaint != null) {
            textView.text = "${closestPaint.name}"
            val closestPaintColor = Color.rgb(closestPaint.r, closestPaint.g, closestPaint.b)
            colorBox.setBackgroundColor(closestPaintColor)

            // Make the box clickable and route to PaintInfoActivity
            colorBox.setOnClickListener {
                val intent = Intent(this, PaintInfoActivity::class.java)
                intent.putExtra("selected_color", closestPaintColor) // Pass the paint color
                intent.putExtra("color_name", closestPaint.name) // Pass the paint color name
                startActivity(intent)
            }
        } else {
            textView.text = "No matching paint found"
        }
    }


}



