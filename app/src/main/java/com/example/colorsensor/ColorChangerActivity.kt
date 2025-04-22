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
import kotlinx.coroutines.*
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
    private lateinit var sobelBitmap: Bitmap
    private lateinit var colorBox: View
    private var selectedColor: Int = Color.WHITE
    private val bitmapHistory: Stack<Bitmap> = Stack()

    companion object {
        init {
            // Load native library
            System.loadLibrary("opencv_java4")
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
            // Scale to a manageable size
            originalBitmap = scaleBitmap(originalBitmap, 800, 600)
            val config = originalBitmap.config ?: Bitmap.Config.ARGB_8888
            modifiedBitmap = originalBitmap.copy(config, true)

            // Apply Sobel edge detection to create sobelBitmap
            sobelBitmap = applyEdgeDetection(originalBitmap)
            sobelBitmap = scaleBitmap(sobelBitmap, 800, 600)

            // Display the original (or modified) image
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
                            replaceColorInBitmapParallel(modifiedBitmap, tappedColor, selectedColor)
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

    private fun isColorSimilar(color1: Int, color2: Int, tolerance: Int): Boolean {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2

        val distanceSquared = dr * dr + dg * dg + db * db
        return distanceSquared <= tolerance * tolerance
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate the ratio to scale the image down proportionally
        val ratioBitmap = width.toFloat() / height.toFloat()
        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioBitmap > 1) {
            finalHeight = (maxWidth / ratioBitmap).toInt()
        } else {
            finalWidth = (maxHeight * ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    suspend fun replaceColorInBitmapParallel(
        bitmap: Bitmap,
        targetColor: Int,
        newColor: Int,
        opacity: Int = 200,
        tolerance: Int = 80,
        numChunks: Int = Runtime.getRuntime().availableProcessors()
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val newBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)

        val targetHSV = FloatArray(3)
        Color.colorToHSV(targetColor, targetHSV)

        val newHSV = FloatArray(3)
        Color.colorToHSV(newColor, newHSV)

        val targetR = Color.red(targetColor)
        val targetG = Color.green(targetColor)
        val targetB = Color.blue(targetColor)

        val chunkSize = height / numChunks
        val jobs = mutableListOf<Deferred<Unit>>()

        for (i in 0 until numChunks) {
            val startY = i * chunkSize
            val endY = if (i == numChunks - 1) height else (i + 1) * chunkSize

            jobs += async {
                val pixels = IntArray(width)
                val pixelHSV = FloatArray(3)

                for (y in startY until endY) {
                    bitmap.getPixels(pixels, 0, width, 0, y, width, 1)

                    for (x in 0 until width) {
                        val pixelColor = pixels[x]

                        val r = Color.red(pixelColor)
                        val g = Color.green(pixelColor)
                        val b = Color.blue(pixelColor)

                        val dr = r - targetR
                        val dg = g - targetG
                        val db = b - targetB

                        val distanceSq = dr * dr + dg * dg + db * db
                        if (distanceSq <= tolerance * tolerance) {
                            Color.colorToHSV(pixelColor, pixelHSV)
                            pixelHSV[0] = newHSV[0]
                            pixelHSV[1] = newHSV[1]
                            val modifiedColor = Color.HSVToColor(Color.alpha(pixelColor), pixelHSV)
                            pixels[x] = blendColors(pixelColor, modifiedColor, opacity)
                        }
                    }

                    synchronized(newBitmap) {
                        newBitmap.setPixels(pixels, 0, width, 0, y, width, 1)
                    }
                }
            }
        }

        jobs.awaitAll()
        newBitmap
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
