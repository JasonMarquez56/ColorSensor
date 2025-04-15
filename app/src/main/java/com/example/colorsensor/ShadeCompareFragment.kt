package com.example.colorsensor

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.colorsensor.utils.PaintFinder

class ShadeCompareFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_shade_compare, container, false)
    }

    private var selectedColor: Int? = null
    private var colorName: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the passed color information from the Intent
        arguments?.let {
            selectedColor = it.getInt("selected_color", Color.WHITE)
            colorName = it.getString("color_name") ?: "No name available"
        }

        val colorNameText = view.findViewById<TextView>(R.id.colorNameText)
        val colorNameText2 = view.findViewById<TextView>(R.id.colorNameText2)
        colorNameText.text = colorName

        val colorBlock = view.findViewById<View>(R.id.viewColor)
        val targetColorBlock = view.findViewById<View>(R.id.viewColor12)

        // Get the LayerDrawable from the ImageView's background
        val colorBlockLayerDrawable = colorBlock.background as LayerDrawable
        val targetColorBlockLayerDrawable = targetColorBlock.background as LayerDrawable

        // Get the GradientDrawable for the colorOverlay item
        val colorOverlay = colorBlockLayerDrawable.findDrawableByLayerId(R.id.colorOverlay) as GradientDrawable
        val targetColorOverlay = targetColorBlockLayerDrawable.findDrawableByLayerId(R.id.colorOverlay) as GradientDrawable

        // Set the color of the GradientDrawable
        colorOverlay.setColor(selectedColor ?: Color.WHITE)

        // Create gradient effect
        val gradientLayout = view.findViewById<LinearLayout>(R.id.gradientLayout)
        val steps = 10
        val alpha = Color.alpha(selectedColor ?: Color.WHITE)
        val red = Color.red(selectedColor ?: Color.WHITE)
        val green = Color.green(selectedColor ?: Color.WHITE)
        val blue = Color.blue(selectedColor ?: Color.WHITE)

        for (i in 0 until steps) {
            val factor = (i - steps / 2).toFloat() / (steps / 2)
            val newRed = (red + factor * (255 - red)).toInt().coerceIn(0, 255)
            val newGreen = (green + factor * (255 - green)).toInt().coerceIn(0, 255)
            val newBlue = (blue + factor * (255 - blue)).toInt().coerceIn(0, 255)
            val color = Color.argb(alpha, newRed, newGreen, newBlue)
            val colorName2 = searchClosestColor(newRed, newGreen, newBlue, colorNameText2)

            val imageView = View(requireContext()).apply {
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
        val closestPaint = PaintFinder.findClosestPaint(targetColor, requireContext())
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
}