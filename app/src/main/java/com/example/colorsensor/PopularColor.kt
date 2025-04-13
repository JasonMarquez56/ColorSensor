package com.example.colorsensor

import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Magnifier
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.colorsensor.RegisterActivity.RGB
import com.example.colorsensor.RegisterActivity.favColor
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

import com.example.colorsensor.SettingsUtil

//testing
class PopularColor : AppCompatActivity() {
    private var magnifier: Magnifier? = null
    private var isMagnifierActive = false // Track magnifier state
    var favColors : MutableList<String> = mutableListOf()
    private lateinit var firestore: FirebaseFirestore
    var selectedFav = ""

    // empty list for RGB
    var rgbList = mutableListOf<Int>()

    // Create a magnifier tutorial
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popular_color) // Ensure correct layout file

        // These 2 lines of code test the settingActivity
        val textView = findViewById<TextView>(R.id.textView17)
        SettingsUtil.updateTextViewBasedOnSettings(this, textView)

        val imageView: ImageView = findViewById(R.id.imageView2)
        val hexMessage = findViewById<TextView>(R.id.textView9)
        val textRGB = findViewById<TextView>(R.id.textView11)
        val zoomButton: Button = findViewById(R.id.button) // Button to toggle magnifier
        // Load the original bitmap
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.blank_wall)
        // Define the region to change (xStart, yStart, width, height)
        //val targetRegion = Rect(0, 125, 1228, 809) // Adjust these values as needed

        val imageWidth = bitmap.width
        val imageHeight = bitmap.height

        val left = (0.0 * imageWidth).toInt()
        val top = (0.13 * imageHeight).toInt()
        val right = (0.735 * imageWidth).toInt()
        val bottom = (0.85 * imageHeight).toInt()

        val targetRegion = Rect(left, top, right, bottom)
        // Create a magnifier tutorial
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier = Magnifier.Builder(imageView)
                .setInitialZoom(2.3f)
                .setElevation(10.0f)
                .setCornerRadius(20.0f)
                .setSize(100, 100)
                .build()
        }

        // Toggle magnifier on button press
        zoomButton.setOnClickListener {
            isMagnifierActive = !isMagnifierActive // Toggle state

            if (isMagnifierActive) {
                zoomButton.text = "Disable Magnifier"
            } else {
                zoomButton.text = "Enable Magnifier"
                magnifier?.dismiss()
            }
        }

        // Move the magnifier while touching the screen
        imageView.setOnTouchListener { _, event ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isMagnifierActive) {
                when (event.action) {
                    MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                        magnifier?.show(event.rawX, event.y)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        magnifier?.dismiss()
                    }
                }
            }
            true
        }

        // Retrieve favorite color from firebase.
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                // Jeff code from profile activity
                for (document in documents) {
                    // Extract the favoriteColors list from the document
                    val colors = document.get("favoriteColors") as? List<Map<String, Any>> ?: emptyList()

                    // Process each color map
                    colors.mapNotNull { colorMap ->
                        val rgbMap = colorMap["rgb"] as? Map<String, Long>

                        // Safely get the RGB values, default to 0 if null
                        val r = rgbMap?.get("r")?.toInt() ?: 0
                        val g = rgbMap?.get("g")?.toInt() ?: 0
                        val b = rgbMap?.get("b")?.toInt() ?: 0

                        // Add the ARGB color value to rgbList
                        rgbList.add(Color.argb(255, r, g, b))
                        // You can return any value here if necessary (not required to return in mapNotNull)
                        null
                    }
                }

                // shuffle the array of favoriteColor
                rgbList.shuffle()
                // Remove duplicate colors after shuffle (popular colors are likely first)
                rgbList = rgbList.toMutableSet().toMutableList()
                // Set the colors from the rgbList
                for (i in 0 until minOf(rgbList.size, 25)) {
                    val resID = resources.getIdentifier("button${i + 1}", "id", packageName)
                    findViewById<Button>(resID)?.setBackgroundColor(rgbList[i])
                }

                // Set random colors for the remaining buttons starting from button 2
                for (i in rgbList.size until 25) {
                    val newRed = Random.nextInt(0, 256)
                    val newGreen = Random.nextInt(0, 256)
                    val newBlue = Random.nextInt(0, 256)

                    // Create a random color
                    val color = Color.argb(255, newRed, newGreen, newBlue)

                    // Find the view by its ID and set the background color
                    val resID = resources.getIdentifier("button${i + 1}", "id", packageName)
                    findViewById<Button>(resID)?.setBackgroundColor(color)
                }
            }
        //rgbList.add(Color.argb(255, 255, 0, 0)) // Red

        // Handle color selection from buttons
        for (i in 1..25) {
            val buttonId = resources.getIdentifier("button$i", "id", packageName)
            val button = findViewById<Button>(buttonId)
            // press any 1 - 25 button
            button?.setOnClickListener {
                val backgroundColor = (button.background as ColorDrawable).color
                val colorHex = String.format("#%06X", 0xFFFFFF and backgroundColor)
                val viewColor: View = findViewById(R.id.viewColor11)

                // Set the TextView text to show the button's background color
                hexMessage.text = "Hex: $colorHex"
                // Get rgb value
                val red = Color.red(backgroundColor)
                val green = Color.green(backgroundColor)
                val blue = Color.blue(backgroundColor)
                textRGB.text = "RGB: ($red, $green, $blue)"

                viewColor.setBackgroundColor(Color.argb(Color.alpha(backgroundColor), red, green, blue))

                // CHANGE WALL COLOR
                val newColor = Color.parseColor(colorHex)
                val modifiedBitmap = fillRegionWithColor(bitmap, targetRegion, newColor)
                imageView.setImageBitmap(modifiedBitmap)
            }
        }
    }

    // Function to fill a specific region with a new color
    fun fillRegionWithColor(bitmap: Bitmap, region: Rect, newColor: Int): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        for (x in region.left until region.right) {
            for (y in region.top until region.bottom) {
                mutableBitmap.setPixel(x, y, newColor)
            }
        }

        return mutableBitmap
    }

    // Data class for defining a rectangular area
    data class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int)

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
        }
    }

    fun parseColorString(colorString: String): Triple<Int, Int, Int> {
        // Remove the curly braces and trim the string
        val cleanedString = colorString.removePrefix("{").removeSuffix("}")

        // Use regex to extract the numbers after "r=", "g=", and "b="
        val regex = Regex("""r=(\d+),\s*b=(\d+),\s*g=(\d+)""")
        val matchResult = regex.find(cleanedString)

        return if (matchResult != null) {
            val (r, b, g) = matchResult.destructured
            Triple(r.toInt(), g.toInt(), b.toInt()) // Reorder to correct RGB order
        } else {
            throw IllegalArgumentException("Invalid color string format")
        }
    }
}
