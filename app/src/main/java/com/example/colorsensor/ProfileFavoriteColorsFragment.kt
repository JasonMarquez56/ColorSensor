package com.example.colorsensor

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import com.example.colorsensor.RegisterActivity.RGB
import com.example.colorsensor.RegisterActivity.favColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class ProfileFavoriteColorsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_favorite_colors, container, false)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    var favColors : MutableList<favColor> = mutableListOf<favColor>()
    private var username: String? = null
    var selectedFav = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find Views
        val favColorContainer: LinearLayout = view.findViewById(R.id.colors)

        // Initialize Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Get username
        arguments?.let {
            username = it.getString("username")
        }

        // Retrieve the user's favorite colors
        firestore.collection("users")
            .whereEqualTo("username", username)  // Query by username
            .get()
            .addOnSuccessListener {documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {

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
                    }
                } else {
                    Log.d("Firestore", "No user found with username: $username")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents: ", exception)
            }

    }

    private fun displayColors(favColorContainer : LinearLayout,favColors:MutableList<favColor>){
        favColorContainer.removeAllViews()
        val context = context ?: return
        for (color in favColors){
            val textView = TextView(context)
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
}