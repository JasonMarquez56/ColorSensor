package com.example.colorsensor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor // Alias for Android's Color class
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class PopularColor : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popular_color) // Ensure correct layout file

        val imageView: ImageView = findViewById(R.id.imageView2) // Ensure this ID matches your XML

        // Load the original bitmap
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.blank_wall)

        // Define the region to change (xStart, yStart, width, height)
        val targetRegion = Rect(0, 125, 1228, 809) // Adjust these values as needed


        val newColor = 0xFF8DC6A2.toInt() // Example Hex color (Orange)

        // Modify the bitmap by filling the target region with the custom color
        val modifiedBitmap = fillRegionWithColor(bitmap, targetRegion, newColor)

        // Set the modified image in the ImageView
        imageView.setImageBitmap(modifiedBitmap)
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
