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

        // Finding analogous complimentary colors
        val (analogousColor1, analogousColor2) = getAnalogousColors(selectedColor)

        // Analogous text and color boxes
        val analogousTextView1: TextView = findViewById(R.id.analogousTextView1)
        val analogousColorBox1 = findViewById<View>(R.id.analogousColorBox1)

        val analogousTextView2: TextView = findViewById(R.id.analogousTextView2)
        val analogousColorBox2 = findViewById<View>(R.id.analogousColorBox2)

        // Updating text and color boxes for analogous
        updateColorInfo(analogousColor1, analogousTextView1, analogousColorBox1)
        updateColorInfo(analogousColor2, analogousTextView2, analogousColorBox2)

        // Finding analogous complimentary colors
        val (triadicColor1, triadicColor2) = getTriadicColors(selectedColor)

        // Triadic text and color boxes
        val triadicTextView1: TextView = findViewById(R.id.triadicTextView1)
        val triadicColorBox1 = findViewById<View>(R.id.triadicColorBox1)

        val triadicTextView2: TextView = findViewById(R.id.triadicTextView2)
        val triadicColorBox2 = findViewById<View>(R.id.triadicColorBox2)

        // Updating text and color boxes for triadic
        updateColorInfo(triadicColor1, triadicTextView1, triadicColorBox1)
        updateColorInfo(triadicColor2, triadicTextView2, triadicColorBox2)

        // Display a back button
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener{
            val intent = Intent(this, FindColorActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
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
        // Ensure it's positive
        splitComp2[0] = (hsl[0] - 150 + 360) % 360

        // Keep saturation and lightness
        splitComp1[1] = hsl[1]
        splitComp1[2] = hsl[2]

        splitComp2[1] = hsl[1]
        splitComp2[2] = hsl[2]

        // Convert back to RGB
        val color1 = ColorUtils.HSLToColor(splitComp1)
        val color2 = ColorUtils.HSLToColor(splitComp2)

        return Pair(color1, color2)
    }

    private fun getAnalogousColors(color: Int): Pair<Int, Int> {
        // Convert to HSL
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)

        // Compute the two analogous colors by shifting hue ±30°
        val leftHSL = hsl.clone()
        val rightHSL = hsl.clone()

        // Shift left, ensuring positive, and shift right
        leftHSL[0] = (hsl[0] - 30 + 360) % 360
        rightHSL[0] = (hsl[0] + 30) % 360

        // Convert back to RGB
        val leftColor = ColorUtils.HSLToColor(leftHSL)
        val rightColor = ColorUtils.HSLToColor(rightHSL)

        return Pair(leftColor, rightColor)
    }

    private fun getTriadicColors(color: Int): Pair<Int, Int> {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)

        // Compute the two triadic colors (hue rotated by ±120 degrees)
        val triadicHSL1 = hsl.clone()
        triadicHSL1[0] = (triadicHSL1[0] + 120) % 360

        val triadicHSL2 = hsl.clone()
        triadicHSL2[0] = (triadicHSL2[0] + 240) % 360

        return Pair(ColorUtils.HSLToColor(triadicHSL1), ColorUtils.HSLToColor(triadicHSL2))
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
                // Pass the paint color and name
                intent.putExtra("selected_color", closestPaintColor)
                intent.putExtra("color_name", closestPaint.name)
                startActivity(intent)
            }
        } else {
            textView.text = "No matching paint found"
        }
    }
}



