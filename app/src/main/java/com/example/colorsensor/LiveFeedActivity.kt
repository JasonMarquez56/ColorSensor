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
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.view.View


class LiveFeedActivity : AppCompatActivity() {
    private lateinit var cameraPreview: PreviewView
    private lateinit var colorDisplay: TextView
    private lateinit var colorPreviewBox: View
    private val CAMERA_REQUEST_CODE = 101  // Unique request code for permissions

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
                val bitmap = cameraPreview.bitmap  // Capture current camera frame
                if (bitmap != null) {
                    val x = event.x.toInt()
                    val y = event.y.toInt()
                    if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                        val pixel = bitmap.getPixel(x, y)
                        val hexColor = String.format("#%02X%02X%02X",
                            Color.red(pixel), Color.green(pixel), Color.blue(pixel))

                        colorDisplay.text = "Selected Color: $hexColor"

                        colorPreviewBox.setBackgroundColor(pixel)
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

                cameraProvider.unbindAll()  // Ensure no duplicate bindings
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
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            Log.e("LiveFeedActivity", "Camera permission denied")
        }
    }
}