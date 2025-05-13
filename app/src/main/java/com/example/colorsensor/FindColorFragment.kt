package com.example.colorsensor

import PaintInfoFragment
import android.annotation.SuppressLint
import android.content.ContentValues
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Magnifier
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.lang.ref.WeakReference
import com.example.colorsensor.utils.PaintFinder
// import for popupWindow
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.core.net.toUri
import androidx.core.graphics.toColorInt
import com.example.colorsensor.RegisterFragment.RGB
import com.example.colorsensor.RegisterFragment.favColor
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File


class FindColorFragment : Fragment() {

    private var imageByteArray: ByteArray? = null
    private var imageUri: String? = null
    private lateinit var bitmap: Bitmap
    private lateinit var firestore: FirebaseFirestore
    private lateinit var imageView: ImageView
    private lateinit var textName: TextView
    private lateinit var textRGB: TextView
    private lateinit var textHex: TextView
    private lateinit var changeColorButton: Button

    private var magnifier: Magnifier? = null
    private val viewColor: View by lazy {
        view?.findViewById<View>(R.id.viewColor)
            ?: throw IllegalStateException("View not found")
    }

//    private val accessibility: View by lazy {
//        view?.findViewById<View>(R.id.viewColor15)
//            ?: throw IllegalStateException("View not found")
//    }

    private val accessibilityText: TextView by lazy {
        view?.findViewById<TextView>(R.id.textView13)
            ?: throw IllegalStateException("View not found")
    }

    private val textViewRGB: TextView by lazy {
        view?.findViewById<TextView>(R.id.textViewRGB)
            ?: throw IllegalStateException("View not found")
    }
    private var selectedColor = RGB(0,0,0)
    private var xRatioForBitmap = 1f
    private var yRatioForBitmap = 1f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_color_sensor, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigationBar()

        arguments?.let {
            imageByteArray = it.getByteArray("image_bitmap")
            imageUri = it.getString("image_uri")
        }

        imageView = view.findViewById(R.id.imageView)
        textName = view.findViewById(R.id.textView8)
        textRGB = view.findViewById(R.id.textView2)
        textHex = view.findViewById(R.id.textView)


        setupButtonClickListener()
        val testButton = view.findViewById<Button>(R.id.button27)
        val saveButton = view.findViewById<Button>(R.id.saveColorButton)
        val favoriteButton = view.findViewById<ImageButton>(R.id.favoriteButton)
        changeColorButton = view.findViewById(R.id.btnChangeColor)
        firestore = FirebaseFirestore.getInstance()

        saveButton.setOnClickListener {
            // Save the image with the banner
            val originalBitmap = bitmap
            saveImageWithBanner(this, originalBitmap, textName.text.toString(), textRGB.text.toString(), textHex.text.toString())
            Toast.makeText(context, "Image saved with banner", Toast.LENGTH_SHORT).show()

        }

