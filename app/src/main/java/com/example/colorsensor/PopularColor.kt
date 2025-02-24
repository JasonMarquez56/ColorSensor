package com.example.colorsensor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.Color as AndroidColor // Alias for Android's Color class
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

//testing
import kotlin.random.Random

class PopularColor : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popular_color) // Ensure correct layout file

        val imageView: ImageView = findViewById(R.id.imageView2) // Ensure this ID matches your XML
        val hexMessage = findViewById<TextView>(R.id.textView9)
        val textRGB = findViewById<TextView>(R.id.textView11)


        // Load the original bitmap
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.blank_wall)

        // Define the region to change (xStart, yStart, width, height)
        val targetRegion = Rect(0, 125, 1228, 809) // Adjust these values as needed


        val newColor = 0xFF8DC6A2.toInt() // Example Hex color (Orange)

        // Modify the bitmap by filling the target region with the custom color
        val modifiedBitmap = fillRegionWithColor(bitmap, targetRegion, newColor)

        // Set the modified image in the ImageView
        imageView.setImageBitmap(modifiedBitmap)

        // Set all 25 button background color to random
        // CHANGE this to popular color
        for (i in 1..25){
            // Generate random RGB values (0-255)
            val newRed = Random.nextInt(0, 256)
            val newGreen = Random.nextInt(0, 256)
            val newBlue = Random.nextInt(0, 256)
            val alpha = 255  // Full opacity

            // Create a random color
            val color = Color.argb(alpha, newRed, newGreen, newBlue)

            // Find the view by its ID and set the background color
            val resID = resources.getIdentifier("button$i", "id", packageName)
            findViewById<Button>(resID)?.setBackgroundColor(color)
        }

        for (i in 1..25) {
            val buttonId = resources.getIdentifier("button$i", "id", packageName)
            val button = findViewById<Button>(buttonId)

            // press any 1 - 25 button
            button?.setOnClickListener {
                val backgroundColor = (button.background as ColorDrawable).color
                val colorHex = String.format("#%06X", 0xFFFFFF and backgroundColor)
                val viewColor: View by lazy { findViewById(R.id.viewColor11) }

                // Set the TextView text to show the button's background color
                hexMessage.text = "Hex: $colorHex"
                // Get rgb value
                val red = Color.red(backgroundColor)
                val green  = Color.green(backgroundColor)
                val blue  = Color.blue(backgroundColor)
                textRGB.text = "RGB: ($red, $green, $blue)"

                val alpha = Color.alpha(backgroundColor)
                viewColor.setBackgroundColor(Color.argb(alpha, red, green, blue))

                // CHANGE WALL COLOR
                // This line from 82-89 was originally from line 42
                val newColor = Color.parseColor(colorHex)

                // Modify the bitmap by filling the target region with the custom color
                val modifiedBitmap = fillRegionWithColor(bitmap, targetRegion, newColor)

                // Set the modified image in the ImageView
                imageView.setImageBitmap(modifiedBitmap)
            }
        }
    }
}

// Function to fill a specific region with a new color
fun fillRegionWithColor(bitmap: Bitmap, region: Rect, newColor: Int): Bitmap {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    for (x in region.left until region.right) {
        for (y in region.top until region.bottom) {
            mutableBitmap.setPixel(x, y, newColor) // Paint the area with the chosen color
        }
    }

    return mutableBitmap
}

// Data class for defining a rectangular area
data class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int)
