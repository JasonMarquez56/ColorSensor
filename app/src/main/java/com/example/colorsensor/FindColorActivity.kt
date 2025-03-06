package com.example.colorsensor

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Magnifier
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.lang.ref.WeakReference
import com.example.colorsensor.utils.PaintFinder
import com.example.colorsensor.RegisterActivity.RGB
import com.example.colorsensor.RegisterActivity.favColor
// import for popupWindow
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.widget.LinearLayout
import android.widget.PopupWindow
import java.io.ByteArrayOutputStream


class FindColorActivity : AppCompatActivity() {

    private lateinit var bitmap: Bitmap
    private lateinit var firestore: FirebaseFirestore // reference to the database for color and username
    //Magnifer
    private var magnifier: Magnifier? = null
    private var isMagnifierActive = false // Track magnifier state


    // reference to the layout of an imageview to find color
    private val imageView: ImageView by lazy { findViewById(R.id.imageView) }
    // show what color has been changed by a view
    private val viewColor: View by lazy { findViewById(R.id.viewColor) }
    private val textHex: TextView by lazy { findViewById(R.id.textView) }
    private val textRGB: TextView by lazy { findViewById(R.id.textView2) }
    private val textName: TextView by lazy { findViewById(R.id.textView8) }
    private val textViewRGB: TextView by lazy { findViewById(R.id.textViewRGB) }
    // find the closest Color
    private var selectedColor = RGB(0,0,0)
    // the size of the imageview and bitmap we need to 2 variables
    private var xRatioForBitmap = 1f
    private var yRatioForBitmap = 1f

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.color_sensor)

        setupButtonClickListener()

        val byteArray = intent.getByteArrayExtra("image_bitmap")
        val imageUri = intent.getStringExtra("image_uri")
        val testButton = findViewById<Button>(R.id.button27)

        val favoriteButton = findViewById<Button>(R.id.favorite)
        firestore = FirebaseFirestore.getInstance()
        //favorite button to store selected color user's favorite list

        favoriteButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
            sharedPreferences.all
            //gets the username of current session.
            val username = sharedPreferences.getString("username", "Guest")
            //search through database with user name
            firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { documents ->
                    //if user name is successfully found.
                    if (!documents.isEmpty) {
                        for (document in documents) {
                            //finds user id
                            val userId = document.id
                            Toast.makeText(this, userId, Toast.LENGTH_SHORT).show()
                            val user = firestore.collection("users").document(userId)
                            val colorAdd = favColor(textName.text.toString().replace("Closest Paint: ",""),selectedColor)
                            //add favorite color.
                            user.update("favoriteColors", FieldValue.arrayUnion(colorAdd))
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Succeeded to create", Toast.LENGTH_SHORT)
                                        .show()
                                }//in case failed to update
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to create", Toast.LENGTH_SHORT)
                                        .show()
                                }
                        }
                        //if no user is found give warning
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

        // split image
        testButton.setOnClickListener {
            val intent = Intent(this, ImageSplitActivity::class.java)

            // Convert Bitmap to ByteArray
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            // Pass byte array through intent
            intent.putExtra("bitmap", byteArray)
            startActivity(intent)
        }

        imageView.post {
            // bitmap is initialized before finding color and update color strip
            if (this::bitmap.isInitialized) {
                xRatioForBitmap = bitmap.width.toFloat() / imageView.width.toFloat()
                yRatioForBitmap = bitmap.height.toFloat() / imageView.height.toFloat()
            }
        }

        // magnifier 133 - 153 line
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier = Magnifier.Builder(imageView)
                .setInitialZoom(2.3f)
                .setElevation(10.0f)
                .setCornerRadius(50.0f) // circle
                .setSize(100, 100)
                .build()
        }

        // Set touch listener for color detection
        // This is for color strip and find color using built-in android studio tools
        imageView.setOnTouchListener { _, motionEvent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                when (motionEvent.action) {
                    MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                        magnifier?.show(motionEvent.rawX, motionEvent.y)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        magnifier?.dismiss()
                    }
                }
            }

            // check the bitmap if it is isInitialized
            if (this::bitmap.isInitialized) {
                // New code 172 - 185
                // Get the actual image drawable bounds inside the ImageView
                val imageMatrixValues = FloatArray(9)
                imageView.imageMatrix.getValues(imageMatrixValues)

                // Extract scaling factors and translations
                val scaleX = imageMatrixValues[0]
                val scaleY = imageMatrixValues[4]
                val translateX = imageMatrixValues[2]
                val translateY = imageMatrixValues[5]

                // Convert touch coordinates to match the size of the imageview and bitmap
                val touchXtoBitmap = (motionEvent.x - translateX) / scaleX
                val touchYtoBitmap = (motionEvent.y - translateY) / scaleY

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
                            findViewById<View>(resID)?.setOnClickListener {
                                val colorStrip = (findViewById<View>(resID)?.background as ColorDrawable).color
                                val colorHex = String.format("#%06X", 0xFFFFFF and colorStrip)
                                val red = Color.red(colorStrip)
                                val green = Color.green(colorStrip)
                                val blue = Color.blue(colorStrip)
                                showPopup(it.context, it,"Hex: $colorHex\nRGB: ($red, $green, $blue)")
                            }
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
                            findViewById<View>(resID)?.setOnClickListener {
                                val colorStrip = (findViewById<View>(resID)?.background as ColorDrawable).color
                                val colorHex = String.format("#%06X", 0xFFFFFF and colorStrip)
                                val red = Color.red(colorStrip)
                                val green = Color.green(colorStrip)
                                val blue = Color.blue(colorStrip)
                                showPopup(it.context, it,"Hex: $colorHex\nRGB: ($red, $green, $blue)")
                            }
                        }
                        // update the text for Hex and RGB to the target color
                        textHex.text = "Hex: #${Integer.toHexString(pixel).uppercase().substring(2)}"
                        textRGB.text = "RGB: ($red, $green, $blue)"
                        selectedColor = RGB(red,green,blue)


                        // Calculating luminance with standard weighted formula
                        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
                        // Changing text color based off luminance, making it more visible
                        val textColor = if (luminance > 0.5) Color.BLACK else Color.WHITE
                        // Changing color of text fields
                        textRGB.setTextColor(textColor)
                        textHex.setTextColor(textColor)
                        textName.setTextColor(textColor)
                        textViewRGB.setTextColor(textColor)

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
        val targetColor = PaintFinder.PaintColor("", "", targetRed, targetGreen, targetBlue)
        val closestPaint = PaintFinder.findClosestPaint(targetColor, this)
        // Setting XML values to correct paint and RGB when found
        if (closestPaint != null) {
            val closestRGB = "(${closestPaint.r}, ${closestPaint.g}, ${closestPaint.b})"
            textName.text = "Closest Paint: ${closestPaint.name}"
            textViewRGB.text = "RGB: $closestRGB"
        } else {
            textName.text = "No matching paint found"
            textViewRGB.text = ""
        }
    }


    private fun setupButtonClickListener() {
        val viewColor = findViewById<View>(R.id.viewColor)

        viewColor.setOnClickListener {
            val colorName = textName.text.toString()
            val rgbText = textViewRGB.text.toString()

            Log.d("DEBUG", "setupButtonClickListener - textName: $colorName, textViewRGB: $rgbText")

            // Ensure a valid color is selected before proceeding
            if (colorName.contains("No matching paint found") || rgbText.isEmpty()) {
                Log.e("ERROR", "setupButtonClickListener - No color loaded!")
                Toast.makeText(this, "No color loaded", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Extract RGB values from the text "(R, G, B)"
            val regex = Regex("\\((\\d+), (\\d+), (\\d+)\\)")
            val matchResult = regex.find(rgbText)

            if (matchResult != null) {
                val red = matchResult.groupValues[1].toInt()
                val green = matchResult.groupValues[2].toInt()
                val blue = matchResult.groupValues[3].toInt()
                val hexColor = String.format("#%02X%02X%02X", red, green, blue)

                try {
                    val color = Color.parseColor(hexColor)
                    val intent = Intent(this, PaintInfoActivity::class.java).apply {
                        putExtra("selected_color", color)
                        putExtra("color_name", colorName.replace("Closest Paint: ", ""))
                        putExtra("color_hex", hexColor)
                    }

                    Log.d("DEBUG", "Starting PaintInfoActivity with colorHex: $hexColor, colorName: ${colorName.replace("Closest Paint: ", "")}")
                    startActivity(intent)
                } catch (e: IllegalArgumentException) {
                    Log.e("ERROR", "Invalid hex color format: $hexColor")
                    Toast.makeText(this, "Invalid color format", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("ERROR", "RGB format is incorrect: $rgbText")
                Toast.makeText(this, "Error processing color", Toast.LENGTH_SHORT).show()
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

    fun showPopup(context: Context, anchorView: View, message: String) {
        // create a LinearLayout to serve as the popup content
        val popupView = LinearLayout(context)
        popupView.orientation = LinearLayout.VERTICAL
        popupView.setPadding(20, 20, 20, 20)

        // display the message
        val textView = TextView(context)
        textView.text = message
        textView.textSize = 18f
        textView.setTextColor(Color.BLACK)

        // add textView to the layout
        popupView.addView(textView)

        // create a popupWindow using the dynamic layout
        val popupWindow = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)

        // show the popupWindow
        popupWindow.showAsDropDown(anchorView, 0, 0)
    }
}