        favoriteButton.setOnClickListener {
            val sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            sharedPreferences.all
            //gets the username of current session.
            val username = sharedPreferences.getString("username", "Guest")
            //search through database with user name
            firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { documents ->
                    //if user name is successfully found.
                    if (!documents.isEmpty) {
                        for (document in documents) {
                            //finds user id
                            val userId = document.id
                            Toast.makeText(context, userId, Toast.LENGTH_SHORT).show()
                            val user = firestore.collection("users").document(userId)
                            val colorAdd = favColor(textName.text.toString().replace("Closest Paint: ",""),selectedColor)
                            //add favorite color.
                            user.update("favoriteColors", FieldValue.arrayUnion(colorAdd))
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Succeeded to create", Toast.LENGTH_SHORT)
                                        .show()
                                    favoriteButton.isSelected = true // Change the star icon to filled
                                }//in case failed to update
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Failed to create", Toast.LENGTH_SHORT)
                                        .show()
                                }
                        }
                        //if no user is found give warning
                    } else {
                        Log.d("Firestore", "No user found with username: $username")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting documents: ", exception)
                }
            return@setOnClickListener
        }

        // Load bitmap from byte array or URI with WeakReference
        bitmap = when {
            imageByteArray != null -> BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray!!.size)
            imageUri != null -> loadBitmapFromUri(imageUri!!.toUri()) ?: getDefaultBitmap()
            else -> getDefaultBitmap()
        }

        // Wrap the bitmap in a WeakReference
        val weakBitmap = WeakReference(bitmap)

        // If the bitmap is null, log the error and set a default image
        if (this::bitmap.isInitialized && bitmap != null) {
            imageView.setImageBitmap(bitmap)
            Log.d("DEBUG", "Bitmap successfully set in imageView")
        } else {
            Log.e("FindColorActivity", "Bitmap is not initialized or is null")
            imageView.setImageBitmap(getDefaultBitmap())
        }


        changeColorButton.setOnClickListener {
            if (imageUri != null) {
                val fragment = ColorChangerFragment.newInstance(imageUri.toString())
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Log.e("FindColorActivity", "No image URI found to pass")
                Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }

        // image split
        // Avoid crashing dur to high-resolution images. Pass the image along a temporary png file
        testButton.setOnClickListener {
            if (imageUri != null) {
                val fragment = ImageSplitFragment.newInstance(imageUri.toString())
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Log.e("ImageSplitActivity", "No image URI found to pass")
                Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
            }
        }

        imageView.post {
            // bitmap is initialized before finding color and update color strip
            if (this::bitmap.isInitialized) {
                xRatioForBitmap = bitmap.width.toFloat() / imageView.width.toFloat()
                yRatioForBitmap = bitmap.height.toFloat() / imageView.height.toFloat()
            }
        }

        // magnifier 133 - 153 line
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier = Magnifier.Builder(imageView)
                .setInitialZoom(2.3f)
                .setElevation(10.0f)
                .setCornerRadius(50.0f) // circle
                .setSize(100, 100)
                .build()
        }

        // Set touch listener for color detection
        // This is for color strip and find color using built-in android studio tools
        imageView.setOnTouchListener { _, motionEvent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                when (motionEvent.action) {
                    MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                        magnifier?.show(motionEvent.rawX, motionEvent.y)
                        favoriteButton.isSelected = false // Change the star icon to outline
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        magnifier?.dismiss()
                    }
                }
            }

            // check the bitmap if it is isInitialized
            if (this::bitmap.isInitialized) {
                // New code 172 - 185
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
                        viewColor.setBackgroundColor(Color.argb(alpha, red, green, blue))
                        // accessbility mode
//                        when {
//                            SettingsUtil.isProtanomalyEnabled(requireContext()) -> {
//                                val protanopiaColor = SettingsUtil.hexToProtanomalyHex(red, green, blue)
//                                accessibility.setBackgroundColor(Color.parseColor(protanopiaColor))
//                                accessibilityText.text = "Protanomaly (Red-Blind)"
//                            }
//                            SettingsUtil.isDeuteranomalyEnabled(requireContext()) -> {
//                                val deuteranomalyColor = SettingsUtil.hexToDeuteranomalyHex(red, green, blue)
//                                accessibility.setBackgroundColor(Color.parseColor(deuteranomalyColor))
//                                accessibilityText.text = "Deuteranomaly"
//                            }
//                            SettingsUtil.isTritanomalyEnabled(requireContext()) -> {
//                                val tritanomalyColor = SettingsUtil.hexToTritanomalyHex(red, green, blue)
//                                accessibility.setBackgroundColor(Color.parseColor(tritanomalyColor))
//                                accessibilityText.text = "Tritanomaly"
//                            }
//                        }


                        // Additional color processing
                        var step = 15 // Color strip get the darkest color use steps to update next rgb
                        for (i in 2..6) {
                            val newRed = 0.coerceAtLeast(red - (i - 2) * step)
                            val newGreen = 0.coerceAtLeast(green - (i - 2) * step)
                            val newBlue = 0.coerceAtLeast(blue - (i - 2) * step)
                            val color = Color.argb(alpha, newRed, newGreen, newBlue)
                            // Find corresponding view by ID and set the background color
                            // this avoid manually getting each viewColor by viewColor2 - 10
                            val resID = resources.getIdentifier("viewColor$i", "id", requireContext().packageName)
                            view.findViewById<View>(resID)?.setBackgroundColor(color)
                            view.findViewById<View>(resID)?.setOnClickListener {
                                val colorStrip = (view.findViewById<View>(resID)?.background as ColorDrawable).color
                                // Changed where the text will appear on the screen
                                val stripTextHex: TextView by lazy { view.findViewById(R.id.textView3) }
                                val colorHex = String.format("#%06X", 0xFFFFFF and colorStrip)
                                stripTextHex.text = "Color Strip\nHex: $colorHex"
                                // for the popup for color strip
                                //val red = Color.red(colorStrip)
                                //val green = Color.green(colorStrip)
                                //val blue = Color.blue(colorStrip)
                                //showPopup(it.context, it,"Hex: $colorHex\nRGB: ($red, $green, $blue)")
                            }
                        }

                        step = 5 // Color strip get the next lighter color use steps to update next rgb
                        for (i in 7..10) {
                            val newRed = 255.coerceAtMost(red + (i - 2) * step)
                            val newGreen = 255.coerceAtMost(green + (i - 2) * step)
                            val newBlue = 255.coerceAtMost(blue + (i - 2) * step)
                            val color = Color.argb(alpha, newRed, newGreen, newBlue)

                            // Find corresponding view by ID and set the background color
                            // this avoid manually getting each viewColor by viewColor2 - 10
                            val resID = resources.getIdentifier("viewColor$i", "id", requireContext().packageName)
                            view.findViewById<View>(resID)?.setBackgroundColor(color) //set the background color
                            view.findViewById<View>(resID)?.setOnClickListener {
                                val colorStrip = (view.findViewById<View>(resID)?.background as ColorDrawable).color
                                val stripTextHex: TextView by lazy { view.findViewById(R.id.textView3) }
                                val colorHex = String.format("#%06X", 0xFFFFFF and colorStrip)
                                stripTextHex.text = "Color Strip\nHex: $colorHex"
                                // for the popup for color strip
                                //val red = Color.red(colorStrip)
                                //val green = Color.green(colorStrip)
                                //val blue = Color.blue(colorStrip)
                                //showPopup(it.context, it,"Hex: $colorHex\nRGB: ($red, $green, $blue)")
                            }
                        }
                        // update the text for Hex and RGB to the target color
                        textHex.text = "Hex: #${Integer.toHexString(pixel).uppercase().substring(2)}"
                        textRGB.text = "RGB: ($red, $green, $blue)"
                        selectedColor = RGB(red,green,blue)

                        // Calculating luminance with standard weighted formula
                        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
                        // Changing text color based off luminance, making it more visible
                        val textColor = if (luminance > 0.5) Color.BLACK else Color.WHITE
                        // Changing color of text fields
                        textRGB.setTextColor(textColor)
                        textHex.setTextColor(textColor)
                        textName.setTextColor(textColor)
                        textViewRGB.setTextColor(textColor)
                        favoriteButton.backgroundTintList = ColorStateList.valueOf(textColor)

                        // Search the closest color when user lifts their finger
                        if (motionEvent.action == MotionEvent.ACTION_UP) {
                            // call the function to update the textView -> textName
                            searchClosestColor(red, green, blue)
                        }

                    } catch (e: Exception) {
                        Log.e("FindColorActivity", "Error: $e")
                    }
                }
            }
            true
        }
    }

    // Function to search inside the database for color to find the closest matching color name
    // example: searchClosestColor(red, green, blue)
    @SuppressLint("SetTextI18n")
    private fun searchClosestColor(targetRed: Int, targetGreen: Int, targetBlue: Int) {
        val targetColor = PaintFinder.PaintColor("", "", targetRed, targetGreen, targetBlue)
        val closestPaint = PaintFinder.findClosestPaint(targetColor, requireContext())
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


    private fun checkIfFavorited() {
        val sharedPreferences = requireContext().getSharedPreferences("UserSession", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "Guest")
        val favoriteButton = view?.findViewById<ImageButton>(R.id.favoriteButton)

        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val favoriteColors = document.get("favoriteColors") as? List<Map<String, Any>> ?: emptyList()
                        val isFavorite = favoriteColors.any { it["colorName"] == textName.text.toString() }

                        if (favoriteButton != null) {
                            favoriteButton.isSelected = isFavorite
                        }
                    }
                }
            }
    }


    private fun setupButtonClickListener() {

        view?.findViewById<View>(R.id.viewColor)?.setOnClickListener {
            val colorName = textName.text.toString()
            val rgbText = textViewRGB.text.toString()

            Log.d("DEBUG", "setupButtonClickListener - textName: $colorName, textViewRGB: $rgbText")

            // Ensure a valid color is selected before proceeding
            if (colorName.contains("No matching paint found") || rgbText.isEmpty()) {
                Log.e("ERROR", "setupButtonClickListener - No color loaded!")
                Toast.makeText(context, "No color loaded", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Extract RGB values from the text "(R, G, B)"
            val regex = Regex("\\((\\d+), (\\d+), (\\d+)\\)")
            val matchResult = regex.find(rgbText)

            if (matchResult != null) {
                val red = matchResult.groupValues[1].toInt()
                val green = matchResult.groupValues[2].toInt()
                val blue = matchResult.groupValues[3].toInt()
                val hexColor = String.format("#%02X%02X%02X", red, green, blue)

                try {
                    val color = hexColor.toColorInt()
                    val fragments = PaintInfoFragment().apply {
                        arguments = Bundle().apply {
                            putInt("selected_color", color)
                            putString("color_name", colorName.replace("Closest Paint: ", ""))
                            putString("color_hex", hexColor)
                        }
                    }

                    Log.d(
                        "DEBUG",
                        "Starting PaintInfoActivity with colorHex: $hexColor, colorName: ${
                            colorName.replace(
                                "Closest Paint: ",
                                ""
                            )
                        }"
                    )
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragments)
                        .addToBackStack(null)
                        .commit()
                } catch (e: IllegalArgumentException) {
                    Log.e("ERROR", "Invalid hex color format: $hexColor")
                    Toast.makeText(context, "Invalid color format", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("ERROR", "RGB format is incorrect: $rgbText")
                Toast.makeText(context, "Error processing color", Toast.LENGTH_SHORT).show()
            }
        }
    }




    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                }
            } else {
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
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

    private fun getDefaultBitmap(): Bitmap {
        return BitmapFactory.decodeResource(resources, R.drawable.colorsensor_home_banner)
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

    private fun saveImageWithBanner(
        context: FindColorFragment,
        originalBitmap: Bitmap,
        paintName: String,
        rgb: String,
        hex: String
    ){
        // Step 1: Create the banner
        val bannerHeight = 400
        val bannerBitmap = Bitmap.createBitmap(originalBitmap.width, bannerHeight, Bitmap.Config.ARGB_8888)
        val bannerCanvas = Canvas(bannerBitmap)
        bannerCanvas.drawColor(Color.BLACK)

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 80f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        bannerCanvas.drawText("$paintName", 20f, 60f, paint)
        bannerCanvas.drawText("$rgb", 20f, 160f, paint)
        bannerCanvas.drawText("$hex", 20f, 260f, paint)

        // Step 2: Combine the image
        val combinedHeight = bannerHeight + originalBitmap.height
        val combinedBitmap = Bitmap.createBitmap(originalBitmap.width, combinedHeight, Bitmap.Config.ARGB_8888)
        val combinedCanvas = Canvas(combinedBitmap)
        combinedCanvas.drawBitmap(bannerBitmap, 0f, 0f, null)
        combinedCanvas.drawBitmap(originalBitmap, 0f, bannerHeight.toFloat(), null)

        // Step 3: Save to DCIM/ColorSensor using MediaStore
        val filename = "combined_image_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/ColorSensor")
        }

        val imageUri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let { uri ->
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                combinedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            Toast.makeText(requireContext(), "Image saved to DCIM/ColorSensor", Toast.LENGTH_LONG).show()
        } ?: run {
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_LONG).show()
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
