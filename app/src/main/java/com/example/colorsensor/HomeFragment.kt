package com.example.colorsensor

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.ByteArrayOutputStream

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the navigation bar
        navigationBar()

        // Find the button by its ID
        val goToProfileButton = view.findViewById<Button>(R.id.btnProfile) // profile
        val goToPhotoButton = view.findViewById<Button>(R.id.btnFindColor) // find color
        val goToLiveColor = view.findViewById<Button>(R.id.btnVideo) // live color
        val goToPopularColor = view.findViewById<Button>(R.id.btnPopularColor) // popular color
        val goToColorBlending = view.findViewById<Button>(R.id.btnColorBlending) // color blending
        val goToShopButton = view.findViewById<Button>(R.id.btnShop)

        // Set a click listener for the Photo button
        goToPhotoButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PhotoFragment())
                .addToBackStack(null)
                .commit()
        }

        // Set a click listener for the Shop button
        goToShopButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .addToBackStack(null)
                .commit()
        }

        // Set a click listener for the Profile button
        goToProfileButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        // Set a click listener for the Video button
        goToLiveColor.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FragmentLiveFeed())
                .addToBackStack(null)
                .commit()
        }

        // Set a click listener for the Popular Color button
        goToPopularColor.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PopularColorFragment())
                .addToBackStack(null)
                .commit()
        }

        goToColorBlending.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ColorBlendingFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    // Convert Bitmap to ByteArray and send to FindColorActivity
    private fun sendImageToFindColor(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()

        val fragments = FindColorFragment().apply {
            arguments = Bundle().apply {
                putByteArray("image_bitmap", byteArray)
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

        bottomNavigationView?.setOnNavigationItemSelectedListener { item ->

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