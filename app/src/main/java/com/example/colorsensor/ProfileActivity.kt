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
import com.google.api.Distribution.BucketOptions.Linear

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    var favColors : MutableList<String> = mutableListOf()
    var selectedFav = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_screen)

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
                        favColors = document.get("favoriteColors") as? MutableList<String> ?: mutableListOf()
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
            val index = favColors.indexOf(selectedFav)
            if(index >0){
                Toast.makeText(this, "Succeeded to change Order of $selectedFav", Toast.LENGTH_SHORT)
                    .show()
                val swap = favColors.get(index-1)
                favColors.set(index-1, selectedFav)
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
            val index = favColors.indexOf(selectedFav)
            if(index <favColors.size-1 ){
                Toast.makeText(this, "Succeeded to change Order of $selectedFav", Toast.LENGTH_SHORT)
                    .show()
                val swap = favColors.get(index+1)
                favColors.set(index+1, selectedFav)
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
            textView.text = color
            firestore.collection("paints")
                .whereEqualTo("name", color)  // Query by username
                .get()
                .addOnSuccessListener { paints ->
                    if (!paints.isEmpty) {
                        for(paint in paints){
                            val hex = paint.get("hex") as String
                            val rgbInfo = hex.removePrefix("rgb(").removeSuffix(")").split(",")
                            val red = rgbInfo[0].trim().toInt()
                            val green = rgbInfo[1].trim().toInt()
                            val blue = rgbInfo[2].trim().toInt()

                            textView.setBackgroundColor(Color.rgb(red,green,blue))
                        }
                    }
                }
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
}
