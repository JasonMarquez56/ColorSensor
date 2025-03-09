package com.example.colorsensor

import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import android.util.Log
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.widget.Button
import com.example.colorsensor.utils.PaintFinder


class LiveFeedActivity : AppCompatActivity() {
    private lateinit var cameraPreview: PreviewView
    private lateinit var colorDisplay: TextView
    private lateinit var colorPreviewBox: Button
    // Unique request code for permissions
    private val CAMERA_REQUEST_CODE = 101


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.live_feed)

        // Find views
        cameraPreview = findViewById(R.id.camera_preview)
        colorDisplay = findViewById(R.id.color_display)
        colorPreviewBox = findViewById(R.id.color_preview_box)

        // Check and request camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            startCamera()
        }


        // Handle touch event to extract color
        cameraPreview.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Capture current camera frame
                val bitmap = cameraPreview.bitmap
                if (bitmap != null) {
                    val x = event.x.toInt()
                    val y = event.y.toInt()
                    if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                        val pixel = bitmap.getPixel(x, y)

                        // Create PaintColor object with RGB values
                        val selectedPaintColor = PaintFinder.PaintColor(
                            brand = "",
                            name = "",
                            r = Color.red(pixel),
                            g = Color.green(pixel),
                            b = Color.blue(pixel)
                        )

                        // Find the closest matching paint color
                        val closestPaint = PaintFinder.findClosestPaint(selectedPaintColor, this)

                        // Set the click listener for the color preview button
                        colorPreviewBox.setOnClickListener {
                            // Assuming closestPaint is already defined and contains the selected color
                            val colorName = closestPaint?.name
                            val r = closestPaint?.r
                            val g = closestPaint?.g
                            val b = closestPaint?.b

                            // Convert RGB to Hex
                            val hexValue = String.format("#%02X%02X%02X", r, g, b)

                            // Try to create a valid color from the RGB values
                            try {
                                // Create a Color object using the hex value
                                val color = Color.parseColor(hexValue)

                                // Create an Intent to start the PaintInfoActivity
                                val intent = Intent(this, PaintInfoActivity::class.java).apply {
                                    // Pass the RGB color (as hex), color name, and hex value using putExtra
                                    putExtra("selected_color", color)
                                    putExtra("color_name", colorName)
                                    putExtra("color_hex", hexValue)
                                }

                                // Log the information being passed for debugging
                                Log.d("DEBUG", "Starting PaintInfoActivity with colorHex: $hexValue, colorName: $colorName")

                                // Start the PaintInfoActivity
                                startActivity(intent)
                            } catch (e: IllegalArgumentException) {
                                // Handle the case where the hex value is invalid
                                Log.e("ERROR", "Invalid hex color format: $hexValue")
                                Toast.makeText(this, "Invalid color format", Toast.LENGTH_SHORT).show()
                            }
                        }
                        // Display the closest paint color's name
                        runOnUiThread {
                            colorDisplay.text = "Selected Color: ${closestPaint?.name ?: "No Match"}"
                            colorPreviewBox.setBackgroundColor(pixel)
                        }
                    }
                } else {
                    Log.e("LiveFeedActivity", "Bitmap is null, cannot extract color")
                }
            }
            true
        }

    }


    // Start camera after permission is granted
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Ensure no duplicate bindings
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)

                Log.d("LiveFeedActivity", "Camera started successfully")

            } catch (e: Exception) {
                Log.e("LiveFeedActivity", "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Handle the result of the camera permission request
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission denied. Please allow access to use the camera.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}