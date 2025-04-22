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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.api.Distribution.BucketOptions.Linear
import com.example.colorsensor.RegisterActivity.favColor
import com.example.colorsensor.RegisterActivity.RGB

class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    var favColors : MutableList<favColor> = mutableListOf<favColor>()
    var friends : MutableList<String> = mutableListOf()
    var requests : MutableList<String> = mutableListOf()
    var selectedFav = ""
    var selectedFriend = ""
    var selectedTextView: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_screen)
        navigationBar()

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        // Get username from SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        var username = sharedPreferences.getString("username", "Guest") ?: "Guest"

        // Find views
        val profileUsername = findViewById<TextView>(R.id.profileUsername)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val favColorContainer: LinearLayout = findViewById(R.id.colors)
        val friendsContainer: LinearLayout = findViewById(R.id.friends)
        val upButton = findViewById<Button>(R.id.upButton)
        val downButton = findViewById<Button>(R.id.downButton)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val friendrequestButton = findViewById<Button>(R.id.friendrequestButton)
        val acceptButton = findViewById<Button>(R.id.acceptButton)
        val friendcolorButton = findViewById<Button>(R.id.friendcolorButton)
        firestore.collection("users")
            .whereEqualTo("username", username)  // Query by username
            .get()
            .addOnSuccessListener {documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                    // Retrieve the user's favorite colors

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
                        displayColors(favColorContainer,favColors)
                        friends = document.get("friends") as? MutableList<String> ?: mutableListOf()
                        requests = document.get("requests") as? MutableList<String> ?: mutableListOf()
                        displayFriends(friendsContainer,friends,requests)
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
        //upvote a color to change order
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
                displayColors(favColorContainer,favColors)
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
                displayColors(favColorContainer,favColors)
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
                            val updates =mapOf(
                                "favoriteColors" to favColors,
                                "friends" to friends,
                                "requests" to requests
                            )
                            userDocRef.update(updates)
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
        friendrequestButton.setOnClickListener {
            var input = ""
            askInput{input->
                addFriend(input,username)
            }
        }
        friendcolorButton.setOnClickListener{
            // Inflate the popup layout
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupView: View = inflater.inflate(R.layout.popup_choose, null)

            // Create the PopupWindow
            val popupWindow = PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true // Focusable
            )

            // Show the popup window
            popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)
            val tempContainer = popupView.findViewById<LinearLayout>(R.id.colors)
            var friendColor : MutableList<favColor> = mutableListOf<favColor>()
            Toast.makeText(this, "name:  $selectedFriend", Toast.LENGTH_SHORT)
                .show()
            firestore.collection("users")
                .whereEqualTo("username", selectedFriend)  // Query by username
                .get()
                .addOnSuccessListener {documents ->
                    if (!documents.isEmpty) {
                        for (document in documents) {
//                        Retrieve the user's favorite colors

                            val colors = document.get("favoriteColors") as? List<Map<String, Any>> ?: emptyList()
                            friendColor = colors.mapNotNull { colorMap ->
                                try {
                                    val name = colorMap["name"] as? String
                                    val rgbMap = colorMap["rgb"] as? Map<String, Long>
                                    val r = rgbMap?.get("r")?.toInt() ?: 0
                                    val g = rgbMap?.get("g")?.toInt() ?: 0
                                    val b = rgbMap?.get("b")?.toInt() ?: 0
                                    favColor(name, RGB(r, g, b))
                                } catch (e: Exception) {
                                    Toast.makeText(this, "ERROR PARSING", Toast.LENGTH_SHORT)
                                        .show()
                                    null // Skip this entry if there’s any problem
                                }
                            }.toMutableList()
                            displayColors(tempContainer,friendColor)
                        }
                    } else {
                        Toast.makeText(this, "User NOT FOUND", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "ERROR GETTING DOCUMENT", Toast.LENGTH_SHORT)
                        .show()
                }

            // Set up the close button
            val closeButton = popupView.findViewById<Button>(R.id.closePopupButton)
            closeButton.setOnClickListener {
                popupWindow.dismiss()
                Toast.makeText(this, "Color Selected:  $selectedFav", Toast.LENGTH_SHORT)
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
        acceptButton.setOnClickListener{
            Toast.makeText(this, "Friend Selected:  $selectedFriend", Toast.LENGTH_SHORT)
                .show()
            if(requests.contains(selectedFriend)){
                requests.remove(selectedFriend)
                friends.add(selectedFriend)
                displayFriends(friendsContainer,friends,requests)
            }
        }


        // Handle Back button click
        backButton.setOnClickListener {
            finish() // Go back to the previous activity
        }
    }
    private fun showPopup() {
        // Inflate the popup layout
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.popup_choose, null)

        // Create the PopupWindow
        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true // Focusable
        )

        // Show the popup window
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)
        val favColorContainer = popupView.findViewById<LinearLayout>(R.id.colors)
        displayFriends(favColorContainer,friends,requests)
        // Set up the close button
        val closeButton = popupView.findViewById<Button>(R.id.closePopupButton)
        closeButton.setOnClickListener {
            popupWindow.dismiss()
            Toast.makeText(this, "Color Selected:  $selectedFav", Toast.LENGTH_SHORT)
                .show()
        }
    }
    private fun askInput(onInputReceived: (String) -> Unit) {
        // Inflate the popup layout
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.popup_input, null)

        // Create the PopupWindow
        val popupWindow = PopupWindow(
            view,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true // Focusable
        )

        // Show the popup window
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
        // Set up the close button
        val editText = view.findViewById<EditText>(R.id.popup_input)
        val button = view.findViewById<Button>(R.id.popup_button)
        button.setOnClickListener {
            val input = editText.text.toString()
            onInputReceived(input)
            Toast.makeText(this, "You entered: $input", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()

        }
    }
    private fun addFriend(friendName : String, username : String){
        firestore.collection("users")
            .whereEqualTo("username", friendName)
            .get()
            .addOnSuccessListener { documents ->
                //if user name is successfully found.
                if (!documents.isEmpty) {
                    for (document in documents) {
                        //finds user id
                        val userId = document.id
                        val user = firestore.collection("users").document(userId)
                        //add favorite color.
                        user.update("requests", FieldValue.arrayUnion(username))
                            .addOnSuccessListener {
                                Toast.makeText(this, "Sent friend request.", Toast.LENGTH_SHORT)
                                    .show()
                            }//in case failed to update
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to create", Toast.LENGTH_SHORT)
                                    .show()
                            }
                    }
                    //if no user is found give warning
                } else {
                    Log.d("Firestore", "No user found with username: $friendName")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents: ", exception)
            }
    }
    private fun displayColors(favColorContainer : LinearLayout,favColors:MutableList<favColor>){
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
            textView.setPadding(16, 8, 16, 8)
            // Calculating luminance with standard weighted formula
            val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
            // Changing text color based off luminance, making it more visible
            val textColor = if (luminance > 0.5) textView.setTextColor(Color.BLACK) else textView.setTextColor(Color.WHITE)

            // Add to LinearLayout
            favColorContainer.addView(textView)
        }
    }
    private fun displayFriends(friendsContainer : LinearLayout, friends:MutableList<String>,requests:MutableList<String>){
        friendsContainer.removeAllViews()
        for (friend in friends){
            val textView = TextView(this)
            textView.text = friend


            textView.setOnClickListener {
                selectedFriend = friend
                selectedTextView?.setBackgroundColor(Color.TRANSPARENT)
                textView.setBackgroundColor(Color.rgb(255,221,87))
                selectedTextView = textView
            }
            textView.textSize = 20f
            textView.setTextColor(Color.BLACK)
            textView.setPadding(16, 8, 16, 8)

            // Add to LinearLayout
            friendsContainer.addView(textView)
        }
        for (request in requests){
            val textView = TextView(this)
            textView.text = "Request: $request"


            textView.setOnClickListener {
                selectedFriend = request
                selectedTextView?.setBackgroundColor(Color.TRANSPARENT)
                textView.setBackgroundColor(Color.rgb(255,221,87))
                selectedTextView = textView
            }
            textView.textSize = 20f
            textView.setTextColor(Color.RED)
            textView.setPadding(16, 8, 16, 8)

            // Add to LinearLayout
            friendsContainer.addView(textView)
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
