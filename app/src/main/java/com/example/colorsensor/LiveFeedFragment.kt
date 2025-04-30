package com.example.colorsensor

import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import android.util.Log
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.colorsensor.utils.PaintFinder
import androidx.core.graphics.toColorInt

class LiveFeedFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_live_feed, container, false)
    }

    private lateinit var cameraPreview: PreviewView
    private lateinit var colorDisplay: TextView
    private lateinit var colorPreviewBox: Button

    // Unique request code for permissions
    private val CAMERA_REQUEST_CODE = 101

    @SuppressLint("ClickableViewAccessibility", "UseKtx", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        cameraPreview = view.findViewById(R.id.camera_preview)
        colorDisplay = view.findViewById(R.id.color_display)
        colorPreviewBox = view.findViewById(R.id.color_preview_box)

        // Check and request camera permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            startCamera()
        }

        // Handle touch event to extract color
        cameraPreview.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val bitmap = cameraPreview.bitmap
                if (bitmap != null) {
                    val x = event.x.toInt()
                    val y = event.y.toInt()
                    if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                        val pixel = bitmap.getPixel(x, y)

                        val selectedPaintColor = PaintFinder.PaintColor(
                            brand = "",
                            name = "",
                            r = Color.red(pixel),
                            g = Color.green(pixel),
                            b = Color.blue(pixel)
                        )

                        val closestPaint = PaintFinder.findClosestPaint(selectedPaintColor, requireContext())

                        colorPreviewBox.setOnClickListener {
                            val colorName = closestPaint?.name
                            val r = closestPaint?.r
                            val g = closestPaint?.g
                            val b = closestPaint?.b
                            val hexValue = String.format("#%02X%02X%02X", r, g, b)

                            try {
                                val color = hexValue.toColorInt()
                                val intent = Intent(requireContext(), PaintInfoActivity::class.java).apply {
                                    putExtra("selected_color", color)
                                    putExtra("color_name", colorName)
                                    putExtra("color_hex", hexValue)
                                }
                                Log.d("DEBUG", "Starting PaintInfoActivity with colorHex: $hexValue, colorName: $colorName")
                                startActivity(intent)
                            } catch (e: IllegalArgumentException) {
                                Log.e("ERROR", "Invalid hex color format: $hexValue")
                                Toast.makeText(requireContext(), "Invalid color format", Toast.LENGTH_SHORT).show()
                            }
                        }

                        requireActivity().runOnUiThread {
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
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Ensure no duplicate bindings
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview)

                Log.d("LiveFeedActivity", "Camera started successfully")

            } catch (e: Exception) {
                Log.e("LiveFeedActivity", "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Handle the result of the camera permission request
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission denied. Please allow access to use the camera.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}