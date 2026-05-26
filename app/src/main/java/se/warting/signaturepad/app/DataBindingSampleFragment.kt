package se.warting.signaturepad.app

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import se.warting.signaturecore.SignatureSDK
import se.warting.signaturecore.utils.SignedListener
import se.warting.signaturepad.app.databinding.ActivityDatabindBinding
import kotlin.math.roundToInt

class DataBindingSampleFragment : Fragment() {

    private lateinit var binding: ActivityDatabindBinding

    private companion object {
        const val SHADOW_INTENSITY_SCALE = 100f
        val SHADOW_BLUE: Int = Color.rgb(21, 101, 192)
    }

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

        binding.shadowAngleSeekBar.progress = SignatureSDK.DEFAULT_ATTR_SHADOW_ANGLE_DEGREES.roundToInt()
        binding.shadowBlackButton.setOnClickListener {
            binding.signaturePad.setShadowColor(Color.BLACK)
        }
        binding.shadowBlueButton.setOnClickListener {
            binding.signaturePad.setShadowColor(SHADOW_BLUE)
        }
        binding.shadowRedButton.setOnClickListener {
            binding.signaturePad.setShadowColor(Color.RED)
        }
        binding.shadowIntensitySeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.signaturePad.setShadowIntensity(progress / SHADOW_INTENSITY_SCALE)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            }
        )
        binding.shadowAngleSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.signaturePad.setShadowAngleDegrees(progress.toFloat())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            }
        )
    }
}
