package com.example.colorsensor

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ShadeCompareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.shade_comparison)

        // Retrieve the passed color information from the Intent
        val selectedColor = intent.getIntExtra("selected_color", Color.WHITE)
        val colorName = intent.getStringExtra("color_name") ?: "No name available"

        val colorNameText = findViewById<TextView>(R.id.colorNameText)
        colorNameText.text = colorName

        val colorBlock = findViewById<View>(R.id.viewColor)

        // Get the LayerDrawable from the ImageView's background
        val layerDrawable = colorBlock.background as LayerDrawable

        // Get the GradientDrawable for the colorOverlay item
        val colorOverlay = layerDrawable.findDrawableByLayerId(R.id.colorOverlay) as GradientDrawable

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

            val imageView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                setBackgroundColor(color)
            }
            gradientLayout.addView(imageView)
        }
    }
}