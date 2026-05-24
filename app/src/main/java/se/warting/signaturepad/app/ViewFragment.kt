package se.warting.signaturepad.app

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import se.warting.signaturecore.SignatureSDK
import se.warting.signaturecore.utils.SignedListener
import se.warting.signatureview.views.SignaturePad
import kotlin.math.roundToInt

@Suppress("TooManyFunctions")
class ViewFragment : Fragment() {

    private lateinit var mSaveButton: Button
    private lateinit var mClearButton: Button
    private lateinit var mUndoButton: Button
    private lateinit var mSignaturePad: SignaturePad
    private lateinit var mPenMinWidthLabel: TextView
    private lateinit var mPenMaxWidthLabel: TextView
    private lateinit var mShadowIntensityLabel: TextView
    private lateinit var mShadowAngleLabel: TextView
    private lateinit var mPenMinWidthSeekBar: SeekBar
    private lateinit var mPenMaxWidthSeekBar: SeekBar
    private lateinit var mShadowIntensitySeekBar: SeekBar
    private lateinit var mShadowAngleSeekBar: SeekBar
    private lateinit var mUseOverrideColorsCheckBox: CheckBox
    private lateinit var mBitmapLabel: TextView
    private lateinit var mBitmapImageView: ImageView
    private lateinit var mTransparentBitmapLabel: TextView
    private lateinit var mTransparentBitmapImageView: ImageView
    private lateinit var mSvgLabel: TextView
    private lateinit var mSvgTextView: TextView

    private var penColor = Color.BLACK
    private var shadowColor = Color.BLACK
    private var imageBackgroundColor = Color.WHITE
    private var imagePenColor = Color.BLACK
    private var penMinWidth = DEFAULT_PEN_MIN_WIDTH_DP
    private var penMaxWidth = DEFAULT_PEN_MAX_WIDTH_DP
    private var shadowIntensity = DEFAULT_SHADOW_INTENSITY
    private var shadowAngleDegrees = SignatureSDK.DEFAULT_ATTR_SHADOW_ANGLE_DEGREES
    private var useOverrideColors = false

    private companion object {
        const val PEN_WIDTH_SCALE = 10f
        const val PEN_WIDTH_MIN_DP = 1f
        const val PEN_WIDTH_MAX_DP = 20f
        const val DEFAULT_PEN_MIN_WIDTH_DP = 3f
        const val DEFAULT_PEN_MAX_WIDTH_DP = 7f
        const val DEFAULT_SHADOW_INTENSITY = 0f
        const val SHADOW_INTENSITY_SCALE = 100f
        const val SHADOW_ANGLE_MAX_DEGREES = 360
        const val SELECTED_COLOR_BUTTON_ALPHA = 1f
        const val UNSELECTED_COLOR_BUTTON_ALPHA = 0.72f
        val SHADOW_BLUE: Int = Color.rgb(21, 101, 192)
    }

    private data class ColorOption(
        @param:IdRes val buttonId: Int,
        @param:ColorInt val color: Int
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEdgeToEdge(view)
        bindViews(view)
        setupInitialSignaturePadConfig()
        setupSignatureListener()
        setupColorControls(view)
        setupWidthControls()
        setupShadowControls()
        setupExportControls()
        setupButtons()
        updateActionButtons()
        updateSavedOutputVisibility(isVisible = false)
    }

