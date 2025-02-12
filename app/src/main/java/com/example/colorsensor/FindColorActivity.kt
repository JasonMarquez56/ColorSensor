package com.example.colorsensor

import android.annotation.SuppressLint
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
    private lateinit var firestore: FirebaseFirestore

    private val imageView: ImageView by lazy { findViewById(R.id.imageView) }
    private val viewColor: View by lazy { findViewById(R.id.viewColor) }
    private val textHex: TextView by lazy { findViewById(R.id.textView) }
    private val textRGB: TextView by lazy { findViewById(R.id.textView2) }
    private val textName: TextView by lazy { findViewById(R.id.textView8) }
    private var closestColorName: String? = null
    private var xRatioForBitmap = 1f
    private var yRatioForBitmap = 1f

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.color_sensor)

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
            if (this::bitmap.isInitialized) {
                xRatioForBitmap = bitmap.width.toFloat() / imageView.width.toFloat()
                yRatioForBitmap = bitmap.height.toFloat() / imageView.height.toFloat()
            }
        }

        // Set touch listener for color detection
        imageView.setOnTouchListener { _, motionEvent ->
            if (this::bitmap.isInitialized) {
                val touchXtoBitmap = motionEvent.x * xRatioForBitmap
                val touchYtoBitmap = motionEvent.y * yRatioForBitmap

                if (touchXtoBitmap in 0f..bitmap.width.toFloat() &&
                    touchYtoBitmap in 0f..bitmap.height.toFloat()
                ) {
                    try {
                        val pixel = bitmap.getPixel(touchXtoBitmap.toInt(), touchYtoBitmap.toInt())
                        val red = Color.red(pixel)
                        val green = Color.green(pixel)
                        val blue = Color.blue(pixel)
                        val alpha = Color.alpha(pixel)

                        viewColor.setBackgroundColor(Color.argb(alpha, red, green, blue))

                        // Additional color processing
                        var step = 15
                        for (i in 2..6) {
                            val newRed = 0.coerceAtLeast(red - (i - 2) * step)
                            val newGreen = 0.coerceAtLeast(green - (i - 2) * step)
                            val newBlue = 0.coerceAtLeast(blue - (i - 2) * step)
                            val color = Color.argb(alpha, newRed, newGreen, newBlue)

                            val resID = resources.getIdentifier("viewColor$i", "id", packageName)
                            findViewById<View>(resID)?.setBackgroundColor(color)
                        }

                        step = 5
                        for (i in 7..10) {
                            val newRed = 255.coerceAtMost(red + (i - 2) * step)
                            val newGreen = 255.coerceAtMost(green + (i - 2) * step)
                            val newBlue = 255.coerceAtMost(blue + (i - 2) * step)
                            val color = Color.argb(alpha, newRed, newGreen, newBlue)

                            val resID = resources.getIdentifier("viewColor$i", "id", packageName)
                            findViewById<View>(resID)?.setBackgroundColor(color)
                        }

                        textHex.text = "Hex: #${Integer.toHexString(pixel).uppercase().substring(2)}"
                        textRGB.text = "RGB: ($red, $green, $blue)"

                        // Search the closest color when user lifts their finger
                        if (motionEvent.action == MotionEvent.ACTION_UP) {
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

    @SuppressLint("SetTextI18n")
    private fun searchClosestColor(targetRed: Int, targetGreen: Int, targetBlue: Int) {
        firestore.collection("paints")
            .get()
            .addOnSuccessListener { documents ->
                var closestColorDistance = Double.MAX_VALUE
                var closestColorName: String? = null
                var closestColorHex: String? = null

                if (documents.isEmpty) {
                    textName.text = "Color not found"
                } else {
                    for (document in documents) {
                        val colorName = document.getString("name")
                        val colorHex = document.getString("hex")

                        val regex = Regex("rgb\\((\\d+), (\\d+), (\\d+)\\)")
                        val matchResult = regex.find(colorHex ?: "")
                        if (matchResult != null) {
                            val dbRed = matchResult.groupValues[1].toInt()
                            val dbGreen = matchResult.groupValues[2].toInt()
                            val dbBlue = matchResult.groupValues[3].toInt()

                            val distance = sqrt(
                                (targetRed - dbRed).toDouble().pow(2.0) +
                                        (targetGreen - dbGreen).toDouble().pow(2.0) +
                                        (targetBlue - dbBlue).toDouble().pow(2.0)
                            )

                            if (distance < closestColorDistance) {
                                closestColorDistance = distance
                                closestColorName = colorName
                                closestColorHex = colorHex
                            }
                        }
                    }

                    textName.text = closestColorName?.let {
                        "Closest color: $it \n($closestColorHex)"
                    } ?: "Color not found"
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
