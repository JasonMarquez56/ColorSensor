package com.example.colorsensor

import android.content.Context
import android.widget.TextView

object SettingsUtil {
    fun updateTextViewBasedOnSettings(context: Context, textView: TextView) {
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isSettingEnabled = sharedPreferences.getBoolean("setting_enabled", false)

        textView.text = if (isSettingEnabled) "Hello setting works!" else ""
    }
}