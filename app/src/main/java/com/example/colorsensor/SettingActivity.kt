package com.example.colorsensor

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting_screen)

        val switch = findViewById<Switch>(R.id.colorBlind)
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Set switch state from saved preference
        switch.isChecked = sharedPreferences.getBoolean("setting_enabled", false)

        switch.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("setting_enabled", isChecked)
            editor.apply()
        }

        val backButton = findViewById<ImageButton>(R.id.backButton)

        // Handle Back button click
        backButton.setOnClickListener {
            onBackPressed() // This will take the user back to the previous activity
        }
    }
}