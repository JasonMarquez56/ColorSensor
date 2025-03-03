package com.example.colorsensor

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference

class ImageSplitActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_split)
        val imageView: ImageView by lazy { findViewById(R.id.imageView) }


        // Retrieve byte array from intent
        val byteArray = intent.getByteArrayExtra("bitmap")

        // Convert byte array back to Bitmap
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)

        val weakBitmap = WeakReference(bitmap)
        imageView.setImageBitmap(weakBitmap.get())
    }
}