package com.example.colorsensor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.ByteArrayOutputStream
import java.io.File

class PhotoFragment : Fragment() {

    private lateinit var photoFile: File
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private val CAMERA_REQUEST_CODE = 101  // Unique request code for permissions

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_color, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigationBar()

        val takePhoto = view.findViewById<Button>(R.id.cameraButton)
        val uploadPhoto = view.findViewById<Button>(R.id.uploadButton)

        // Register the camera activity result
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
                sendUriToFindColor(photoUri)
            } else {
                Toast.makeText(requireContext(), "Camera capture failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Register the gallery image picker
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: $uri")
                sendUriToFindColor(uri)
            } else {
                Log.d("PhotoPicker", "No media selected")
                Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle camera button click
        takePhoto.setOnClickListener {
            // Check and request camera permissions
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    requireContext() as Activity,
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
        photoFile = File(requireContext().cacheDir, "temp_image.jpg")

        val photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri) // Save high-res image to file
        }

        takePhotoLauncher.launch(cameraIntent)
    }

    // Handle the result of the camera permission request
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission denied. Please allow access to use the camera.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Send URI to FindColorActivity
    private fun sendUriToFindColor(uri: Uri) {
        val fragments = FindColorFragment().apply {
            arguments = Bundle().apply {
                putString("image_uri", uri.toString())
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragments)  // ← use the one with arguments
            .addToBackStack(null)
            .commit()
    }

    private fun navigationBar() {
        // Navigation bar
        val bottomNavigationView = view?.findViewById<BottomNavigationView>(R.id.bottomNavigationView3)

        // Map default and selected icons
        val iconMap = mapOf(
            R.id.profile to Pair(R.drawable.account_outline, R.drawable.account),
            R.id.home to Pair(R.drawable.home_outline, R.drawable.home),
            R.id.settings to Pair(R.drawable.cog_outline, R.drawable.cog)
        )

        // Track currently selected item
        var selectedItemId: Int? = null

        bottomNavigationView?.setOnItemSelectedListener { item ->


            // Reset previous selection
            selectedItemId?.let { prevId ->
                bottomNavigationView.menu.findItem(prevId).setIcon(iconMap[prevId]?.first ?: R.drawable.home)
            }

            // Change selected icon
            item.setIcon(iconMap[item.itemId]?.second ?: R.drawable.home)
            selectedItemId = item.itemId

            when (item.itemId) {
                R.id.profile -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProfileFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                R.id.home -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                R.id.settings -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LandingFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                else -> false
            }
        }
    }
}