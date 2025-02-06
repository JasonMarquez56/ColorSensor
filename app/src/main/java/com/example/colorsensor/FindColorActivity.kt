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
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class FindColorActivity : AppCompatActivity() {

    private lateinit var bitmap: Bitmap

    private val imageView: ImageView by lazy { findViewById(R.id.imageView) }
    private val viewColor: View by lazy { findViewById(R.id.viewColor) }
    private val viewColor2: View by lazy { findViewById(R.id.viewColor2) }
    private val viewColor3: View by lazy { findViewById(R.id.viewColor3) }
    private val viewColor4: View by lazy { findViewById(R.id.viewColor4) }
    private val viewColor5: View by lazy { findViewById(R.id.viewColor5) }
    private val viewColor6: View by lazy { findViewById(R.id.viewColor6) }
    private val viewColor7: View by lazy { findViewById(R.id.viewColor7) }
    private val viewColor8: View by lazy { findViewById(R.id.viewColor8) }
    private val viewColor9: View by lazy { findViewById(R.id.viewColor9) }
    private val viewColor10: View by lazy { findViewById(R.id.viewColor10) }
    private val textHex: TextView by lazy { findViewById(R.id.textView) }
    private val textRGB: TextView by lazy { findViewById(R.id.textView2) }

    private var xRatioForBitmap = 1f
    private var yRatioForBitmap = 1f

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.color_sensor)

        // Get image from intent
        val byteArray = intent.getByteArrayExtra("image_bitmap")
        val imageUri = intent.getStringExtra("image_uri")

        bitmap = when {
            byteArray != null -> BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            imageUri != null -> loadBitmapFromUri(Uri.parse(imageUri)) ?: getDefaultBitmap()
            else -> getDefaultBitmap()
        }

        imageView.setImageBitmap(bitmap)

        // Adjust touch coordinates based on image scaling
        imageView.post {
            xRatioForBitmap = bitmap.width.toFloat() / imageView.width.toFloat()
            yRatioForBitmap = bitmap.height.toFloat() / imageView.height.toFloat()
        }

        // Set touch listener for color detection
        imageView.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN ||
                motionEvent.action == MotionEvent.ACTION_MOVE ||
                motionEvent.action == MotionEvent.ACTION_UP) {

                val touchXtoBitmap = motionEvent.x * xRatioForBitmap
                val touchYtoBitmap = motionEvent.y * yRatioForBitmap

                if (touchXtoBitmap in 0f..bitmap.width.toFloat() &&
                    touchYtoBitmap in 0f..bitmap.height.toFloat()) {

                    val pixel = bitmap.getPixel(touchXtoBitmap.toInt(), touchYtoBitmap.toInt())

                    val red = Color.red(pixel)
                    val green = Color.green(pixel)
                    val blue = Color.blue(pixel)
                    val alpha = Color.alpha(pixel)

                    viewColor.setBackgroundColor(Color.argb(alpha, red, green, blue))

                    var step = 15 // Adjust this value to control the lightening effect
                    for (i in 2..6) { // Loop through viewColor2 to viewColor10
                        val newRed = Math.max(0, red - (i - 2) * step)
                        val newGreen = Math.max(0, green - (i - 2) * step)
                        val newBlue = Math.max(0, blue - (i - 2) * step)

                        val color = Color.argb(alpha, newRed, newGreen, newBlue)


                        // Set background color dynamically
                        val resID = resources.getIdentifier("viewColor$i", "id", packageName)
                        val childView = findViewById<View>(resID)
                        childView?.setBackgroundColor(color)
                    }
                    step = 5
                    for (i in 7..10) { // Loop through viewColor2 to viewColor10
                        val newRed = Math.min(255, red + (i - 2) * step)
                        val newGreen = Math.min(255, green + (i - 2) * step)
                        val newBlue = Math.min(255, blue + (i - 2) * step)

                        val color = Color.argb(alpha, newRed, newGreen, newBlue)

                        // Set background color dynamically
                        val resID = resources.getIdentifier("viewColor$i", "id", packageName)
                        val childView = findViewById<View>(resID)
                        childView?.setBackgroundColor(color)
                    }
                    textHex.text = "Hex: #${Integer.toHexString(pixel).uppercase()}"
                    textRGB.text = "RGB: ($red, $green, $blue)"
                }
            }
            true
        }
    }

    // Convert Uri to Bitmap
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }
        } catch (e: IOException) {
            Log.e("FindColorActivity", "Error loading bitmap from Uri: $uri", e)
            null
        }
    }

    // Returns the default bitmap if no image is provided
    private fun getDefaultBitmap(): Bitmap {
        return BitmapFactory.decodeResource(resources, R.drawable.colorsensor_home_banner)
    }
}
