package com.example.colorsensor

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.LAYOUT_INFLATER_SERVICE
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URL
import androidx.core.net.toUri
import com.example.colorsensor.RegisterFragment.RGB
import com.example.colorsensor.RegisterFragment.favColor
import com.google.android.material.bottomnavigation.BottomNavigationView

class ShopFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_shop, container, false)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    var favColors : MutableList<favColor> = mutableListOf<favColor>()
    var selectedFav = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigationBar()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val sharedPreferences = requireContext().getSharedPreferences("UserSession", MODE_PRIVATE)
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
        val back = requireView().findViewById<ImageButton>(R.id.previous)
        val favorite = requireView().findViewById<Button>(R.id.favoriteButton)
        val searchColors = requireView().findViewById<EditText>(R.id.searchColors)
        favorite.setOnClickListener{
            showPopup(searchColors)
        }
        val results = requireView().findViewById<LinearLayout>(R.id.Results)
        val logos = requireView().findViewById<LinearLayout>(R.id.Logos)
        searchColors.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                searchGoogle(searchColors.text.toString(),results,logos)
                true
            } else {
                false // Let the system handle other key events
            }
        }
        back.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun searchGoogle(query: String, results: LinearLayout, logos: LinearLayout){
        results.removeAllViews()
        logos.removeAllViews()
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
                    val jsonObject = jsonData?.let { JSONObject(it) }
                    val items = jsonObject?.getJSONArray("items")
                    val domains = mutableSetOf<String>()
                    val resultBuilder = StringBuilder()

                    if (items != null) {
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
                            //runOnUiThread {
                            val textView = TextView(requireContext()).apply {
                                text = title
                                textSize = 16f
                                setTextColor(Color.BLUE)
                                isClickable = true
                                setPadding(10, 10, 10, 10)
                                layoutParams = LinearLayout.LayoutParams(700, 200).apply {
                                    setMargins(10, 10, 10, 10)
                                }

                                // Make it clickable
                                setOnClickListener {
                                    val intent = Intent(Intent.ACTION_VIEW, link.toUri())
                                    startActivity(intent)
                                }

                            }
                            val logo = ImageView(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                                    setMargins(10, 10, 10, 10)
                                }
                                if (domain == "www.behr.com") {
                                    setImageResource(R.drawable.behr)
                                } else if (domain == "www.ppgpaints.com") {
                                    setImageResource(R.drawable.ppg)
                                } else if (domain == "www.sherwin-williams.com") {
                                    setImageResource(R.drawable.sherwin_williams)
                                } else {
                                    setImageResource(R.drawable.home_depot)
                                }
                            }
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "Title:  $title", Toast.LENGTH_SHORT).show()
                            }

                            // Add TextView to LinearLayout
                            requireActivity().runOnUiThread {
                                results.addView(textView)
                                logos.addView(logo)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    @SuppressLint("InflateParams")
    private fun showPopup(searchColors: EditText) {
        // Inflate the popup layout
        val inflater = requireContext().getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
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
            Toast.makeText(requireContext(), "Color Selected:  $selectedFav", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun displayColors(favColorContainer : LinearLayout,searchColors : EditText){
        favColorContainer.removeAllViews()
        for (color in favColors){
            val textView = TextView(requireContext())
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

    private fun navigationBar() {
        // Navigation bar
        val bottomNavigationView = view?.findViewById<BottomNavigationView>(R.id.bottomNavigationView3)

        // Map default and selected icons
        val iconMap = mapOf(
            R.id.profile to Pair(R.drawable.account_outline, R.drawable.account),
            R.id.home to Pair(R.drawable.home_outline, R.drawable.home),
            R.id.settings to Pair(R.drawable.cog_outline, R.drawable.cog)
        )

        // Track currently selected item
        var selectedItemId: Int? = null

        bottomNavigationView?.setOnItemSelectedListener { item ->


            // Reset previous selection
            selectedItemId?.let { prevId ->
                bottomNavigationView.menu.findItem(prevId).setIcon(iconMap[prevId]?.first ?: R.drawable.home)
            }

            // Change selected icon
            item.setIcon(iconMap[item.itemId]?.second ?: R.drawable.home)
            selectedItemId = item.itemId

            when (item.itemId) {
                R.id.profile -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProfileFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                R.id.home -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                R.id.settings -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LandingFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                else -> false
            }
        }
    }

}