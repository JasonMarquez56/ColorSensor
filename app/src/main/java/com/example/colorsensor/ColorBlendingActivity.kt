package com.example.colorsensor

import ColorPickerDialogFragment
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.colorsensor.utils.PaintFinder
import com.google.android.material.bottomnavigation.BottomNavigationView

class ColorBlendingActivity : AppCompatActivity(), ColorPickerDialogFragment.OnColorSelectedListener {

    private var selectedBlock: View? = null  // Track which block was clicked
    private var color1Value: Int? = null  // Store selected color for blendColor1
    private var color2Value: Int? = null  // Store selected color for blendColor2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.color_blending)
        navigationBar()

        val color1 = findViewById<View>(R.id.blendColor1)
        val color2 = findViewById<View>(R.id.blendColor2)
        val color3 = findViewById<View>(R.id.blendResult)

        val textName1 = findViewById<View>(R.id.closetColor1)
        val textRGB1 = findViewById<View>(R.id.RGB1)
        val textHex1 = findViewById<View>(R.id.Hex1)
        val textName2 = findViewById<View>(R.id.closetColor2)
        val textRGB2 = findViewById<View>(R.id.RGB2)
        val textHex2 = findViewById<View>(R.id.Hex2)
        val textName3 = findViewById<View>(R.id.closetColor3)
        val textRGB3 = findViewById<View>(R.id.RGB3)
        val textHex3 = findViewById<View>(R.id.Hex3)

        val clickListener = View.OnClickListener { view ->
            selectedBlock = view  // Store which block was clicked
            val dialog = ColorPickerDialogFragment()
            dialog.show(supportFragmentManager, "ColorPickerDialog")
        }

        color1.setOnClickListener(clickListener)
        color2.setOnClickListener(clickListener)
    }

    override fun onColorSelected(color: Int) {
        selectedBlock?.let { block ->
            updateDrawableColor(block, color) // Apply color using drawable layers

            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)

            when (block.id) {
                R.id.blendColor1 -> {
                    color1Value = color
                    searchClosestColor(red, green, blue, findViewById(R.id.closetColor1), findViewById(R.id.RGB1), findViewById(R.id.Hex1))
                }
                R.id.blendColor2 -> {
                    color2Value = color
                    searchClosestColor(red, green, blue, findViewById(R.id.closetColor2), findViewById(R.id.RGB2), findViewById(R.id.Hex2))
                }
            }

            updateBlendedColor()
        }
    }

    private fun updateBlendedColor() {
        val color3 = findViewById<View>(R.id.blendResult)

        if (color1Value != null && color2Value != null) {
            val blendedColor = blendColors(color1Value!!, color2Value!!)
            updateDrawableColor(color3, blendedColor)

            val red = Color.red(blendedColor)
            val green = Color.green(blendedColor)
            val blue = Color.blue(blendedColor)

            searchClosestColor(red, green, blue, findViewById(R.id.closetColor3), findViewById(R.id.RGB3), findViewById(R.id.Hex3))
        }
    }

    private fun updateDrawableColor(view: View, color: Int) {
        val layerDrawable = view.background as? LayerDrawable ?: return
        val colorOverlay = layerDrawable.findDrawableByLayerId(R.id.colorOverlay) as? GradientDrawable
        colorOverlay?.setColor(color)
    }

    private fun blendColors(color1: Int, color2: Int): Int {
        val r = (Color.red(color1) + Color.red(color2)) / 2
        val g = (Color.green(color1) + Color.green(color2)) / 2
        val b = (Color.blue(color1) + Color.blue(color2)) / 2
        return Color.rgb(r, g, b)
    }

    @SuppressLint("SetTextI18n")
    private fun searchClosestColor(targetRed: Int, targetGreen: Int, targetBlue: Int, textName: TextView, textViewRGB: TextView, textViewHex: TextView) {
        val targetColor = PaintFinder.PaintColor("", "", targetRed, targetGreen, targetBlue)
        val closestPaint = PaintFinder.findClosestPaint(targetColor, this)
        // Setting XML values to correct paint and RGB when found
        if (closestPaint != null) {
            val closestRGB = "(${closestPaint.r}, ${closestPaint.g}, ${closestPaint.b})"
            val closestHex = rgbToHex(closestPaint.r, closestPaint.g, closestPaint.b)
            textName.text = "Closest Paint: ${closestPaint.name}"
            textViewRGB.text = "RGB: $closestRGB"
            textViewHex.text = "Hex: $closestHex"
        } else {
            textName.text = "No matching paint found"
            textViewRGB.text = ""
            textViewHex.text = ""
        }
    }

    private fun rgbToHex(red: Int, green: Int, blue: Int): String {
        return String.format("#%02X%02X%02X", red, green, blue)
    }

    private fun navigationBar() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView3)

        val iconMap = mapOf(
            R.id.profile to Pair(R.drawable.account_outline, R.drawable.account),
            R.id.home to Pair(R.drawable.home_outline, R.drawable.home),
            R.id.settings to Pair(R.drawable.cog_outline, R.drawable.cog)
        )

        var selectedItemId: Int? = null

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            selectedItemId?.let { prevId ->
                bottomNavigationView.menu.findItem(prevId).setIcon(iconMap[prevId]?.first ?: R.drawable.home)
            }

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
                R.id.settings -> true
                else -> false
            }
        }
    }
}
