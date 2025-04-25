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
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import com.example.colorsensor.utils.PaintFinder
import android.view.MotionEvent
import android.widget.ProgressBar
import androidx.compose.ui.Modifier
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

class ImageSplitActivity : AppCompatActivity(), ColorPickerDialogFragment.OnColorSelectedListener {
    private lateinit var rgbValueText: TextView
    private lateinit var imageView: ImageView
    private lateinit var colorsButton: Button
    private lateinit var undoButton: Button
    private lateinit var resetButton: Button
    private lateinit var originalBitmap: Bitmap
    private lateinit var modifiedBitmap: Bitmap
    private lateinit var cannyBitmap: Bitmap
    private lateinit var rightModifiedBitmap: Bitmap
    private lateinit var leftModifiedBitmap:  Bitmap
    private lateinit var rightCannyBitmap: Bitmap
    private lateinit var leftCannyBitmap:  Bitmap
    private lateinit var colorBox: View
    private var mid: Int = 0
    private var x: Int = 0
    private var y: Int = 0
    private var threshold: Int = 200
    private var opacity: Int = 128
    private var selectedColor: Int = Color.WHITE
    val bitmapHistory = Stack<Pair<Bitmap, Bitmap>>()


    companion object {
        init {
            // Load native library
            System.loadLibrary("opencv_java4")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_split)

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
            Log.e("ImageSplitActivity", "No imageUri provided")
            finish()
            return
        }

