package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.graphics.Color
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.api.Distribution.BucketOptions.Linear
import com.example.colorsensor.RegisterActivity.favColor
import com.example.colorsensor.RegisterActivity.RGB

class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    var favColors : MutableList<favColor> = mutableListOf<favColor>()
    var selectedFav = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_screen)
        navigationBar()

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        // Get username from SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        var username = sharedPreferences.getString("username", "Guest")

        // Find views
        val profileUsername = findViewById<TextView>(R.id.profileUsername)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val favColorContainer: LinearLayout = findViewById(R.id.colors)
        val upButton = findViewById<Button>(R.id.upButton)
        val downButton = findViewById<Button>(R.id.downButton)
        val saveButton = findViewById<Button>(R.id.saveButton)


        firestore.collection("users")
            .whereEqualTo("username", username)  // Query by username
            .get()
            .addOnSuccessListener {documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
//                        Retrieve the user's favorite colors

                        val colors = document.get("favoriteColors") as? List<Map<String, Any>> ?: emptyList()
                        favColors = colors.mapNotNull { colorMap ->
                            try {
                                val name = colorMap["name"] as? String
                                val rgbMap = colorMap["rgb"] as? Map<String, Long>
                                val r = rgbMap?.get("r")?.toInt() ?: 0
                                val g = rgbMap?.get("g")?.toInt() ?: 0
                                val b = rgbMap?.get("b")?.toInt() ?: 0
                                favColor(name, RGB(r, g, b))
                            } catch (e: Exception) {
                                Log.e("Firestore", "Error parsing favorite color", e)
                                null // Skip this entry if there’s any problem
                            }
                        }.toMutableList()
                        displayColors(favColorContainer)
                    }
                } else {
                    Log.d("Firestore", "No user found with username: $username")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents: ", exception)
            }


        // Set the username
        profileUsername.text = username
        upButton.setOnClickListener(){
            val index = favColors.indexOfFirst{it.name ==selectedFav}
            if(index >0){
                Toast.makeText(this, "Succeeded to change Order of $selectedFav", Toast.LENGTH_SHORT)
                    .show()
                val swap = favColors.get(index-1)
                val current = favColors.get(index)
                favColors.set(index-1, current)
                favColors.set(index,swap)
                //CHANGE Text view of SELECTED COLOR WITH ABOVE
                displayColors(favColorContainer)
            }
            else if (index ==0){
                Toast.makeText(this, "Already Your Top favorite.", Toast.LENGTH_SHORT)
                    .show()
            }
            else{
                Toast.makeText(this, "Select a color first.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        downButton.setOnClickListener(){
            val index = favColors.indexOfFirst{it.name ==selectedFav}
            if(index <favColors.size-1 ){
                Toast.makeText(this, "Succeeded to change Order of $selectedFav", Toast.LENGTH_SHORT)
                    .show()
                val swap = favColors.get(index+1)
                val current = favColors.get(index)
                favColors.set(index+1, current)
                favColors.set(index,swap)
                //CHANGE Text view of SELECTED COLOR WITH ABOVE
                displayColors(favColorContainer)
            }
            else if (index ==favColors.size-1){
                Toast.makeText(this, "Already Your Least favorite.", Toast.LENGTH_SHORT)
                    .show()
            }
            else{
                Toast.makeText(this, "Select a color first.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        saveButton.setOnClickListener(){
            firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        for (document in documents) {
                            val userDocRef = firestore.collection("users").document(document.id)
                            userDocRef.update("favoriteColors", favColors)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Succeeded to update favorite Colors", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error Updating", Toast.LENGTH_SHORT)
                                        .show()
                                }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error getting documents", Toast.LENGTH_SHORT)
                        .show()
                }
        }
        // Handle Logout button click
        logoutButton.setOnClickListener {
            // Log out from Firebase Auth
            auth.signOut()

            // Clear stored session data
            sharedPreferences.edit().clear().apply()

            // Redirect to LoginActivity
            val intent = Intent(this, LandingActivity::class.java)
            startActivity(intent)
            finish() // Close ProfileActivity
        }

        // Handle Back button click
        backButton.setOnClickListener {
            finish() // Go back to the previous activity
        }
    }
    private fun displayColors(favColorContainer : LinearLayout){
        favColorContainer.removeAllViews()
        for (color in favColors){
            val textView = TextView(this)
            textView.text = color.name

            val red = color.rgb.r
            val green = color.rgb.g
            val blue = color.rgb.b

            textView.setBackgroundColor(Color.rgb(red,green,blue))


            textView.setOnClickListener {
                selectedFav = textView.text.toString()
            }
            textView.textSize = 20f
            textView.setTextColor(Color.BLACK)
            textView.setPadding(16, 8, 16, 8)

            // Add to LinearLayout
            favColorContainer.addView(textView)
        }
    }

    private fun navigationBar() {
        // Navigation bar
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView3)

        // Map default and selected icons
        val iconMap = mapOf(
            R.id.profile to Pair(R.drawable.account_outline, R.drawable.account),
            R.id.home to Pair(R.drawable.home_outline, R.drawable.home),
            R.id.settings to Pair(R.drawable.cog_outline, R.drawable.cog)
        )

        // Track currently selected item
        var selectedItemId: Int? = null

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->

            // Reset previous selection
            selectedItemId?.let { prevId ->
                bottomNavigationView.menu.findItem(prevId).setIcon(iconMap[prevId]?.first ?: R.drawable.home)
            }

            // Change selected icon
            item.setIcon(iconMap[item.itemId]?.second ?: R.drawable.home)
            selectedItemId = item.itemId

            when (item.itemId) {
                R.id.profile -> {
                    // Handle Profile button click
                    true
                }
                R.id.home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.settings -> {
                    // Handle Settings button click
                    true
                }
                else -> false
            }
        }
    }
}
