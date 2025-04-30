import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.skydoves.colorpickerview.ColorPickerView
import com.example.colorsensor.R
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Color
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import androidx.core.graphics.toColorInt

class ColorPickerDialogFragment : DialogFragment() {
    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null)

        val colorPicker: ColorPickerView = view.findViewById(R.id.colorPickerView)
        val hexInput: EditText = view.findViewById(R.id.hexInput)
        val colorPreview: View = view.findViewById(R.id.colorPreview)

        // Function to update preview based on color
        fun updatePreview(color: Int) {
            colorPreview.setBackgroundColor(color)
            hexInput.setText(String.format("#%06X", 0xFFFFFF and color))
        }

        // Color Picker Listener
        colorPicker.setColorListener(ColorEnvelopeListener { envelope, _ ->
            updatePreview(envelope.color)
        })

        // HEX Input Listener
        hexInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val hex = s.toString().trim()
                if (hex.matches(Regex("^#([A-Fa-f0-9]{6})$"))) {
                    try {
                        val color = Color.parseColor(hex)
                        colorPreview.setBackgroundColor(color)
                    } catch (e: IllegalArgumentException) {
                        // Invalid color (ignore)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        builder.setView(view)
            .setTitle("Select Color & Enter HEX")
            .setPositiveButton("OK") { _, _ ->
                val hexColor = hexInput.text.toString().trim()
                if (hexColor.matches(Regex("^#([A-Fa-f0-9]{6})$"))) {
                    val color = Color.parseColor(hexColor)
                    (activity as? OnColorSelectedListener)?.onColorSelected(color)
                }
            }
            .setNegativeButton("Cancel") { _, _ -> dismiss() }


        return builder.create()
    }

    interface OnColorSelectedListener {
        fun onColorSelected(color: Int)
    }
}

