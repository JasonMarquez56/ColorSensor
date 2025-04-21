package com.example.colorsensor

import android.content.Intent
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
import androidx.core.graphics.ColorUtils

import com.example.colorsensor.SettingsUtil
import com.google.android.material.bottomnavigation.BottomNavigationView

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
        navigationBar()

//        // These 2 lines of code test the settingActivity
//        val textView = findViewById<TextView>(R.id.textView17)
//        SettingsUtil.updateTextViewBasedOnSettings(this, textView)

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
        val disappearImage = findViewById<ImageView>(R.id.imageView5)
        zoomButton.setOnClickListener {
            isMagnifierActive = !isMagnifierActive // Toggle state
            // toggle image to go away
            disappearImage.visibility = View.GONE

            if (isMagnifierActive) {
                zoomButton.text = "Disable Magnifier"
            } else {
                zoomButton.text = "Magnifier"
                disappearImage.visibility = View.VISIBLE
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

                // accessbility mode
                val accessbility: View by lazy { findViewById(R.id.viewColor12) }
                val accessbilityText: TextView by lazy { findViewById(R.id.textViewAccessbilityName) }
                val accessbilityHex: TextView by lazy { findViewById(R.id.textViewAccessbility) }
                // Set to default blank
                accessbility.setBackgroundColor(Color.WHITE)
                accessbilityHex.text = ""
                accessbilityText.text = ""
                when {
                    SettingsUtil.isProtanomalyEnabled(this) -> {
                        val protanopiaColor = SettingsUtil.hexToProtanomalyHex(red, green, blue)
                        accessbility.setBackgroundColor(Color.parseColor(protanopiaColor))
                        accessbilityHex.text = "Hex: ${protanopiaColor.uppercase()}"
                        accessbilityText.text = "Protanomaly (Red-Blind)"
                    }

                    SettingsUtil.isDeuteranomalyEnabled(this) -> {
                        val deuteranomalyColor =
                            SettingsUtil.hexToDeuteranomalyHex(red, green, blue)
                        accessbility.setBackgroundColor(Color.parseColor(deuteranomalyColor))
                        accessbilityHex.text = "Hex: ${deuteranomalyColor.uppercase()}"
                        accessbilityText.text = "Deuteranomaly"
                    }

                    SettingsUtil.isTritanomalyEnabled(this) -> {
                        val tritanomalyColor = SettingsUtil.hexToTritanomalyHex(red, green, blue)
                        accessbility.setBackgroundColor(Color.parseColor(tritanomalyColor))
                        accessbilityHex.text = "Hex: ${tritanomalyColor.uppercase()}"
                        accessbilityText.text = "Tritanomaly"
                    }
                }

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

        // Storing the RGB values of the selected color
        val pr = Color.red(newColor)
        val pg = Color.green(newColor)
        val pb = Color.blue(newColor)

        // Create an array to hold the pixels for each row
        val pixels = IntArray(region.right - region.left)

        // Process each row individually
        for (y in region.top until region.bottom) {
            // Get the pixels of the current row
            bitmap.getPixels(pixels, 0, region.right - region.left, region.left, y, region.right - region.left, 1)

            // Process each pixel in the row
            for (x in 0 until pixels.size) {
                val pixel = pixels[x]

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Calculating a more realistic brightness
                val brightness = (r + g + b) / 3f / 255f

                // Setting the RGB values with respect to the new blended color
                val newR = (pr * brightness).toInt().coerceIn(0, 255)
                val newG = (pg * brightness).toInt().coerceIn(0, 255)
                val newB = (pb * brightness).toInt().coerceIn(0, 255)

                val newColor = Color.rgb(newR, newG, newB)

                // Update the pixel color in the array
                pixels[x] = newColor
            }

            // Set the modified pixels back to the bitmap
            mutableBitmap.setPixels(pixels, 0, region.right - region.left, region.left, y, region.right - region.left, 1)
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
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.settings -> {
                    // Handle Settings button click
                    val intent = Intent(this, SettingActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
}
