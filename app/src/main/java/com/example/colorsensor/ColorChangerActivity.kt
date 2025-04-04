package com.example.colorsensor

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ColorPickerDialogFragment
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import com.example.colorsensor.utils.PaintFinder
import android.view.MotionEvent

class ColorChangerActivity : AppCompatActivity(), ColorPickerDialogFragment.OnColorSelectedListener {

    private lateinit var rgbValueText: TextView
    private lateinit var imageView: ImageView
    private lateinit var colorsButton: Button
    private lateinit var originalBitmap: Bitmap
    private lateinit var modifiedBitmap: Bitmap
    private lateinit var colorBox: View
    private var selectedColor: Int = Color.WHITE // Default selected color

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.color_changer)

        // Initialize views
        rgbValueText = findViewById(R.id.rgbValueText)
        imageView = findViewById(R.id.imageView)
        colorsButton = findViewById(R.id.colorsButton)
        colorBox = findViewById(R.id.colorBox)

        // Retrieve and decode the bitmap from intent
        val byteArray = intent.getByteArrayExtra("image")
        if (byteArray != null) {
            originalBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } else {
            Log.e("ColorChangerActivity", "Bitmap is null")
            return
        }

        // Ensure bitmap config is valid
        val config = originalBitmap.config ?: Bitmap.Config.ARGB_8888
        modifiedBitmap = originalBitmap.copy(config, true)
        imageView.setImageBitmap(modifiedBitmap)

        // Open color picker when button is clicked
        colorsButton.setOnClickListener {
            val dialog = ColorPickerDialogFragment()
            dialog.show(supportFragmentManager, "ColorPickerDialog")
        }

        // Detect taps on the image
        imageView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imageMatrix = imageView.imageMatrix
                val drawable = imageView.drawable ?: return@setOnTouchListener true

                val inverse = android.graphics.Matrix()
                imageMatrix.invert(inverse)

                val touchPoint = floatArrayOf(event.x, event.y)
                inverse.mapPoints(touchPoint)

                val x = touchPoint[0].toInt()
                val y = touchPoint[1].toInt()

                if (x in 0 until modifiedBitmap.width && y in 0 until modifiedBitmap.height) {
                    val tappedColor = modifiedBitmap.getPixel(x, y)
                    Log.d("ColorChangerActivity", "Tapped Color: $tappedColor at ($x, $y)")

                    // Replace similar pixels with selected color
                    modifiedBitmap = replaceColorInBitmap(modifiedBitmap, tappedColor, selectedColor)
                    imageView.setImageBitmap(modifiedBitmap)
                }
            }
            true
        }

    }

    override fun onColorSelected(color: Int) {
        selectedColor = color // Store selected color
        colorBox.setBackgroundColor(color)

        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        val targetColor = PaintFinder.PaintColor("Selected", "Current", r, g, b)
        val closestPaint = PaintFinder.findClosestPaint(targetColor, this)

        rgbValueText.text = if (closestPaint != null) {
            "Closest Paint: ${closestPaint.name}"
        } else {
            "No close match found"
        }
    }

    // Replaces pixels similar to targetColor with newColor
    private fun replaceColorInBitmap(bitmap: Bitmap, targetColor: Int, newColor: Int, tolerance: Int = 30): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = bitmap.getPixel(x, y)

                if (isColorSimilar(pixelColor, targetColor, tolerance)) {
                    newBitmap.setPixel(x, y, newColor)
                } else {
                    newBitmap.setPixel(x, y, pixelColor)
                }
            }
        }
        return newBitmap
    }

    // Determines if two colors are similar within a given tolerance
    private fun isColorSimilar(color1: Int, color2: Int, tolerance: Int): Boolean {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        return (Math.abs(r1 - r2) < tolerance &&
                Math.abs(g1 - g2) < tolerance &&
                Math.abs(b1 - b2) < tolerance)
    }
}
