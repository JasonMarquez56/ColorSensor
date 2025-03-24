package com.example.colorsensor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.util.Log
import android.view.View
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.view.LayoutInflater
import android.graphics.Color
import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.api.Distribution.BucketOptions.Linear
import com.example.colorsensor.RegisterActivity.favColor
import com.example.colorsensor.RegisterActivity.RGB
import android.view.Gravity
import android.widget.EditText
import kotlinx.coroutines.selects.select
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.view.KeyEvent

//Testing result for search result
import java.io.File
import java.net.URL

class ShopActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    var favColors : MutableList<favColor> = mutableListOf<favColor>()
    var selectedFav = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        setContentView(R.layout.shop_online)
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        var username = sharedPreferences.getString("username", "Guest")
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
                    }
                } else {
                    Log.d("Firestore", "No user found with username: $username")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents: ", exception)
            }
        // Find views
        val back = findViewById<ImageButton>(R.id.previous)
        val favorite = findViewById<Button>(R.id.favoriteButton)
        val searchColors = findViewById<EditText>(R.id.searchColors)
        favorite.setOnClickListener{
            showPopup(searchColors)
        }
        val results = findViewById<LinearLayout>(R.id.Results)
        searchColors.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                searchGoogle(searchColors.text.toString(),results)
                true 
            } else {
                false // Let the system handle other key events
            }
        }
        back.setOnClickListener {
            finish() // Go back to the previous activity
        }
    }
    private fun searchGoogle(query: String, results: LinearLayout){
        val apiKey = "AIzaSyBFCNcSION-b11NXfyz5ZR2jU_oXUjcgrE"
        val cx = "9302c865f0ecb43fe"
        val searchUrl = "https://www.googleapis.com/customsearch/v1?q=$query&key=$apiKey&cx=$cx"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(searchUrl)
            .get()
            .build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Unexpected code $response")

                    val jsonData = response.body?.string()
                    val jsonObject = JSONObject(jsonData)
                    val items = jsonObject.getJSONArray("items")
                    val domains = mutableSetOf<String>()
                    val resultBuilder = StringBuilder()

                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val title = item.getString("title")
                        val link = item.getString("link")
                        val domain = URL(link).host
                        if (domains.contains(domain)) {
                            continue
                        }
                        domains.add(domain)
                        // Create TextView dynamically
                        runOnUiThread {
                            val textView = TextView(this).apply {
                                text = title
                                textSize = 16f
                                setTextColor(Color.BLUE)
                                isClickable = true
                                setPadding(10, 10, 10, 10)

                                // Make it clickable
                                setOnClickListener {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                    startActivity(intent)
                                }

                            }
                            Toast.makeText(this, "Title:  $title", Toast.LENGTH_SHORT)
                                .show()

                            // Add TextView to LinearLayout
                            results.addView(textView)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
    private fun showPopup(searchColors: EditText) {
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
        displayColors(favColorContainer, searchColors)
        // Set up the close button
        val closeButton = popupView.findViewById<Button>(R.id.closePopupButton)
        closeButton.setOnClickListener {
            popupWindow.dismiss()
            Toast.makeText(this, "Color Selected:  $selectedFav", Toast.LENGTH_SHORT)
                .show()
        }
    }
    private fun displayColors(favColorContainer : LinearLayout,searchColors : EditText){
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
                searchColors.setText(selectedFav)
            }
            textView.textSize = 20f
            textView.setTextColor(Color.BLACK)
            textView.setPadding(16, 8, 16, 8)

            // Add to LinearLayout
            favColorContainer.addView(textView)
        }
    }
}
