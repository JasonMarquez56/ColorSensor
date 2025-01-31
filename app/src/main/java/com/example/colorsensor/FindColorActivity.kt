package com.example.colorsensor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class FindColorActivity : AppCompatActivity() {

    private val imageView: ImageView by lazy {
        findViewById(R.id.imageView2)
    }
    private val viewColor: View by lazy{
        findViewById(R.id.viewColor)
    }

    private lateinit var bitmap: Bitmap

    private var xRatioForBitmap = 1f
    private var yRatioForBitmap = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.color_sensor)

        bitmap = BitmapFactory.decodeResource(this.resources, R.drawable.colorsensor_home_banner)
        imageView.setImageBitmap(bitmap)

        imageView.post {
            xRatioForBitmap = bitmap.width.toFloat() / imageView.width.toFloat()
            yRatioForBitmap = bitmap.height.toFloat() / imageView.height.toFloat()
        }

        imageView.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                    val touchXtoBitmap: Float = motionEvent.x * xRatioForBitmap
                    val touchYtoBitmap: Float = motionEvent.y * yRatioForBitmap  // Fixed typo

                    if(touchXtoBitmap < 0 || touchYtoBitmap < 0){
                        return@setOnTouchListener true
                    }
                    if(touchXtoBitmap > bitmap.width || touchYtoBitmap > bitmap.height){
                        return@setOnTouchListener true
                    }

                    val pixel: Int = bitmap.getPixel(touchXtoBitmap.toInt(), touchYtoBitmap.toInt())

                    val red = Color.red(pixel)
                    val green = Color.green(pixel)
                    val blue = Color.blue(pixel)
                    val alpha = Color.alpha(pixel)

                    viewColor.setBackgroundColor(Color.argb(alpha, red, green, blue))
                }
            }
            true
        }

    }
}