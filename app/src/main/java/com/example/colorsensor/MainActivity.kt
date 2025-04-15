package com.example.colorsensor

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install SplashScreen
        installSplashScreen()

        // Set your content view
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("FirebaseInit", "Firebase has been initialized successfully")

        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        // Only load fragment if it's a fresh start
        if (savedInstanceState == null) {
            val initialFragment = if (isLoggedIn) HomeFragment() else LandingFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, initialFragment)
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

    // Navigation bar
    fun setBottomNavVisible(isVisible: Boolean) {
        val navBar = findViewById<BottomNavigationView>(R.id.bottomNavigationView3)
        navBar.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
