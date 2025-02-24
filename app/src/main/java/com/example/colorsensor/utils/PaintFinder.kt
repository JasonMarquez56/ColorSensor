package com.example.colorsensor.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

object PaintFinder {

    // Data class representing a paint color
    data class PaintColor(val brand: String, val name: String, val r: Int, val g: Int, val b: Int)

    // Function to load the local JSON file and parse it into a list of PaintColor objects
    fun loadPaintColors(context: Context): List<PaintColor> {
        // Open the formatted_paint_info.json file from assets/lookup
        val inputStream = context.assets.open("lookup/formatted_paint_info.json")  // Access from assets folder
        val reader = InputStreamReader(inputStream)

        // Parse the JSON using Gson into a list of PaintColor objects
        val gson = Gson()
        val listType = object : TypeToken<List<PaintColor>>() {}.type
        return gson.fromJson(reader, listType)
    }

    // Function to find the closest paint color based on RGB values
    fun findClosestPaint(
        targetColor: PaintColor,
        context: Context,
        range: Int = 30 // Optional range to tweak distance tolerance
    ): PaintColor? {
        val allColors = loadPaintColors(context)

        // Find the paint color with the minimum distance to the target color (Euclidean distance)
        return allColors.minByOrNull { color ->
            val dr = color.r - targetColor.r
            val dg = color.g - targetColor.g
            val db = color.b - targetColor.b
            // Calculate the square Euclidean distance
            dr * dr + dg * dg + db * db
        }
    }
}
