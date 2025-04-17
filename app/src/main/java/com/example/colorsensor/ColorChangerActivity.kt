package com.example.colorsensor

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ColorPickerDialogFragment
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.View
import com.example.colorsensor.utils.PaintFinder
import android.view.MotionEvent
import android.widget.ProgressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import java.util.*
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc


class ColorChangerActivity : AppCompatActivity(), ColorPickerDialogFragment.OnColorSelectedListener {

    private lateinit var rgbValueText: TextView
    private lateinit var imageView: ImageView
    private lateinit var colorsButton: Button
    private lateinit var undoButton: Button
    private lateinit var resetButton: Button
    private lateinit var originalBitmap: Bitmap
    private lateinit var modifiedBitmap: Bitmap
    private lateinit var colorBox: View
    private var selectedColor: Int = Color.WHITE
    private val bitmapHistory: Stack<Bitmap> = Stack()

    companion object {
        init {
            System.loadLibrary("opencv_java4")  // load native lib
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.color_changer)

        // Initializing views for xml
        rgbValueText = findViewById(R.id.rgbValueText)
        imageView = findViewById(R.id.imageView)
        colorsButton = findViewById(R.id.colorsButton)
        undoButton = findViewById(R.id.undoButton)
        resetButton = findViewById(R.id.resetButton)
        colorBox = findViewById(R.id.colorBox)
        val spinner = findViewById<ProgressBar>(R.id.loadingSpinner)

        // Retrieve the image URI from intent (passed from ColorFinder)
        val imageUriString = intent.getStringExtra("image_uri")
        if (imageUriString == null) {
            Log.e("ColorChangerActivity", "No imageUri provided")
            finish()
            return
        }

        // Parsing image to bitmap
        try {
            val imageUri = Uri.parse(imageUriString)
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e("ColorChangerActivity", "Failed to decode bitmap from URI")
                finish()
                return
            }

            originalBitmap = bitmap
            val config = originalBitmap.config ?: Bitmap.Config.ARGB_8888
            modifiedBitmap = originalBitmap.copy(config, true)

            // Edge detection test
            modifiedBitmap = applyEdgeDetection(originalBitmap)

            // Remove edge detection if displaying normal image
            imageView.setImageBitmap(modifiedBitmap)

        } catch (e: Exception) {
            Log.e("ColorChangerActivity", "Error loading image from URI", e)
            finish()
        }

        // Open color wheel when button is clicked
        colorsButton.setOnClickListener {
            val dialog = ColorPickerDialogFragment()
            dialog.show(supportFragmentManager, "ColorPickerDialog")
        }

        // Detecting taps on the image
        imageView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imageMatrix = imageView.imageMatrix
                val drawable = imageView.drawable ?: return@setOnTouchListener true

                val inverse = android.graphics.Matrix()
                imageMatrix.invert(inverse)

                val touchPoint = floatArrayOf(event.x, event.y)
                inverse.mapPoints(touchPoint)

                val x = touchPoint[0].toInt()
                val y = touchPoint[1].toInt()

