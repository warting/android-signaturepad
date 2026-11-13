package se.warting.signaturepad.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import se.warting.signaturecore.utils.SignedListener
import se.warting.signaturepad.app.databinding.ActivityDatabindBinding

class DataBindingSampleFragment : Fragment() {

    private lateinit var binding: ActivityDatabindBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using data binding
        binding = DataBindingUtil.inflate(
            inflater, R.layout.activity_databind, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup edge-to-edge content
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

        // Create the signature listener
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

        // Set up data binding and click listeners
        binding.binding = onStartSigning

        binding.clearButton.setOnClickListener { binding.signaturePad.clear() }

        binding.saveButton.setOnClickListener {
            val signatureBitmap = binding.signaturePad.getSignatureBitmap()
            val signatureSvg = binding.signaturePad.getSignatureSvg()
            val transparentSignatureBitmap = binding.signaturePad.getTransparentSignatureBitmap()

            if (BuildConfig.DEBUG) {
                Log.d("DataBindingFragment", "Bitmap size: " + signatureBitmap.byteCount)
                Log.d(
                    "DataBindingFragment",
                    "Bitmap transparent size: " + transparentSignatureBitmap.byteCount
                )
                Log.d("DataBindingFragment", "Svg length: " + signatureSvg.length)
            }
        }
    }
}
