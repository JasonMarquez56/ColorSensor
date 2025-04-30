package com.example.colorsensor

import ColorPickerDialogFragment
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.colorsensor.utils.PaintFinder

class ColorBlendingFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_color_blending, container, false)
    }

    private var selectedBlock: View? = null  // Track which block was clicked
    private var color1Value: Int? = null  // Store selected color for blendColor1
    private var color2Value: Int? = null  // Store selected color for blendColor2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //SettingsUtil.navigationBar(this)

        val color3 = view?.findViewById<View>(R.id.blendResult)

        val textName1 = view?.findViewById<View>(R.id.closetColor1)
        val textRGB1 = view?.findViewById<View>(R.id.RGB1)
        val textHex1 = view?.findViewById<View>(R.id.Hex1)
        val textName2 = view?.findViewById<View>(R.id.closetColor2)
        val textRGB2 = view?.findViewById<View>(R.id.RGB2)
        val textHex2 = view?.findViewById<View>(R.id.Hex2)
        val textName3 = view?.findViewById<View>(R.id.closetColor3)
        val textRGB3 = view?.findViewById<View>(R.id.RGB3)
        val textHex3 = view?.findViewById<View>(R.id.Hex3)

        val clickListener = View.OnClickListener { view ->
            selectedBlock = view  // Store which block was clicked
            val dialog = ColorPickerDialogFragment()
            dialog.show(parentFragmentManager, "ColorPickerDialog")
        }

        view?.findViewById<View>(R.id.blendColor1)?.setOnClickListener(clickListener)
        view?.findViewById<View>(R.id.blendColor2)?.setOnClickListener(clickListener)
    }

    fun onColorSelected(color: Int) {
        selectedBlock?.let { block ->
            updateDrawableColor(block, color) // Apply color using drawable layers

            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)

            when (block.id) {
                R.id.blendColor1 -> {
                    color1Value = color
                    view?.let { searchClosestColor(red, green, blue, it.findViewById(R.id.closetColor1), requireView().findViewById(R.id.RGB1), requireView().findViewById(R.id.Hex1)) }
                }
                R.id.blendColor2 -> {
                    color2Value = color
                    searchClosestColor(red, green, blue, requireView().findViewById(R.id.closetColor2), requireView().findViewById(R.id.RGB2), requireView().findViewById(R.id.Hex2))
                }
            }

            updateBlendedColor()
        }
    }

    private fun updateBlendedColor() {
        val color3 = requireView().findViewById<View>(R.id.blendResult)

        if (color1Value != null && color2Value != null) {
            val blendedColor = blendColors(color1Value!!, color2Value!!)
            if (color3 != null) {
                updateDrawableColor(color3, blendedColor)
            }

            val red = Color.red(blendedColor)
            val green = Color.green(blendedColor)
            val blue = Color.blue(blendedColor)

            view?.let { searchClosestColor(red, green, blue, it.findViewById(R.id.closetColor3), requireView().findViewById(R.id.RGB3), requireView().findViewById(R.id.Hex3)) }
        }
    }

    private fun updateDrawableColor(view: View, color: Int) {
        val layerDrawable = view.background as? LayerDrawable ?: return
        val colorOverlay = layerDrawable.findDrawableByLayerId(R.id.colorOverlay) as? GradientDrawable
        colorOverlay?.setColor(color)
    }

    private fun blendColors(color1: Int, color2: Int): Int {
        val r = (Color.red(color1) + Color.red(color2)) / 2
        val g = (Color.green(color1) + Color.green(color2)) / 2
        val b = (Color.blue(color1) + Color.blue(color2)) / 2

        // accessbility mode
//        val accessbility: View by lazy { requireView().findViewById(R.id.viewColor12) }
//        val accessbilityText: TextView by lazy { requireView().findViewById(R.id.textViewAccessbilityName) }
//        val accessbilityHex: TextView by lazy { requireView().findViewById(R.id.textViewAccessbility) }
        // Set to default blank
//        accessbility.setBackgroundColor(Color.WHITE)
//        accessbilityHex.text = ""
//        accessbilityText.text = ""
//        when {
//            SettingsUtil.isProtanomalyEnabled(this) -> {
//                val protanopiaColor = SettingsUtil.hexToProtanomalyHex(r, g, b)
//                accessbility.setBackgroundColor(Color.parseColor(protanopiaColor))
//                accessbilityHex.text = "Hex: ${protanopiaColor.uppercase()}"
//                accessbilityText.text = "Protanomaly (Red-Blind)"
//            }
//
//            SettingsUtil.isDeuteranomalyEnabled(this) -> {
//                val deuteranomalyColor =
//                    SettingsUtil.hexToDeuteranomalyHex(r, g, b)
//                accessbility.setBackgroundColor(Color.parseColor(deuteranomalyColor))
//                accessbilityHex.text = "Hex: ${deuteranomalyColor.uppercase()}"
//                accessbilityText.text = "Deuteranomaly"
//            }
//
//            SettingsUtil.isTritanomalyEnabled(this) -> {
//                val tritanomalyColor = SettingsUtil.hexToTritanomalyHex(r, g, b)
//                accessbility.setBackgroundColor(Color.parseColor(tritanomalyColor))
//                accessbilityHex.text = "Hex: ${tritanomalyColor.uppercase()}"
//                accessbilityText.text = "Tritanomaly"
//            }
//        }
        return Color.rgb(r, g, b)
    }

    @SuppressLint("SetTextI18n")
    private fun searchClosestColor(targetRed: Int, targetGreen: Int, targetBlue: Int, textName: TextView, textViewRGB: TextView, textViewHex: TextView) {
        val targetColor = PaintFinder.PaintColor("", "", targetRed, targetGreen, targetBlue)
        val closestPaint = PaintFinder.findClosestPaint(targetColor, requireContext())
        // Setting XML values to correct paint and RGB when found
        if (closestPaint != null) {
            val closestRGB = "(${closestPaint.r}, ${closestPaint.g}, ${closestPaint.b})"
            val closestHex = rgbToHex(closestPaint.r, closestPaint.g, closestPaint.b)
            textName.text = "Closest Paint: ${closestPaint.name}"
            textViewRGB.text = "RGB: $closestRGB"
            textViewHex.text = "Hex: $closestHex"
        } else {
            textName.text = "No matching paint found"
            textViewRGB.text = ""
            textViewHex.text = ""
        }
    }

    private fun rgbToHex(red: Int, green: Int, blue: Int): String {
        return String.format("#%02X%02X%02X", red, green, blue)
    }

}