package com.example.colorsensor

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting_screen)

        //val switch = findViewById<Switch>(R.id.colorBlind)
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Set switch state from saved preference
        //switch.isChecked = sharedPreferences.getBoolean("setting_enabled", false)

//        switch.setOnCheckedChangeListener { _, isChecked ->
//            editor.putBoolean("setting_enabled", isChecked)
//            editor.apply()
//        }

        val backButton = findViewById<ImageButton>(R.id.backButton)

        // Handle Back button click
        backButton.setOnClickListener {
            onBackPressed() // This will take the user back to the previous activity
        }

        val protanomalySwitch = findViewById<Switch>(R.id.protanomalySwitch)
        val deuteranomalySwitch = findViewById<Switch>(R.id.deuteranomalySwitch)
        val tritanomalySwitch = findViewById<Switch>(R.id.tritanomalySwitch)

        val allSwitches = listOf(protanomalySwitch, deuteranomalySwitch, tritanomalySwitch)

        // Restore saved states from individual keys
        protanomalySwitch.isChecked = sharedPreferences.getBoolean("protanomaly_enabled", false)
        deuteranomalySwitch.isChecked = sharedPreferences.getBoolean("deuteranomaly_enabled", false)
        tritanomalySwitch.isChecked = sharedPreferences.getBoolean("tritanomaly_enabled", false)

        // Flag to prevent infinite loop during programmatic changes
        var isChanging = false

        val switchListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChanging) return@OnCheckedChangeListener

            if (isChecked) {
                isChanging = true

                // Uncheck other switches
                allSwitches.forEach { sw ->
                    if (sw != buttonView) {
                        sw.isChecked = false
                    }
                }

                // Save current state
                editor.putBoolean("protanomaly_enabled", protanomalySwitch.isChecked)
                editor.putBoolean("deuteranomaly_enabled", deuteranomalySwitch.isChecked)
                editor.putBoolean("tritanomaly_enabled", tritanomalySwitch.isChecked)
                editor.apply()

                isChanging = false
            } else {
                // Even if turned off, save it
                editor.putBoolean("protanomaly_enabled", protanomalySwitch.isChecked)
                editor.putBoolean("deuteranomaly_enabled", deuteranomalySwitch.isChecked)
                editor.putBoolean("tritanomaly_enabled", tritanomalySwitch.isChecked)
                editor.apply()
            }
        }

        protanomalySwitch.setOnCheckedChangeListener(switchListener)
        deuteranomalySwitch.setOnCheckedChangeListener(switchListener)
        tritanomalySwitch.setOnCheckedChangeListener(switchListener)
    }
}