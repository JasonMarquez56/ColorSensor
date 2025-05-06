package com.example.colorsensor

import ColorPickerDialogFragment
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.colorsensor.utils.PaintFinder

class ColorBlendingFragment : Fragment(), ColorPickerDialogFragment.OnColorSelectedListener {

    private var selectedBlock: View? = null  // Track which block was clicked
    private var color1Value: Int? = null  // Store selected color for blendColor1
    private var color2Value: Int? = null  // Store selected color for blendColor2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_color_blending, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //SettingsUtil.navigationBar(ColorBlendingActivity())

        val color1 = view.findViewById<View>(R.id.blendColor1)
        val color2 = view.findViewById<View>(R.id.blendColor2)
        val color3 = view.findViewById<View>(R.id.blendResult)

        val textName1 = view.findViewById<TextView>(R.id.closetColor1)
        val textRGB1 = view.findViewById<TextView>(R.id.RGB1)
        val textHex1 = view.findViewById<TextView>(R.id.Hex1)
        val textName2 = view.findViewById<TextView>(R.id.closetColor2)
        val textRGB2 = view.findViewById<TextView>(R.id.RGB2)
        val textHex2 = view.findViewById<TextView>(R.id.Hex2)
        val textName3 = view.findViewById<TextView>(R.id.closetColor3)
        val textRGB3 = view.findViewById<TextView>(R.id.RGB3)
        val textHex3 = view.findViewById<TextView>(R.id.Hex3)

        // Define the click listener for color blocks
        val clickListener = View.OnClickListener { view ->
            //selectedBlock = view
            //val dialog = ColorPickerDialogFragment()
            //dialog.setTargetFragment(this, 0)  // Make sure to set the target fragment
            //dialog.show(parentFragmentManager, "ColorPickerDialog")
        }

        // Set click listeners for color blocks
        color1.setOnClickListener(clickListener)
        color2.setOnClickListener(clickListener)
    }

    override fun onColorSelected(color: Int) {
        selectedBlock?.let { block ->
            updateBlockColor(block, color) // Apply color directly to the block

            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)

            when (block.id) {
                R.id.blendColor1 -> {
                    color1Value = color
                    updateColorInfo(red, green, blue, R.id.closetColor1, R.id.RGB1, R.id.Hex1)
                }
                R.id.blendColor2 -> {
                    color2Value = color
                    updateColorInfo(red, green, blue, R.id.closetColor2, R.id.RGB2, R.id.Hex2)
                }
            }

            updateBlendedColor()
        }
    }

    private fun updateBlendedColor() {
        val color3 = requireView().findViewById<View>(R.id.blendResult)

        if (color1Value != null && color2Value != null) {
            val blendedColor = blendColors(color1Value!!, color2Value!!)
            updateBlockColor(color3, blendedColor)

            val red = Color.red(blendedColor)
            val green = Color.green(blendedColor)
            val blue = Color.blue(blendedColor)

            updateColorInfo(red, green, blue, R.id.closetColor3, R.id.RGB3, R.id.Hex3)
        }
    }

    private fun updateBlockColor(view: View, color: Int) {
        // Set the background color of the block directly
        view.setBackgroundColor(color)
    }

    private fun updateColorInfo(red: Int, green: Int, blue: Int, colorNameId: Int, rgbId: Int, hexId: Int) {
        val colorName = requireView().findViewById<TextView>(colorNameId)
        val rgb = requireView().findViewById<TextView>(rgbId)
        val hex = requireView().findViewById<TextView>(hexId)

        // Update the color information for each block
        val rgbValue = "($red, $green, $blue)"
        val hexValue = "#${Integer.toHexString(red)}${Integer.toHexString(green)}${Integer.toHexString(blue)}"
        colorName.text = "Color: RGB $rgbValue"
        rgb.text = "RGB: $rgbValue"
        hex.text = "Hex: $hexValue"
    }

    private fun blendColors(color1: Int, color2: Int): Int {
        val r = (Color.red(color1) + Color.red(color2)) / 2
        val g = (Color.green(color1) + Color.green(color2)) / 2
        val b = (Color.blue(color1) + Color.blue(color2)) / 2

        return Color.rgb(r, g, b)
    }
}