                if (x in 0 until modifiedBitmap.width && y in 0 until modifiedBitmap.height) {
                    val tappedColor = modifiedBitmap.getPixel(x, y)
                    Log.d("ColorChangerActivity", "Tapped Color: $tappedColor at ($x, $y)")

                    // Save the current bitmap before making changes for undo functionality
                    bitmapHistory.push(modifiedBitmap.copy(modifiedBitmap.config ?: Bitmap.Config.ARGB_8888, true))


                    // Launch on background thread to avoid freezing the UI
                    lifecycleScope.launch {
                        // Show a temporary loading spinner
                        spinner.visibility = View.VISIBLE

                        val updatedBitmap = withContext(Dispatchers.Default) {
                            // Replace similar pixels with selected color
                            replaceColorInBitmap(modifiedBitmap, tappedColor, selectedColor)
                        }

                        // Update the UI on the main thread
                        imageView.setImageBitmap(updatedBitmap)
                        modifiedBitmap = updatedBitmap

                        // Removing spinner when done loading
                        spinner.visibility = View.GONE
                    }
                }
            }
            true
        }

        // Undo button
        undoButton.setOnClickListener {
            if (bitmapHistory.isNotEmpty()) {
                // Pop the last bitmap from history and set it back
                modifiedBitmap = bitmapHistory.pop()
                imageView.setImageBitmap(modifiedBitmap)
            }
        }

        // Reset button
        resetButton.setOnClickListener {
            modifiedBitmap = originalBitmap.copy(originalBitmap.config ?: Bitmap.Config.ARGB_8888, true)
            imageView.setImageBitmap(modifiedBitmap)
            // Clear history to reset to original
            bitmapHistory.clear()
        }
    }

    override fun onColorSelected(color: Int) {
        selectedColor = color
        colorBox.setBackgroundColor(color)

        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        val targetColor = PaintFinder.PaintColor("Selected", "Current", r, g, b)
        val closestPaint = PaintFinder.findClosestPaint(targetColor, this)

        rgbValueText.text = if (closestPaint != null) {
            "Closest Paint: ${closestPaint.name}"
        } else {
            "No close match found"
        }

        updateColorInfo(color, colorBox)
        colorBox.setBackgroundColor(color)
    }

    // Determines if two colors are similar within a given tolerance (passed from replaceColor below)
    private fun isColorSimilar(color1: Int, color2: Int, tolerance: Int): Boolean {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        return (Math.abs(r1 - r2) < tolerance &&
                Math.abs(g1 - g2) < tolerance &&
                Math.abs(b1 - b2) < tolerance)
    }

    private fun replaceColorInBitmap(
        bitmap: Bitmap,
        targetColor: Int,
        newColor: Int,
        opacity: Int = 200,
        tolerance: Int = 80
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)

        val targetHSV = FloatArray(3)
        Color.colorToHSV(targetColor, targetHSV)

        val newHSV = FloatArray(3)
        Color.colorToHSV(newColor, newHSV)

        val pixels = IntArray(width)
        val pixelHSV = FloatArray(3)

        for (y in 0 until height) {
            bitmap.getPixels(pixels, 0, width, 0, y, width, 1)

            for (x in 0 until width) {
                val pixelColor = pixels[x]

                if (isColorSimilar(pixelColor, targetColor, tolerance)) {
                    // Convert the original pixel to HSV
                    Color.colorToHSV(pixelColor, pixelHSV)

                    // Keep brightness, replace hue & saturation
                    pixelHSV[0] = newHSV[0] // Hue
                    pixelHSV[1] = newHSV[1] // Saturation
                    // pixelHSV[2] stays the same (brightness)

                    // Convert back to ARGB with original alpha
                    val modifiedColor = Color.HSVToColor(Color.alpha(pixelColor), pixelHSV)

                    // Blend it softly with opacity
                    val blendedColor = blendColors(pixelColor, modifiedColor, opacity)
                    pixels[x] = blendedColor
                }
            }

            newBitmap.setPixels(pixels, 0, width, 0, y, width, 1)
        }

        return newBitmap
    }

    private fun blendColors(originalColor: Int, newColor: Int, opacity: Int): Int {
        val originalAlpha = Color.alpha(originalColor)
        val newAlpha = opacity

        val blendedAlpha = ((originalAlpha * (255 - newAlpha)) + (newAlpha * newAlpha)) / 255

        val r = (Color.red(originalColor) * (255 - opacity) + Color.red(newColor) * opacity) / 255
        val g = (Color.green(originalColor) * (255 - opacity) + Color.green(newColor) * opacity) / 255
        val b = (Color.blue(originalColor) * (255 - opacity) + Color.blue(newColor) * opacity) / 255

        return Color.argb(blendedAlpha, r, g, b)
    }

    fun applyEdgeDetection(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

        // Apply Sobel
        val gradX = Mat()
        val gradY = Mat()
        Imgproc.Sobel(gray, gradX, CvType.CV_16S, 1, 0)
        Imgproc.Sobel(gray, gradY, CvType.CV_16S, 0, 1)

        val absGradX = Mat()
        val absGradY = Mat()
        Core.convertScaleAbs(gradX, absGradX)
        Core.convertScaleAbs(gradY, absGradY)

        val sobelEdges = Mat()
        Core.addWeighted(absGradX, 0.5, absGradY, 0.5, 0.0, sobelEdges)

        // Convert to 3-channel
        val edgesColor = Mat()
        Imgproc.cvtColor(sobelEdges, edgesColor, Imgproc.COLOR_GRAY2BGR)

        val edgeBitmap = Bitmap.createBitmap(edgesColor.cols(), edgesColor.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(edgesColor, edgeBitmap)

        return edgeBitmap
    }


    private fun updateColorInfo(
        color: Int,
        colorBox: View
    ) {
        // Extract RGB values from the color
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        // Using the RGB values to search the database
        val targetColor = PaintFinder.PaintColor("", "", red, green, blue)
        val closestPaint = PaintFinder.findClosestPaint(targetColor, this)

        // Set the XML values to the correct paint and RGB when found
        if (closestPaint != null) {
            val closestPaintColor = Color.rgb(closestPaint.r, closestPaint.g, closestPaint.b)
            colorBox.setBackgroundColor(closestPaintColor)

            // Make the box clickable and route to PaintInfoActivity
            colorBox.setOnClickListener {
                val intent = Intent(this, PaintInfoActivity::class.java)
                // Pass the paint color and name
                intent.putExtra("selected_color", closestPaintColor)
                intent.putExtra("color_name", closestPaint.name)
                startActivity(intent)
            }
        }
    }
}
