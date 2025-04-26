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
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import com.example.colorsensor.utils.PaintFinder
import android.view.MotionEvent
import android.widget.ProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import java.util.*
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.IOException


class ColorChangerActivity : AppCompatActivity(), ColorPickerDialogFragment.OnColorSelectedListener {

    private lateinit var rgbValueText: TextView
    private lateinit var imageView: ImageView
    private lateinit var colorsButton: Button
    private lateinit var undoButton: Button
    private lateinit var resetButton: Button
    private lateinit var originalBitmap: Bitmap
    private lateinit var modifiedBitmap: Bitmap
    private lateinit var cannyBitmap: Bitmap
    private lateinit var colorBox: View
    private var x: Int = 0
    private var y: Int = 0
    private var threshold: Int = 200
    private var opacity: Int = 128
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
            val bitmap = loadBitmapFromUri(imageUri)

            // Error handling failed decoding
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

            // Apply canny edge detection to create cannyBitmap
            cannyBitmap = applyEdgeDetection(originalBitmap)
            cannyBitmap = scaleBitmap(cannyBitmap, 800, 600)

            // Display the original image (can change to cannyBitmap for debugging)
            imageView.setImageBitmap(modifiedBitmap)

        } catch (e: Exception) {
            Log.e("ColorChangerActivity", "Error loading image from URI", e)
            finish()
        }

        // Open color wheel when button is clicked for choosing color to change to
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

                x = touchPoint[0].toInt()
                y = touchPoint[1].toInt()

                if (x in 0 until modifiedBitmap.width && y in 0 until modifiedBitmap.height) {
                    val tappedColor = modifiedBitmap.getPixel(x, y)
                    Log.d("ColorChangerActivity", "Tapped Color: $tappedColor at ($x, $y)")

                    // Save the current bitmap before making changes for undo functionality
                    bitmapHistory.push(modifiedBitmap.copy(modifiedBitmap.config ?: Bitmap.Config.ARGB_8888, true))


                    // Launch on background thread to avoid freezing the UI
                    lifecycleScope.launch {
                        // Show a temporary loading spinner
                        spinner.visibility = View.VISIBLE

                        // Before applying the color change, store the current state of the modified bitmap in history
                        bitmapHistory.push(modifiedBitmap.copy(modifiedBitmap.config ?: Bitmap.Config.ARGB_8888, true))


                        val updatedBitmap = withContext(Dispatchers.Default) {
                            // Replace similar pixels with selected color
                            edgeAwareColorReplace(
                                modifiedBitmap,
                                cannyBitmap,
                                x,
                                y,
                                selectedColor,
                                opacity
                            )
                        }

                        // Update the UI on the main thread
                        imageView.setImageBitmap(updatedBitmap)
                        modifiedBitmap = updatedBitmap  // Update modifiedBitmap after change

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


    private fun getBrightness(color: Int): Int {
        // Using relative luminance formula
        return ((Color.red(color) * 0.299) + (Color.green(color) * 0.587) + (Color.blue(color) * 0.114)).toInt()
    }

    private fun adjustColorBrightness(baseColor: Int, targetBrightness: Int): Int {
        val r = Color.red(baseColor)
        val g = Color.green(baseColor)
        val b = Color.blue(baseColor)

        val currentBrightness = getBrightness(baseColor)
        // Avoid division by zero
        if (currentBrightness == 0) return baseColor

        // Blending brightness of original and target color to reach a more realistic finish
        val ratio = targetBrightness.toFloat() / currentBrightness

        val newR = (r * ratio).coerceIn(0f, 255f).toInt()
        val newG = (g * ratio).coerceIn(0f, 255f).toInt()
        val newB = (b * ratio).coerceIn(0f, 255f).toInt()

        return Color.rgb(newR, newG, newB)
    }


    private fun isEdgePixel(pixel: Int): Boolean {
        // Canny produces a black and white image where white pixels are defined as edges
        return pixel == Color.WHITE
    }

    private fun floodFillWallRegion(
        cannyBitmap: Bitmap,
        originalBitmap: Bitmap,
        startX: Int,
        startY: Int
    ): List<Pair<Int, Int>> {
        val width = originalBitmap.width
        val height = originalBitmap.height

        /* Used for debugging. Displays tapped pixel and pixel's marked as edge pixels

        val tappedPixel = cannyBitmap.getPixel(startX, startY)
        Log.d("DEBUG", "Tapped pixel at ($startX, $startY): $tappedPixel")

        if (isEdgePixel(tappedPixel)) {
            Log.d("DEBUG", "Edge pixel detected at ($startX, $startY): $tappedPixel")
            return emptyList()
        } */

        // Creating storage variables for the region and visited pixels
        val visited = Array(height) { BooleanArray(width) }
        val region = mutableListOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        // Setting starting point
        queue.add(Pair(startX, startY))
        visited[startY][startX] = true

        // Setting possible directions to move in
        val directions = arrayOf(
            Pair(0, 1), Pair(1, 0), Pair(0, -1), Pair(-1, 0),
            Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
        )

        // Loop for going through the image
        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            region.add(Pair(x, y))

            for ((dx, dy) in directions) {
                // Iterating through pixels
                val newX = x + dx
                val newY = y + dy

                // If pixel has not been visited, and is not an edge, add to queue and mark as visited
                if (
                    newX in 0 until width &&
                    newY in 0 until height &&
                    !visited[newY][newX] &&
                    !isEdgePixel(cannyBitmap.getPixel(newX, newY))
                ) {
                    visited[newY][newX] = true
                    queue.add(Pair(newX, newY))
                }
            }
        }

        return region
    }


    private fun edgeAwareColorReplace(
        modifiedBitmap: Bitmap,
        cannyBitmap: Bitmap,
        tappedX: Int,
        tappedY: Int,
        newColor: Int,
        opacity: Int
    ): Bitmap {
        val newBitmap = modifiedBitmap.copy(modifiedBitmap.config ?: Bitmap.Config.ARGB_8888, true)

        // Get region to replace (non-edge area)
        val region = floodFillWallRegion(cannyBitmap, modifiedBitmap, tappedX, tappedY)
        Log.d("DEBUG", "Region to replace: $region")

        // Apply color changes only to the region
        for ((x, y) in region) {
            val originalPixel = modifiedBitmap.getPixel(x, y)

            // Adjust the brightness of the new color to match the original pixel’s brightness
            val adjustedColor = adjustColorBrightness(newColor, getBrightness(originalPixel))

            // Blend the adjusted color with the original pixel’s color
            val blendedColor = blendColors(originalPixel, adjustedColor, opacity)

            newBitmap.setPixel(x, y, blendedColor)
        }

        return newBitmap
    }


    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Using ImageDecoder function
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    // Setting allocator to avoid hardware related issues when rendering
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                }
            } else {
                // Fallback for older Android versions using BitmapFactory
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
                        // Using ARGB_8888 config for better image quality
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    })
                }
            }
        } catch (e: IOException) {
            Log.e("FindColorActivity", "Error loading bitmap from Uri: $uri", e)
            null
        }
    }

    override fun onColorSelected(color: Int) {
        selectedColor = color
        colorBox.setBackgroundColor(color)

        // Collecting RGB values of selected color
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        // Finding closest matching paint in the database
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


    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Scaling down image, maintains aspect ratio
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

    private fun blendColors(originalColor: Int, newColor: Int, opacity: Int): Int {
        // Alpha values of original and selected color
        val originalAlpha = Color.alpha(originalColor) / 255f
        val newAlpha = opacity / 255f

        // Blending the alpha values
        val blendedAlpha = (newAlpha + originalAlpha * (1 - newAlpha)) * 255

        // Blending RGB channels with respective opacities
        val r = (Color.red(originalColor) * (1 - newAlpha) + Color.red(newColor) * newAlpha).toInt()
        val g = (Color.green(originalColor) * (1 - newAlpha) + Color.green(newColor) * newAlpha).toInt()
        val b = (Color.blue(originalColor) * (1 - newAlpha) + Color.blue(newColor) * newAlpha).toInt()

        return Color.argb(blendedAlpha.toInt(), r, g, b)
    }

    fun applyEdgeDetection(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

        // Apply Canny edge detection
        val edges = Mat()
        Imgproc.GaussianBlur(gray, gray, Size(3.0, 3.0), 0.5, 0.5)
        // Thresholds for Canny (can be adjusted)
        Imgproc.Canny(gray, edges, 40.0, 100.0)

        // Dilate to thicken the edges to 2 pixels (1 pixel was causing issues of edges not connecting)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
        Imgproc.dilate(edges, edges, kernel, Point(-1.0, -1.0), 1)

        // Convert the edges to a 3-channel BGR image for visualization
        val edgesColor = Mat()
        Imgproc.cvtColor(edges, edgesColor, Imgproc.COLOR_GRAY2BGR)

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