    private fun setupEdgeToEdge(view: View) {
        val root = view.findViewById<View>(R.id.view_root)
        root.setBackgroundColor(Color.WHITE)
        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = initialLeft + bars.left,
                top = initialTop + bars.top,
                right = initialRight + bars.right,
                bottom = initialBottom + bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun bindViews(view: View) {
        mSaveButton = view.findViewById(R.id.save_button)
        mClearButton = view.findViewById(R.id.clear_button)
        mUndoButton = view.findViewById(R.id.undo_button)
        mSignaturePad = view.findViewById(R.id.signature_pad)
        mPenMinWidthLabel = view.findViewById(R.id.pen_min_width_label)
        mPenMaxWidthLabel = view.findViewById(R.id.pen_max_width_label)
        mShadowIntensityLabel = view.findViewById(R.id.shadow_intensity_label)
        mShadowAngleLabel = view.findViewById(R.id.shadow_angle_label)
        mPenMinWidthSeekBar = view.findViewById(R.id.pen_min_width_seek_bar)
        mPenMaxWidthSeekBar = view.findViewById(R.id.pen_max_width_seek_bar)
        mShadowIntensitySeekBar = view.findViewById(R.id.shadow_intensity_seek_bar)
        mShadowAngleSeekBar = view.findViewById(R.id.shadow_angle_seek_bar)
        mUseOverrideColorsCheckBox = view.findViewById(R.id.use_override_colors_check_box)
        mBitmapLabel = view.findViewById(R.id.bitmap_label)
        mBitmapImageView = view.findViewById(R.id.bitmap_image)
        mTransparentBitmapLabel = view.findViewById(R.id.transparent_bitmap_label)
        mTransparentBitmapImageView = view.findViewById(R.id.transparent_bitmap_image)
        mSvgLabel = view.findViewById(R.id.svg_label_text)
        mSvgTextView = view.findViewById(R.id.signature_svg_text)
        applyReadableTextColors(view)
    }

    private fun applyReadableTextColors(view: View) {
        when (view) {
            is TextView -> {
                if (view.id != R.id.signature_pad_description) {
                    view.setTextColor(Color.BLACK)
                }
            }
            is ViewGroup -> {
                for (index in 0 until view.childCount) {
                    applyReadableTextColors(view.getChildAt(index))
                }
            }
        }
    }

    private fun setupInitialSignaturePadConfig() {
        mSignaturePad.setPenColor(penColor)
        mSignaturePad.setMinWidth(penMinWidth)
        mSignaturePad.setMaxWidth(penMaxWidth)
        mSignaturePad.setShadowColor(shadowColor)
        mSignaturePad.setShadowIntensity(shadowIntensity)
        mSignaturePad.setShadowAngleDegrees(shadowAngleDegrees)
    }

    private fun setupSignatureListener() {
        mSignaturePad.setOnSignedListener(object : SignedListener {

            override fun onStartSigning() {
                Log.d("SignedListener", "onStartSigning")
            }

            override fun onSigning() {
                Log.d("SignedListener", "onSigning")
            }

            override fun onSigned() {
                Log.d("SignedListener", "onSigned")
                updateActionButtons()
            }

            override fun onClear() {
                context?.let {
                    Toast.makeText(it, R.string.on_clear, Toast.LENGTH_SHORT).show()
                }
                updateActionButtons()
            }
        })
    }

    private fun setupColorControls(view: View) {
        bindColorButtons(
            view = view,
            options = listOf(
                ColorOption(R.id.pen_red_button, Color.RED),
                ColorOption(R.id.pen_black_button, Color.BLACK),
                ColorOption(R.id.pen_white_button, Color.WHITE),
            ),
            selectedColor = penColor,
        ) {
            penColor = it
            mSignaturePad.setPenColor(it)
        }
        bindColorButtons(
            view = view,
            options = listOf(
                ColorOption(R.id.shadow_black_button, Color.BLACK),
                ColorOption(R.id.shadow_blue_button, SHADOW_BLUE),
                ColorOption(R.id.shadow_red_button, Color.RED),
            ),
            selectedColor = shadowColor,
        ) {
            shadowColor = it
            mSignaturePad.setShadowColor(it)
        }
        bindColorButtons(
            view = view,
            options = listOf(
                ColorOption(R.id.image_background_red_button, Color.RED),
                ColorOption(R.id.image_background_green_button, Color.GREEN),
                ColorOption(R.id.image_background_blue_button, Color.BLUE),
            ),
            selectedColor = imageBackgroundColor,
        ) {
            imageBackgroundColor = it
        }
        bindColorButtons(
            view = view,
            options = listOf(
                ColorOption(R.id.image_pen_red_button, Color.RED),
                ColorOption(R.id.image_pen_black_button, Color.BLACK),
                ColorOption(R.id.image_pen_white_button, Color.WHITE),
            ),
            selectedColor = imagePenColor,
        ) {
            imagePenColor = it
        }
    }

    private fun bindColorButtons(
        view: View,
        options: List<ColorOption>,
        @ColorInt selectedColor: Int,
        onSelect: (Int) -> Unit
    ) {
        val buttons = options.map { option -> option to view.findViewById<Button>(option.buttonId) }

        fun updateSelection(@ColorInt color: Int) {
            buttons.forEach { (option, button) ->
                val selected = option.color == color
                button.isSelected = selected
                button.alpha = if (selected) SELECTED_COLOR_BUTTON_ALPHA else UNSELECTED_COLOR_BUTTON_ALPHA
            }
        }

        updateSelection(selectedColor)
        buttons.forEach { (option, button) ->
            button.setOnClickListener {
                onSelect(option.color)
                updateSelection(option.color)
            }
        }
    }

    private fun setupWidthControls() {
        val widthSeekBarMax = widthToProgress(PEN_WIDTH_MAX_DP)
        mPenMinWidthSeekBar.max = widthSeekBarMax
        mPenMaxWidthSeekBar.max = widthSeekBarMax
        mPenMinWidthSeekBar.progress = widthToProgress(penMinWidth)
        mPenMaxWidthSeekBar.progress = widthToProgress(penMaxWidth)
        updateWidthLabels()

        mPenMinWidthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                penMinWidth = progressToWidth(progress)
                if (penMaxWidth < penMinWidth) {
                    penMaxWidth = penMinWidth
                    mPenMaxWidthSeekBar.progress = widthToProgress(penMaxWidth)
                }
                mSignaturePad.setMinWidth(penMinWidth)
                updateWidthLabels()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        mPenMaxWidthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                penMaxWidth = progressToWidth(progress)
                if (penMinWidth > penMaxWidth) {
                    penMinWidth = penMaxWidth
                    mPenMinWidthSeekBar.progress = widthToProgress(penMinWidth)
                }
                mSignaturePad.setMaxWidth(penMaxWidth)
                updateWidthLabels()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun setupShadowControls() {
        mShadowIntensitySeekBar.max = SHADOW_INTENSITY_SCALE.roundToInt()
        mShadowIntensitySeekBar.progress = (shadowIntensity * SHADOW_INTENSITY_SCALE).roundToInt()
        mShadowAngleSeekBar.max = SHADOW_ANGLE_MAX_DEGREES
        mShadowAngleSeekBar.progress = shadowAngleDegrees.roundToInt()
        updateShadowLabels()

        mShadowIntensitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                shadowIntensity = progress / SHADOW_INTENSITY_SCALE
                mSignaturePad.setShadowIntensity(shadowIntensity)
                updateShadowLabels()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        mShadowAngleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                shadowAngleDegrees = progress.toFloat()
                mSignaturePad.setShadowAngleDegrees(shadowAngleDegrees)
                updateShadowLabels()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun setupExportControls() {
        mUseOverrideColorsCheckBox.isChecked = useOverrideColors
        mUseOverrideColorsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            useOverrideColors = isChecked
        }
    }

    private fun setupButtons() {
        mClearButton.setOnClickListener { mSignaturePad.clear() }
        mUndoButton.setOnClickListener {
            mSignaturePad.undo()
            updateActionButtons()
        }
        mSaveButton.setOnClickListener { saveSignature() }
    }

    internal fun updateActionButtons() {
        mSaveButton.isEnabled = !mSignaturePad.isEmpty
        mClearButton.isEnabled = !mSignaturePad.isEmpty
        mUndoButton.isEnabled = mSignaturePad.canUndo()
    }

    private fun saveSignature() {
        val signatureBitmap = getSignatureBitmap()
        val transparentSignatureBitmap = getTransparentSignatureBitmap()
        val signatureSvg = getSignatureSvg()
        mBitmapImageView.setImageBitmap(signatureBitmap)
        mTransparentBitmapImageView.setImageBitmap(transparentSignatureBitmap)
        mSvgTextView.text = signatureSvg
        updateSavedOutputVisibility(isVisible = true)

        if (BuildConfig.DEBUG) {
            Log.d("ViewFragment", "Bitmap size: ${signatureBitmap.byteCount}")
            Log.d(
                "ViewFragment",
                "Bitmap transparent size: ${transparentSignatureBitmap.byteCount}"
            )
            Log.d("ViewFragment", "Svg length: ${signatureSvg.length}")
        }
    }

    private fun getSignatureBitmap(): Bitmap {
        return if (useOverrideColors) {
            mSignaturePad.getSignatureBitmap(imageBackgroundColor, imagePenColor)
        } else {
            mSignaturePad.getSignatureBitmap()
        }
    }

    private fun getTransparentSignatureBitmap(): Bitmap {
        return if (useOverrideColors) {
            mSignaturePad.getTransparentSignatureBitmap(penColor = imagePenColor)
        } else {
            mSignaturePad.getTransparentSignatureBitmap()
        }
    }

    private fun getSignatureSvg(): String {
        return if (useOverrideColors) {
            mSignaturePad.getSignatureSvg(
                penColor = imagePenColor,
                backgroundColor = imageBackgroundColor,
            )
        } else {
            mSignaturePad.getSignatureSvg()
        }
    }

    private fun updateSavedOutputVisibility(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        mBitmapLabel.visibility = visibility
        mBitmapImageView.visibility = visibility
        mTransparentBitmapLabel.visibility = visibility
        mTransparentBitmapImageView.visibility = visibility
        mSvgLabel.visibility = visibility
        mSvgTextView.visibility = visibility
    }

    internal fun updateWidthLabels() {
        mPenMinWidthLabel.text = getString(R.string.pen_min_width_format, penMinWidth)
        mPenMaxWidthLabel.text = getString(R.string.pen_max_width_format, penMaxWidth)
    }

    internal fun updateShadowLabels() {
        mShadowIntensityLabel.text = getString(R.string.shadow_intensity_format, shadowIntensity)
        mShadowAngleLabel.text = getString(R.string.shadow_angle_format, shadowAngleDegrees)
    }

    internal fun widthToProgress(width: Float): Int {
        return ((width - PEN_WIDTH_MIN_DP) * PEN_WIDTH_SCALE).roundToInt()
    }

    internal fun progressToWidth(progress: Int): Float {
        return PEN_WIDTH_MIN_DP + progress / PEN_WIDTH_SCALE
    }
}