        // Parsing image to bitmap
        try {
            val imageUri = Uri.parse(imageUriString)
            val bitmap = loadBitmapFromUri(imageUri)

            if (bitmap == null) {
                Log.e("ImageSplitActivity", "Failed to decode bitmap from URI")
                finish()
                return
            }

            originalBitmap = bitmap
            // Scale to a manageable size
            originalBitmap = scaleBitmap(originalBitmap, 800, 600)
            val config = originalBitmap.config ?: Bitmap.Config.ARGB_8888
            modifiedBitmap = originalBitmap.copy(config, true)

            // Apply Sobel edge detection to create sobelBitmap
            cannyBitmap = applyEdgeDetection(originalBitmap)
            cannyBitmap = scaleBitmap(cannyBitmap, 800, 600)

            // Display the original (or modified) image
            imageView.setImageBitmap(cannyBitmap)

            // Calculating half the original height and the whole width
            val width = originalBitmap.width
            val height = originalBitmap.height
            mid = width / 2

            // Split originalBitmap into left and right for image split
            leftModifiedBitmap = Bitmap.createBitmap(modifiedBitmap, 0, 0, mid, height)
            rightModifiedBitmap = Bitmap.createBitmap(modifiedBitmap, mid, 0, width - mid, height)

            // Split cannyBitmap into left and right
            leftCannyBitmap = Bitmap.createBitmap(cannyBitmap, 0, 0, mid, height)
            rightCannyBitmap = Bitmap.createBitmap(cannyBitmap, mid, 0, width - mid, height)


        } catch (e: Exception) {
            Log.e("ImageSplitActivity", "Error loading image from URI", e)
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

                x = touchPoint[0].toInt()
                y = touchPoint[1].toInt()

                if (x in 0 until modifiedBitmap.width && y in 0 until modifiedBitmap.height) {
                    val tappedColor = modifiedBitmap.getPixel(x, y)
                    Log.d("ImageSplitActivity", "Tapped Color: $tappedColor at ($x, $y)")

                    // Save the current bitmap before making changes for undo functionality
                    bitmapHistory.push(
                        Pair(
                            leftModifiedBitmap.copy(
                                leftModifiedBitmap.config ?: Bitmap.Config.ARGB_8888, true
                            ),
                            rightModifiedBitmap.copy(
                                rightModifiedBitmap.config ?: Bitmap.Config.ARGB_8888, true
                            )
                        )
                    )


                    // Launch on background thread to avoid freezing the UI
                    lifecycleScope.launch {
                        // Show a temporary loading spinner
                        spinner.visibility = View.VISIBLE

                        // Before applying the color change, store the current state of the modified bitmap in history
                        bitmapHistory.push(
                            Pair(
                                leftModifiedBitmap.copy(
                                    leftModifiedBitmap.config ?: Bitmap.Config.ARGB_8888, true
                                ),
                                rightModifiedBitmap.copy(
                                    rightModifiedBitmap.config ?: Bitmap.Config.ARGB_8888, true
                                )
                            )
                        )

                        if (x < mid) {
                            val updatedBitmap = withContext(Dispatchers.Default) {
                                // Replace similar pixels with selected color
                                edgeAwareColorReplace(
                                    leftModifiedBitmap,   // Use modifiedBitmap to accumulate changes
                                    leftCannyBitmap,      // Sobel edge-detection bitmap
                                    x,          // X coordinate where tapped
                                    y,          // Y coordinate where tapped
                                    selectedColor,    // The new color to apply
                                    opacity           // Opacity for blending
                                )
                            }

                            // Update leftModifiedBitmap to keep track of the new state
                            leftModifiedBitmap = updatedBitmap

                            // Recombine both halves into a new full image
                            val combinedBitmap = Bitmap.createBitmap(
                                leftModifiedBitmap.width + rightModifiedBitmap.width,
                                leftModifiedBitmap.height,
                                Bitmap.Config.ARGB_8888
                            )

                            val canvas = Canvas(combinedBitmap)
                            canvas.drawBitmap(leftModifiedBitmap, 0f, 0f, null)
                            canvas.drawBitmap(rightModifiedBitmap, mid.toFloat(), 0f, null)

                            // Set the result to ImageView and save the combined result
                            imageView.setImageBitmap(combinedBitmap)
                            modifiedBitmap = combinedBitmap

                            // Hide the loading spinner
                            spinner.visibility = View.GONE
                        } else {
                            val updatedBitmap = withContext(Dispatchers.Default) {
                                // Replace similar pixels with selected color
                                edgeAwareColorReplace(
                                    rightModifiedBitmap,   // Use modifiedBitmap to accumulate changes
                                    rightCannyBitmap,      // Canny edge-detection bitmap
                                    x - mid,          // X coordinate where tapped, subtract mid since split
                                    y,          // Y coordinate where tapped
                                    selectedColor,    // The new color to apply
                                    opacity           // Opacity for blending
                                )
                            }

                            rightModifiedBitmap = updatedBitmap

                            // Recombine both halves into a new full image
                            val combinedBitmap = Bitmap.createBitmap(
                                leftModifiedBitmap.width + rightModifiedBitmap.width,
                                leftModifiedBitmap.height,
                                Bitmap.Config.ARGB_8888
                            )

                            val canvas = Canvas(combinedBitmap)
                            canvas.drawBitmap(leftModifiedBitmap, 0f, 0f, null)
                            canvas.drawBitmap(rightModifiedBitmap, mid.toFloat(), 0f, null)

                            // Set the result to ImageView and save the combined result
                            imageView.setImageBitmap(combinedBitmap)
                            modifiedBitmap = combinedBitmap

                            // Removing spinner when done loading
                            spinner.visibility = View.GONE
                        }
                    }
                }
            }
            true
        }

        // Undo button
        // Undo button
        undoButton.setOnClickListener {
            if (bitmapHistory.isNotEmpty()) {
                // Pop the last bitmap pair from history
                val (leftBitmap, rightBitmap) = bitmapHistory.pop()

                // Update the left and right halves to the previous state
                leftModifiedBitmap = leftBitmap
                rightModifiedBitmap = rightBitmap

                // Recombine the left and right halves into a full bitmap
                val combinedBitmap = Bitmap.createBitmap(
                    leftModifiedBitmap.width + rightModifiedBitmap.width,
                    leftModifiedBitmap.height,
                    Bitmap.Config.ARGB_8888
                )

                val canvas = Canvas(combinedBitmap)
                canvas.drawBitmap(leftModifiedBitmap, 0f, 0f, null)
                canvas.drawBitmap(rightModifiedBitmap, leftModifiedBitmap.width.toFloat(), 0f, null)

                // Update the ImageView and the modifiedBitmap
                modifiedBitmap = combinedBitmap
                imageView.setImageBitmap(modifiedBitmap)
            }
        }

        // Reset button
        resetButton.setOnClickListener {
            leftModifiedBitmap =
                originalBitmap.copy(originalBitmap.config ?: Bitmap.Config.ARGB_8888, true)
                    .let { Bitmap.createBitmap(it, 0, 0, it.width / 2, it.height) }

            rightModifiedBitmap =
                originalBitmap.copy(originalBitmap.config ?: Bitmap.Config.ARGB_8888, true)
                    .let { Bitmap.createBitmap(it, it.width / 2, 0, it.width / 2, it.height) }

            val combined = Bitmap.createBitmap(
                leftModifiedBitmap.width + rightModifiedBitmap.width,
                leftModifiedBitmap.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(combined)
            canvas.drawBitmap(leftModifiedBitmap, 0f, 0f, null)
            canvas.drawBitmap(rightModifiedBitmap, leftModifiedBitmap.width.toFloat(), 0f, null)

            modifiedBitmap = combined
            imageView.setImageBitmap(combined)
            bitmapHistory.clear()
        }
    }

        private fun getBrightness(color: Int): Int {
        return ((Color.red(color) * 0.299) + (Color.green(color) * 0.587) + (Color.blue(color) * 0.114)).toInt()
    }

    private fun adjustColorBrightness(baseColor: Int, targetBrightness: Int): Int {
        val r = Color.red(baseColor)
        val g = Color.green(baseColor)
        val b = Color.blue(baseColor)

        val currentBrightness = getBrightness(baseColor)
        // Avoid division by zero
        if (currentBrightness == 0) return baseColor

        val ratio = targetBrightness.toFloat() / currentBrightness

        val newR = (r * ratio).coerceIn(0f, 255f).toInt()
        val newG = (g * ratio).coerceIn(0f, 255f).toInt()
        val newB = (b * ratio).coerceIn(0f, 255f).toInt()

        return Color.rgb(newR, newG, newB)
    }


    private fun isEdgePixel(pixel: Int): Boolean {
        // Assuming Canny edge detection produces a binary image
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

        val tappedPixel = cannyBitmap.getPixel(startX, startY)
        Log.d("DEBUG", "Tapped pixel at ($startX, $startY): $tappedPixel")

        if (isEdgePixel(tappedPixel)) {
            Log.d("DEBUG", "Edge pixel detected at ($startX, $startY): $tappedPixel")
            return emptyList()
        }

        val visited = Array(height) { BooleanArray(width) }
        val region = mutableListOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        queue.add(Pair(startX, startY))
        visited[startY][startX] = true

        val directions = arrayOf(
            Pair(0, 1), Pair(1, 0), Pair(0, -1), Pair(-1, 0),
            Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
        )

        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            region.add(Pair(x, y))

            for ((dx, dy) in directions) {
                val newX = x + dx
                val newY = y + dy

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

    private fun blendColors(originalColor: Int, newColor: Int, opacity: Int): Int {
        // Alpha values of both colors
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
        Imgproc.Canny(gray, edges, 40.0, 100.0)  // Thresholds for Canny (can be adjusted)

        // Dilate to thicken the edges to 2 pixels
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