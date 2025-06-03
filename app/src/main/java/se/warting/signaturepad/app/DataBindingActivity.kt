package se.warting.signaturepad.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import se.warting.signaturecore.utils.SignedListener
import se.warting.signaturepad.app.databinding.ActivityDatabindBinding

class DataBindingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // The layout for this activity is a Data Binding layout so it needs to be inflated using
        // DataBindingUtil.
        val binding: ActivityDatabindBinding = DataBindingUtil.setContentView(
            this, R.layout.activity_databind
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }


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
            }

            override fun onClear() {
                Log.d("SignedListener", "OnClear")

                binding.saveButton.isEnabled = false
                binding.clearButton.isEnabled = false
            }
        }
        binding.binding = onStartSigning
        binding.clearButton.setOnClickListener { binding.signaturePad.clear() }
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
