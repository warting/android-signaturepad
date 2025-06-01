package se.warting.signaturepad

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import se.warting.signaturepad.databinding.ActivityDatabindBinding
import se.warting.signatureview.views.SignedListener

class DataBindingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The layout for this activity is a Data Binding layout so it needs to be inflated using
        // DataBindingUtil.
        val binding: ActivityDatabindBinding = DataBindingUtil.setContentView(
            this, R.layout.activity_databind
        )
        val onStartSigning: SignedListener = object : SignedListener {
            override fun onStartSigning() {
                Log.d("SignedListener", "OnStartSigning")
            }

            override fun onSigning() {
                Log.d("SignedListener", "OnSigning")
            }

            override fun onSigned() {
                Log.d("SignedListener", "OnSigned")
                binding.saveButton.isEnabled = true
                binding.clearButton.isEnabled = true
                binding.undoButton.isEnabled = true
            }

            override fun onClear() {
                Log.d("SignedListener", "OnClear")

                binding.saveButton.isEnabled = false
                binding.clearButton.isEnabled = false
                binding.undoButton.isEnabled = false
            }
        }
        binding.binding = onStartSigning
        binding.clearButton.setOnClickListener { binding.signaturePad.clear() }
        binding.undoButton.setOnClickListener { binding.signaturePad.undo() }
        binding.saveButton.setOnClickListener {
            val signatureBitmap = binding.signaturePad.getSignatureBitmap()
            val signatureSvg = binding.signaturePad.getSignatureSvg()
            val transparentSignatureBitmap = binding.signaturePad.getTransparentSignatureBitmap()

            if (BuildConfig.DEBUG) {
                Log.d("DataBindingActivity", "Bitmap size: " + signatureBitmap.byteCount)
                Log.d(
                    "DataBindingActivity",
                    "Bitmap trasparent size: " + transparentSignatureBitmap.byteCount
                )
                Log.d("DataBindingActivity", "Svg length: " + signatureSvg.length)
            }
        }
    }
}
