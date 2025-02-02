package com.example.colorsensor

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.appcompat.app.AppCompatActivity

class PhotoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.find_color) // Ensure the correct layout file is referenced here

        // val cameraButton = findViewById<Button>(R.id.cameraButton) // find color
        val takePhoto = findViewById<Button>(R.id.cameraButton)

        activityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
            StartActivityForResult(),
            object : ActivityResultCallback<ActivityResult?> {
                override fun onActivityResult(result: ActivityResult) {
                    if (result.resultCode == RESULT_OK && result.data != null) {
                        val bundle = result.data!!.extras
                        val bitmap = bundle!!["data"] as Bitmap?
                        imageProfile.setImageBitmap(bitmap)
                    }
                }
            })

        takePhoto.setOnClickListener(View.OnClickListener {
            val intent = Intent((MediaStore.ACTION_IMAGE_CAPTURE))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        })
    }
}