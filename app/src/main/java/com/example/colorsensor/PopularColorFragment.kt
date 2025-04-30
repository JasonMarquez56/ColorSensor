package com.example.colorsensor

import android.annotation.SuppressLint
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random
import androidx.core.graphics.toColorInt

class PopularColorFragment : Fragment() {
    private var magnifier: Magnifier? = null
    private var isMagnifierActive = false
    private lateinit var imageView: ImageView
    private lateinit var hexMessage: TextView
    private lateinit var textRGB: TextView
    private lateinit var bitmap: Bitmap
    private var rgbList = mutableListOf<Int>()
    private lateinit var firestore: FirebaseFirestore

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_popular_color, container, false)

        imageView = view.findViewById(R.id.imageView2)
        hexMessage = view.findViewById(R.id.textView9)
        textRGB = view.findViewById(R.id.textView11)

        firestore = FirebaseFirestore.getInstance()
        bitmap = BitmapFactory.decodeResource(resources, R.drawable.blank_wall)


        imageView.setOnTouchListener { _, event ->
            if (isMagnifierActive) {
                when (event.action) {
                    MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                        magnifier?.show(event.rawX, event.y)
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        magnifier?.dismiss()
                    }
                }
            }
            true
        }

        loadPopularColors(view)

        return view
    }

    private fun loadPopularColors(root: View) {
        val imageWidth = bitmap.width
        val imageHeight = bitmap.height
        val region = Rect(
            (0.0 * imageWidth).toInt(),
            (0.13 * imageHeight).toInt(),
            (0.735 * imageWidth).toInt(),
            (0.85 * imageHeight).toInt()
        )

        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val colors = document.get("favoriteColors") as? List<Map<String, Any>> ?: emptyList()
                    colors.mapNotNull { colorMap ->
                        val rgbMap = colorMap["rgb"] as? Map<String, Long>
                        val r = rgbMap?.get("r")?.toInt() ?: 0
                        val g = rgbMap?.get("g")?.toInt() ?: 0
                        val b = rgbMap?.get("b")?.toInt() ?: 0
                        rgbList.add(Color.argb(255, r, g, b))
                        null
                    }
                }

                rgbList.shuffle()
                rgbList = rgbList.toMutableSet().toMutableList()

                for (i in 0 until minOf(rgbList.size, 25)) {
                    val resID = resources.getIdentifier("button${i + 1}", "id", requireContext().packageName)
                    root.findViewById<Button>(resID)?.setBackgroundColor(rgbList[i])
                }

                for (i in rgbList.size until 25) {
                    val color = Color.argb(
                        255,
                        Random.nextInt(256),
                        Random.nextInt(256),
                        Random.nextInt(256)
                    )
                    val resID = resources.getIdentifier("button${i + 1}", "id", requireContext().packageName)
                    root.findViewById<Button>(resID)?.setBackgroundColor(color)
                }

                setupButtonListeners(root, region)
            }
    }

    @SuppressLint("SetTextI18n", "DiscouragedApi")
    private fun setupButtonListeners(root: View, targetRegion: Rect) {
        for (i in 1..25) {
            val buttonId = resources.getIdentifier("button$i", "id", requireContext().packageName)
            val button = root.findViewById<Button>(buttonId)
            button?.setOnClickListener {
                val color = (it.background as ColorDrawable).color
                val red = Color.red(color)
                val green = Color.green(color)
                val blue = Color.blue(color)
                val colorHex = String.format("#%06X", 0xFFFFFF and color)

                root.findViewById<View>(R.id.viewColor11).setBackgroundColor(color)
                hexMessage.text = "Hex: $colorHex"
                textRGB.text = "RGB: ($red, $green, $blue)"

//                when {
//                    SettingsUtil.isProtanomalyEnabled(requireContext()) -> {
//                        val hex = SettingsUtil.hexToProtanomalyHex(red, green, blue)
//                    }
//
//                    SettingsUtil.isDeuteranomalyEnabled(requireContext()) -> {
//                        val hex = SettingsUtil.hexToDeuteranomalyHex(red, green, blue)
//                    }
//
//                    SettingsUtil.isTritanomalyEnabled(requireContext()) -> {
//                        val hex = SettingsUtil.hexToTritanomalyHex(red, green, blue)
//                    }
//                }

                val modifiedBitmap = fillRegionWithColor(bitmap, targetRegion, color)
                imageView.setImageBitmap(modifiedBitmap)
            }
        }
    }

    private fun fillRegionWithColor(bitmap: Bitmap, region: Rect, newColor: Int): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pr = Color.red(newColor)
        val pg = Color.green(newColor)
        val pb = Color.blue(newColor)
        val pixels = IntArray(region.right - region.left)

        for (y in region.top until region.bottom) {
            bitmap.getPixels(pixels, 0, region.width(), region.left, y, region.width(), 1)

            for (x in pixels.indices) {
                val pixel = pixels[x]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val brightness = (r + g + b) / 3f / 255f
                val newR = (pr * brightness).toInt().coerceIn(0, 255)
                val newG = (pg * brightness).toInt().coerceIn(0, 255)
                val newB = (pb * brightness).toInt().coerceIn(0, 255)
                pixels[x] = Color.rgb(newR, newG, newB)
            }

            mutableBitmap.setPixels(pixels, 0, region.width(), region.left, y, region.width(), 1)
        }

        return mutableBitmap
    }
}
