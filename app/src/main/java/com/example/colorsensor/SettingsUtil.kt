package com.example.colorsensor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Switch
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView

object SettingsUtil {
    fun updateTextViewBasedOnSettings(context: Context, textView: TextView) {
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isSettingEnabled = sharedPreferences.getBoolean("setting_enabled", false)

        textView.text = if (isSettingEnabled) "Hello setting works!" else ""
    }
    // return trust if setting enable
    fun isSettingEnabled(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("setting_enabled", false)
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


    fun isProtanomalyEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("protanomaly_enabled", false)
    }

    fun isDeuteranomalyEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("deuteranomaly_enabled", false)
    }

    fun isTritanomalyEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("tritanomaly_enabled", false)
    }

    fun navigationBar(activity: Activity) {
        val bottomNavigationView = activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView3)

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
                    val intent = Intent(activity, ProfileActivity::class.java)
                    activity.startActivity(intent)
                    true
                }
                R.id.home -> {
                    val intent = Intent(activity, HomeActivity::class.java)
                    activity.startActivity(intent)
                    true
                }
                R.id.settings -> {
                    val intent = Intent(activity, SettingActivity::class.java)
                    activity.startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
}