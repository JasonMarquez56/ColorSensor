package com.example.colorsensor

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.*
import androidx.activity.result.contract.*
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PhotoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.find_color) // Ensure the correct layout file is referenced here

        // val cameraButton = findViewById<Button>(R.id.cameraButton) // find color
        val takePhoto = findViewById<Button>(R.id.cameraButton)
        val uploadButton = findViewById<Button>(R.id.uploadButton)
        val imageSelector = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: $uri")
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }
        uploadButton.setOnClickListener{
            imageSelector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        imageSelector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
//        activityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
//            StartActivityForResult(),
//            object : ActivityResultCallback<ActivityResult?> {
//                override fun onActivityResult(result: ActivityResult) {
//                    if (result.resultCode == RESULT_OK && result.data != null) {
//                        val bundle = result.data!!.extras
//                        val bitmap = bundle!!["data"] as Bitmap?
//                        imageProfile.setImageBitmap(bitmap)
//                    }
//                }
//            })

        takePhoto.setOnClickListener(View.OnClickListener {
            val intent = Intent((MediaStore.ACTION_IMAGE_CAPTURE))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        })
    }
}