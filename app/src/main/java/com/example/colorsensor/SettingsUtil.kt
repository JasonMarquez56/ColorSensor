package com.example.colorsensor

import android.content.Context
import android.widget.TextView

object SettingsUtil {
    fun updateTextViewBasedOnSettings(context: Context, textView: TextView) {
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isSettingEnabled = sharedPreferences.getBoolean("setting_enabled", false)

        textView.text = if (isSettingEnabled) "Hello setting works!" else ""
    }

    // Testing protanomaly
    fun hexToProtanomalyHex(red: Int, green: Int, blue: Int): String {

        val protanomalyMatrix = arrayOf(
            arrayOf(0.817, 0.183, 0.0),
            arrayOf(0.333, 0.667, 0.0),
            arrayOf(0.0, 0.125, 0.875)
        )

        val r = red
        val g = green
        val b = blue

        val newR = protanomalyMatrix[0][0] * r + protanomalyMatrix[0][1] * g + protanomalyMatrix[0][2] * b
        val newG = protanomalyMatrix[1][0] * r + protanomalyMatrix[1][1] * g + protanomalyMatrix[1][2] * b
        val newB = protanomalyMatrix[2][0] * r + protanomalyMatrix[2][1] * g + protanomalyMatrix[2][2] * b

        val newRInt = Math.round(newR).toInt()
        val newGInt = Math.round(newG).toInt()
        val newBInt = Math.round(newB).toInt()

        // 3. Convert back to hex
        return String.format("#%02x%02x%02x", newRInt, newGInt, newBInt)
    }

    // Deuteranomaly
    fun hexToDeuteranomalyHex(red: Int, green: Int, blue: Int): String {

        val deuteranomalyMatrix = arrayOf(
            arrayOf(0.8, 0.2, 0.0),
            arrayOf(0.258, 0.742, 0.0),
            arrayOf(0.0, 0.142, 0.858)
        )

        val r = red
        val g = green
        val b = blue

        val newR = deuteranomalyMatrix[0][0] * r + deuteranomalyMatrix[0][1] * g + deuteranomalyMatrix[0][2] * b
        val newG = deuteranomalyMatrix[1][0] * r + deuteranomalyMatrix[1][1] * g + deuteranomalyMatrix[1][2] * b
        val newB = deuteranomalyMatrix[2][0] * r + deuteranomalyMatrix[2][1] * g + deuteranomalyMatrix[2][2] * b

        val newRInt = Math.round(newR).toInt()
        val newGInt = Math.round(newG).toInt()
        val newBInt = Math.round(newB).toInt()

        // 3. Convert back to hex
        return String.format("#%02x%02x%02x", newRInt, newGInt, newBInt)
    }

    // Tritanomaly
    fun hexToTritanomalyHex(red: Int, green: Int, blue: Int): String {

        val tritanomalyMatrix = arrayOf(
            arrayOf(0.967, 0.033, 0.0),   // Red channel
            arrayOf(0.0, 0.733, 0.267),   // Green channel
            arrayOf(0.0, 0.183, 0.817)    // Blue channel
        )

        val r = red
        val g = green
        val b = blue

        val newR = tritanomalyMatrix[0][0] * r + tritanomalyMatrix[0][1] * g + tritanomalyMatrix[0][2] * b
        val newG = tritanomalyMatrix[1][0] * r + tritanomalyMatrix[1][1] * g + tritanomalyMatrix[1][2] * b
        val newB = tritanomalyMatrix[2][0] * r + tritanomalyMatrix[2][1] * g + tritanomalyMatrix[2][2] * b

        val newRInt = newR.coerceIn(0.0, 255.0).toInt()
        val newGInt = newG.coerceIn(0.0, 255.0).toInt()
        val newBInt = newB.coerceIn(0.0, 255.0).toInt()

        // 3. Convert back to hex
        return String.format("#%02x%02x%02x", newRInt, newGInt, newBInt)
    }
}