package com.example.colorsensor

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.math.pow
import kotlin.math.sqrt

class FindColorActivity : AppCompatActivity() {

    private lateinit var bitmap: Bitmap
    private lateinit var firestore: FirebaseFirestore // reference to the database for color and username

    // reference to the layout of an imageview to find color
    private val imageView: ImageView by lazy { findViewById(R.id.imageView) }
    // show what color has been changed by a view
    private val viewColor: View by lazy { findViewById(R.id.viewColor) }
    private val textHex: TextView by lazy { findViewById(R.id.textView) }
    private val textRGB: TextView by lazy { findViewById(R.id.textView2) }
    private val textName: TextView by lazy { findViewById(R.id.textView8) }
    // find the closest Color
    private var closestColorName: String? = null
    private var closestColorHex: String? = null
    // the size of the imageview and bitmap we need to 2 variables
    private var xRatioForBitmap = 1f
    private var yRatioForBitmap = 1f

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.color_sensor)

        setupButtonClickListener()

        val byteArray = intent.getByteArrayExtra("image_bitmap")
        val imageUri = intent.getStringExtra("image_uri")

        val favoriteButton = findViewById<Button>(R.id.favorite)
        firestore = FirebaseFirestore.getInstance()

        favoriteButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
            sharedPreferences.all
            val username = sharedPreferences.getString("username", "Guest")
            firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        for (document in documents) {
                            val userId = document.id
                            Toast.makeText(this, userId, Toast.LENGTH_SHORT).show()
                            val user = firestore.collection("users").document(userId)

                            user.update("favoriteColors", FieldValue.arrayUnion(closestColorName))
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Succeeded to create", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to create", Toast.LENGTH_SHORT)
                                        .show()
                                }
                        }
                    } else {
                        Log.d("Firestore", "No user found with username: $username")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting documents: ", exception)
                }
            return@setOnClickListener
        }

        // Load bitmap from byte array or URI with WeakReference
        bitmap = when {
            byteArray != null -> BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            imageUri != null -> loadBitmapFromUri(Uri.parse(imageUri)) ?: getDefaultBitmap()
            else -> getDefaultBitmap()
        }

        // Wrap the bitmap in a WeakReference
        val weakBitmap = WeakReference(bitmap)

        // If the bitmap is null, log the error and set a default image
        if (this::bitmap.isInitialized) {
            imageView.setImageBitmap(weakBitmap.get())  // Use the bitmap from the WeakReference
        } else {
            Log.e("FindColorActivity", "Error: Bitmap is not initialized properly.")
            imageView.setImageBitmap(getDefaultBitmap())
        }

        imageView.post {
            // bitmap is initialized before finding color and update color strip
            if (this::bitmap.isInitialized) {
                xRatioForBitmap = bitmap.width.toFloat() / imageView.width.toFloat()
                yRatioForBitmap = bitmap.height.toFloat() / imageView.height.toFloat()
            }
        }

        // Set touch listener for color detection
        // This is for color strip and find color using built-in android studio tools
        imageView.setOnTouchListener { _, motionEvent ->
            // check the bitmap if it is isInitialized
            if (this::bitmap.isInitialized) {
                // Convert touch coordinates to match the size of the imageview and bitmap
                val touchXtoBitmap = motionEvent.x * xRatioForBitmap
                val touchYtoBitmap = motionEvent.y * yRatioForBitmap

                // Ensure the touch coordinates are within the bitmap bounds
                if (touchXtoBitmap in 0f..bitmap.width.toFloat() &&
                    touchYtoBitmap in 0f..bitmap.height.toFloat()
                ) {
                    try {
                        // Get the pixel color at the touched position
                        val pixel = bitmap.getPixel(touchXtoBitmap.toInt(), touchYtoBitmap.toInt())
                        // Extract RGBA components from the pixel color
                        val red = Color.red(pixel)
                        val green = Color.green(pixel)
                        val blue = Color.blue(pixel)
                        val alpha = Color.alpha(pixel)
                        // update the viewColor background color
                        viewColor.setBackgroundColor(Color.argb(alpha, red, green, blue))

                        // Additional color processing
                        var step = 15 // Color strip get the darkest color use steps to update next rgb
                        for (i in 2..6) {
                            val newRed = 0.coerceAtLeast(red - (i - 2) * step)
                            val newGreen = 0.coerceAtLeast(green - (i - 2) * step)
                            val newBlue = 0.coerceAtLeast(blue - (i - 2) * step)
                            val color = Color.argb(alpha, newRed, newGreen, newBlue)
                            // Find corresponding view by ID and set the background color
                            // this avoid manually getting each viewColor by viewColor2 - 10
                            val resID = resources.getIdentifier("viewColor$i", "id", packageName)
                            findViewById<View>(resID)?.setBackgroundColor(color)
                        }

                        step = 5 // Color strip get the next lighter color use steps to update next rgb
                        for (i in 7..10) {
                            val newRed = 255.coerceAtMost(red + (i - 2) * step)
                            val newGreen = 255.coerceAtMost(green + (i - 2) * step)
                            val newBlue = 255.coerceAtMost(blue + (i - 2) * step)
                            val color = Color.argb(alpha, newRed, newGreen, newBlue)

                            // Find corresponding view by ID and set the background color
                            // this avoid manually getting each viewColor by viewColor2 - 10
                            val resID = resources.getIdentifier("viewColor$i", "id", packageName)
                            findViewById<View>(resID)?.setBackgroundColor(color) //set the background color
                        }
                        // update the text for Hex and RGB to the target color
                        textHex.text = "Hex: #${Integer.toHexString(pixel).uppercase().substring(2)}"
                        textRGB.text = "RGB: ($red, $green, $blue)"

                        // Search the closest color when user lifts their finger
                        if (motionEvent.action == MotionEvent.ACTION_UP) {
                            // call the function to update the textView -> textName
                            searchClosestColor(red, green, blue)
                        }

                    } catch (e: Exception) {
                        Log.e("FindColorActivity", "Error: $e")
                    }
                }
            }
            true
        }
    }

    // Function to search inside the database for color to find the closest matching color name
    // example: searchClosestColor(red, green, blue)
    @SuppressLint("SetTextI18n")
    private fun searchClosestColor(targetRed: Int, targetGreen: Int, targetBlue: Int) {
        // Our firebase database with the collection label paints store rgb and name.
        firestore.collection("paints")
            .get()
            .addOnSuccessListener { documents ->
                // variables name and rgb
                var closestColorDistance = Double.MAX_VALUE
                var closestColorName: String? = null
                var closestColorHex: String? = null

                if (documents.isEmpty) { // if empty exit
                    textName.text = "Color not found"
                } else {
                    // for loop through the document to find color
                    for (document in documents) {
                        // variables for name and rgb
                        val colorName = document.getString("name")
                        val colorHex = document.getString("hex")
                        // Regular expression to extract RGB values from the hex string
                        val regex = Regex("rgb\\((\\d+), (\\d+), (\\d+)\\)")
                        val matchResult = regex.find(colorHex ?: "")
                        // get the red, green, and blue of the rgb
                        if (matchResult != null) {
                            val dbRed = matchResult.groupValues[1].toInt()
                            val dbGreen = matchResult.groupValues[2].toInt()
                            val dbBlue = matchResult.groupValues[3].toInt()

                            // find the distance between the target and database by subtracting the distance
                            // searchClosestColor(red, green, blue) - matchResult
                            val distance = sqrt(
                                (targetRed - dbRed).toDouble().pow(2.0) +
                                        (targetGreen - dbGreen).toDouble().pow(2.0) +
                                        (targetBlue - dbBlue).toDouble().pow(2.0)
                            )
                            // if distance < closestColorDistance then update the closestColorDistance
                            if (distance < closestColorDistance) {
                                closestColorDistance = distance
                                closestColorName = colorName
                                closestColorHex = colorHex
                            }
                        }
                    }
                    // Update the text to show closestColorName
                    this.closestColorName = closestColorName
                    this.closestColorHex = closestColorHex
                    closestColorName?.let {
                        textName.text = "Closest color: $it \n$closestColorHex"
                    } ?: run {
                        textName.text = "Color not found"
                    }
                }
            }
    }

    private fun setupButtonClickListener() {
        val viewColor = findViewById<View>(R.id.viewColor)
        viewColor.setOnClickListener {
            val currentClosestColorHex = closestColorHex // Make a local copy to avoid smart cast issue
            Log.d("DEBUG", "setupButtonClickListener - closestColorHex: $currentClosestColorHex, closestColorName: $closestColorName")

            // Check if closestColorHex is null or empty
            if (currentClosestColorHex.isNullOrEmpty()) {
                Log.e("ERROR", "setupButtonClickListener - closestColorHex is null or empty!")
                Toast.makeText(this, "Color data is not loaded yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If closestColorHex is in RGB format, convert it to Hex
            val hexColor = if (currentClosestColorHex.startsWith("rgb")) {
                // Extract RGB values from the string using regex
                val regex = Regex("rgb\\((\\d+), (\\d+), (\\d+)\\)")
                val matchResult = regex.find(currentClosestColorHex)

                if (matchResult != null) {
                    // Convert RGB to hex
                    val red = matchResult.groupValues[1].toInt()
                    val green = matchResult.groupValues[2].toInt()
                    val blue = matchResult.groupValues[3].toInt()

                    // Format the hex string
                    String.format("#%02X%02X%02X", red, green, blue)
                } else {
                    currentClosestColorHex // If it's not valid RGB, use as is
                }
            } else {
                currentClosestColorHex // If it's already a valid hex, use it
            }

            try {
                val color = Color.parseColor(hexColor)
                val intent = Intent(this, PaintInfoActivity::class.java)

                intent.putExtra("selected_color", color)
                intent.putExtra("color_name", closestColorName)
                intent.putExtra("color_hex", hexColor)

                Log.d("DEBUG", "Starting PaintInfoActivity with colorHex: $hexColor, colorName: $closestColorName")
                startActivity(intent)
            } catch (e: IllegalArgumentException) {
                Log.e("ERROR", "Invalid hex color format: $hexColor")
                Toast.makeText(this, "Invalid color format", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                }
            } else {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    })
                }
            }
        } catch (e: IOException) {
            Log.e("FindColorActivity", "Error loading bitmap from Uri: $uri", e)
            null
        }
    }

    private fun getDefaultBitmap(): Bitmap {
        return BitmapFactory.decodeResource(resources, R.drawable.colorsensor_home_banner)
    }
}
