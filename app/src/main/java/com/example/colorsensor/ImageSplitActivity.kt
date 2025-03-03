package com.example.colorsensor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.colorsensor.utils.PaintFinder
import java.lang.ref.WeakReference

class ImageSplitActivity : AppCompatActivity() {
    private lateinit var bitmap : Bitmap
    private val viewColor: View by lazy { findViewById(R.id.viewColor) }
    private val textHex: TextView by lazy { findViewById(R.id.textView) }
    private val textRGB: TextView by lazy { findViewById(R.id.textView2) }
    private val textName: TextView by lazy { findViewById(R.id.textView8) }
    private val textViewRGB: TextView by lazy { findViewById(R.id.textViewRGB) }


    private val viewColor_2: View by lazy { findViewById(R.id.viewColor12) }
    private val textHex_2: TextView by lazy { findViewById(R.id.textView_2) }
    private val textRGB_2: TextView by lazy { findViewById(R.id.textView2_2) }
    private val textName_2: TextView by lazy { findViewById(R.id.textView8_2) }
    private val textViewRGB_2: TextView by lazy { findViewById(R.id.textViewRGB_2) }

    private var tap = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_split)

        val imageView: ImageView by lazy { findViewById(R.id.imageView) }
        // Retrieve byte array from intent
        val byteArray = intent.getByteArrayExtra("bitmap")
        // Convert byte array back to Bitmap
        bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
        val weakBitmap = WeakReference(bitmap)
        imageView.setImageBitmap(weakBitmap.get())

        imageView.setOnTouchListener { _, motionEvent ->
            if (this::bitmap.isInitialized) {
                // Get the actual image drawable bounds inside the ImageView
                val imageMatrixValues = FloatArray(9)
                imageView.imageMatrix.getValues(imageMatrixValues)

                // Extract scaling factors and translations
                val scaleX = imageMatrixValues[0]
                val scaleY = imageMatrixValues[4]
                val translateX = imageMatrixValues[2]
                val translateY = imageMatrixValues[5]

                // Convert touch coordinates to match the size of the imageview and bitmap
                val touchXtoBitmap = (motionEvent.x - translateX) / scaleX
                val touchYtoBitmap = (motionEvent.y - translateY) / scaleY

                // Ensure the touch coordinates are within the bitmap bounds
                if (touchXtoBitmap in 0f..bitmap.width.toFloat() &&
                    touchYtoBitmap in 0f..bitmap.height.toFloat()
                ) {
                    try {
                        // Get the pixel color at the touched position
                        val pixel = bitmap.getPixel(touchXtoBitmap.toInt(), touchYtoBitmap.toInt())
                        // Extract RGBA components from the pixel color
                        val red = Color.red(pixel)
                        val green = Color.green(pixel)
                        val blue = Color.blue(pixel)
                        val alpha = Color.alpha(pixel)
                        // update the viewColor background color
                        if(tap == 0){
                            viewColor.setBackgroundColor(Color.argb(alpha, red, green, blue))
                        }
                        else
                            viewColor_2.setBackgroundColor(Color.argb(alpha, red, green, blue))

                        // update the text for Hex and RGB to the target color
                        if(tap == 0) {
                            textHex.text = "Hex: #${Integer.toHexString(pixel).uppercase().substring(2)}"
                            textRGB.text = "RGB: ($red, $green, $blue)"
                        }
                        else{
                            textHex_2.text = "Hex: #${Integer.toHexString(pixel).uppercase().substring(2)}"
                            textRGB_2.text = "RGB: ($red, $green, $blue)"
                        }

                        // Calculating luminance with standard weighted formula
                        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
                        // Changing text color based off luminance, making it more visible
                        val textColor = if (luminance > 0.5) Color.BLACK else Color.WHITE
                        // Changing color of text fields
                        if(tap == 0) {
                            textRGB.setTextColor(textColor)
                            textHex.setTextColor(textColor)
                            textName.setTextColor(textColor)
                            textViewRGB.setTextColor(textColor)
                        }
                        else{
                            textRGB_2.setTextColor(textColor)
                            textHex_2.setTextColor(textColor)
                            textName_2.setTextColor(textColor)
                            textViewRGB_2.setTextColor(textColor)
                        }

                        // Search the closest color when user lifts their finger
                        if (motionEvent.action == MotionEvent.ACTION_UP) {
                            // call the function to update the textView -> textName
                            if(tap == 0)
                                searchClosestColor(red, green, blue)
                            else
                                searchClosestColor_2(red, green, blue)

                            tap = 1 - tap
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
        val targetColor = PaintFinder.PaintColor("", "", targetRed, targetGreen, targetBlue)
        val closestPaint = PaintFinder.findClosestPaint(targetColor, this)
        // Setting XML values to correct paint and RGB when found
        if (closestPaint != null) {
            val closestRGB = "(${closestPaint.r}, ${closestPaint.g}, ${closestPaint.b})"
            textName.text = "Closest Paint: ${closestPaint.name}"
            textViewRGB.text = "RGB: $closestRGB"
        } else {
            textName.text = "No matching paint found"
            textViewRGB.text = ""
        }
    }

    private fun searchClosestColor_2(targetRed: Int, targetGreen: Int, targetBlue: Int) {
        val targetColor = PaintFinder.PaintColor("", "", targetRed, targetGreen, targetBlue)
        val closestPaint = PaintFinder.findClosestPaint(targetColor, this)
        // Setting XML values to correct paint and RGB when found
        if (closestPaint != null) {
            val closestRGB = "(${closestPaint.r}, ${closestPaint.g}, ${closestPaint.b})"
            textName_2.text = "Closest Paint: ${closestPaint.name}"
            textViewRGB_2.text = "RGB: $closestRGB"
        } else {
            textName_2.text = "No matching paint found"
            textViewRGB_2.text = ""
        }
    }

    fun showPopup(context: Context, anchorView: View, message: String) {
        // create a LinearLayout to serve as the popup content
        val popupView = LinearLayout(context)
        popupView.orientation = LinearLayout.VERTICAL
        popupView.setPadding(20, 20, 20, 20)

        // display the message
        val textView = TextView(context)
        textView.text = message
        textView.textSize = 18f
        textView.setTextColor(Color.BLACK)

        // add textView to the layout
        popupView.addView(textView)

        // create a popupWindow using the dynamic layout
        val popupWindow = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)

        // show the popupWindow
        popupWindow.showAsDropDown(anchorView, 0, 0)
    }
}