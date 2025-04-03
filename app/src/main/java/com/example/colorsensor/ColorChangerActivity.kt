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
import com.example.colorsensor.R

class ColorChangerActivity : AppCompatActivity(), ColorPickerDialogFragment.OnColorSelectedListener {

    private lateinit var rgbValueText: TextView
    private lateinit var imageView: ImageView
    private lateinit var colorsButton: Button
    private lateinit var originalBitmap: Bitmap  // The original bitmap image
    private lateinit var modifiedBitmap: Bitmap // The modified bitmap image

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.color_changer)

        // Initialize views
        rgbValueText = findViewById(R.id.rgbValueText)
        imageView = findViewById(R.id.imageView)
        colorsButton = findViewById(R.id.colorsButton)

        // Retrieve the byte array passed in the Intent and decode to Bitmap
        val byteArray = intent.getByteArrayExtra("image")
        if (byteArray != null) {
            originalBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } else {
            Log.e("ColorChangerActivity", "Bitmap is null")
            return
        }

        // Ensure the bitmap has a valid config
        val config = originalBitmap.config ?: Bitmap.Config.ARGB_8888
        modifiedBitmap = originalBitmap.copy(config, true)

        imageView.setImageBitmap(modifiedBitmap)

        // Set the color picker dialog on button click
        colorsButton.setOnClickListener {
            val dialog = ColorPickerDialogFragment()
            dialog.show(supportFragmentManager, "ColorPickerDialog")
        }
    }

    // This method is called when the user selects a color in the dialog
    override fun onColorSelected(color: Int) {
        // Replace the target color in the bitmap with the selected color
        modifiedBitmap = replaceColorInBitmap(modifiedBitmap, color)

        // Set the modified bitmap to the ImageView
        imageView.setImageBitmap(modifiedBitmap)

        // Display the RGB values in the TextView
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        rgbValueText.text = "RGB: ($r, $g, $b)"
    }

    // This function replaces a specific color in the bitmap with the selected color
    private fun replaceColorInBitmap(bitmap: Bitmap, newColor: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Create a new bitmap to store the modified image
        val newBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = bitmap.getPixel(x, y)

                // If the pixel is the target color (e.g., white), replace it with the new color
                if (pixelColor == Color.WHITE) { // Change this condition to match your target color
                    newBitmap.setPixel(x, y, newColor)
                } else {
                    newBitmap.setPixel(x, y, pixelColor) // Otherwise, keep the original color
                }
            }
        }
        return newBitmap
    }
}
