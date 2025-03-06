package com.example.colorsensor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

class PhotoActivity : AppCompatActivity() {

    private lateinit var photoFile: File
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private val CAMERA_REQUEST_CODE = 101  // Unique request code for permissions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.find_color) // Ensure correct layout file

        val takePhoto = findViewById<Button>(R.id.cameraButton)
        val uploadPhoto = findViewById<Button>(R.id.uploadButton)

        // Register the camera activity result
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
                sendUriToFindColor(photoUri)
            } else {
                Toast.makeText(this, "Camera capture failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Register the gallery image picker
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: $uri")
                sendUriToFindColor(uri)
            } else {
                Log.d("PhotoPicker", "No media selected")
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle camera button click
        takePhoto.setOnClickListener {
            // Check and request camera permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
            } else {
                launchCamera()
            }
        }

        // Handle gallery button click
        uploadPhoto.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun launchCamera() {
        // Always use the same file path
        photoFile = File(cacheDir, "temp_image.jpg")

        val photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri) // Save high-res image to file
        }

        takePhotoLauncher.launch(cameraIntent)
    }

    // Handle the result of the camera permission request
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission denied. Please allow access to use the camera.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Send URI to FindColorActivity
    private fun sendUriToFindColor(uri: Uri) {
        val intent = Intent(this, FindColorActivity::class.java)
        intent.putExtra("image_uri", uri.toString())
        startActivity(intent)
    }
}
