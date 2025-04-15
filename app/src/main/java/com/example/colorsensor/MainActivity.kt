package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.fragment.NavHostFragment


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install SplashScreen
        installSplashScreen()

        // Set your content view
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("FirebaseInit", "Firebase has been initialized successfully")

        // Load initial fragment
        if (isLoggedIn) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        } else if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LandingFragment())
                .commit()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

}