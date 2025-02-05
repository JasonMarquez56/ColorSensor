package com.example.colorsensor

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.lang.String

class FindColorActivity : AppCompatActivity() {

    private lateinit var bitmap: Bitmap

    private val imageView: ImageView by lazy { findViewById(R.id.imageView) }
    private val viewColor: View by lazy { findViewById(R.id.viewColor) }
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
