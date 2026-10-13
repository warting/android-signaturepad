package se.warting.signaturepad.app

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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import se.warting.signaturecore.SignatureSDK
import se.warting.signaturecore.utils.SignedListener
import se.warting.signatureview.views.SignaturePad
import kotlin.math.roundToInt

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_view, container, false)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        mSignaturePad.setPenColor(penColor)
        mSignaturePad.setMinWidth(penMinWidth)
        mSignaturePad.setMaxWidth(penMaxWidth)
        mSignaturePad.setShadowColor(shadowColor)
        mSignaturePad.setShadowIntensity(shadowIntensity)
        mSignaturePad.setShadowAngleDegrees(shadowAngleDegrees)

        val savedOutputViews = listOf(
            mBitmapLabel,
            mBitmapImageView,
            mTransparentBitmapLabel,
            mTransparentBitmapImageView,
            mSvgLabel,
            mSvgTextView,
        )

        val penColorButtons = listOf(
            view.findViewById<Button>(R.id.pen_red_button) to Color.RED,
            view.findViewById<Button>(R.id.pen_black_button) to Color.BLACK,
            view.findViewById<Button>(R.id.pen_white_button) to Color.WHITE,
        )
        val shadowColorButtons = listOf(
            view.findViewById<Button>(R.id.shadow_black_button) to Color.BLACK,
            view.findViewById<Button>(R.id.shadow_blue_button) to SHADOW_BLUE,
            view.findViewById<Button>(R.id.shadow_red_button) to Color.RED,
        )
        val imageBackgroundColorButtons = listOf(
            view.findViewById<Button>(R.id.image_background_red_button) to Color.RED,
            view.findViewById<Button>(R.id.image_background_green_button) to Color.GREEN,
            view.findViewById<Button>(R.id.image_background_blue_button) to Color.BLUE,
        )
        val imagePenColorButtons = listOf(
            view.findViewById<Button>(R.id.image_pen_red_button) to Color.RED,
            view.findViewById<Button>(R.id.image_pen_black_button) to Color.BLACK,
            view.findViewById<Button>(R.id.image_pen_white_button) to Color.WHITE,
        )

        listOf(
            mSaveButton,
            mClearButton,
            mUndoButton,
            mUseOverrideColorsCheckBox,
            mPenMinWidthLabel,
            mPenMaxWidthLabel,
            mShadowIntensityLabel,
            mShadowAngleLabel,
            mBitmapLabel,
            mTransparentBitmapLabel,
            mSvgLabel,
            mSvgTextView,
        ).forEach { it.setTextColor(Color.BLACK) }
        (penColorButtons + shadowColorButtons + imageBackgroundColorButtons + imagePenColorButtons)
            .forEach { (button, _) -> button.setTextColor(Color.BLACK) }

        penColorButtons.forEach { (button, color) ->
            val selected = color == penColor
            button.isSelected = selected
            button.alpha = if (selected) SELECTED_COLOR_BUTTON_ALPHA else UNSELECTED_COLOR_BUTTON_ALPHA
            button.setOnClickListener {
                penColor = color
                mSignaturePad.setPenColor(color)
                penColorButtons.forEach { (optionButton, optionColor) ->
                    val optionSelected = optionColor == color
                    optionButton.isSelected = optionSelected
                    optionButton.alpha = if (optionSelected) {
                        SELECTED_COLOR_BUTTON_ALPHA
                    } else {
                        UNSELECTED_COLOR_BUTTON_ALPHA
                    }
                }
            }
        }
        shadowColorButtons.forEach { (button, color) ->
            val selected = color == shadowColor
            button.isSelected = selected
            button.alpha = if (selected) SELECTED_COLOR_BUTTON_ALPHA else UNSELECTED_COLOR_BUTTON_ALPHA
            button.setOnClickListener {
                shadowColor = color
                mSignaturePad.setShadowColor(color)
                shadowColorButtons.forEach { (optionButton, optionColor) ->
                    val optionSelected = optionColor == color
                    optionButton.isSelected = optionSelected
                    optionButton.alpha = if (optionSelected) {
                        SELECTED_COLOR_BUTTON_ALPHA
                    } else {
                        UNSELECTED_COLOR_BUTTON_ALPHA
                    }
                }
            }
        }
        imageBackgroundColorButtons.forEach { (button, color) ->
            val selected = color == imageBackgroundColor
            button.isSelected = selected
            button.alpha = if (selected) SELECTED_COLOR_BUTTON_ALPHA else UNSELECTED_COLOR_BUTTON_ALPHA
            button.setOnClickListener {
                imageBackgroundColor = color
                imageBackgroundColorButtons.forEach { (optionButton, optionColor) ->
                    val optionSelected = optionColor == color
                    optionButton.isSelected = optionSelected
                    optionButton.alpha = if (optionSelected) {
                        SELECTED_COLOR_BUTTON_ALPHA
                    } else {
                        UNSELECTED_COLOR_BUTTON_ALPHA
                    }
                }
            }
        }
        imagePenColorButtons.forEach { (button, color) ->
            val selected = color == imagePenColor
            button.isSelected = selected
            button.alpha = if (selected) SELECTED_COLOR_BUTTON_ALPHA else UNSELECTED_COLOR_BUTTON_ALPHA
            button.setOnClickListener {
                imagePenColor = color
                imagePenColorButtons.forEach { (optionButton, optionColor) ->
                    val optionSelected = optionColor == color
                    optionButton.isSelected = optionSelected
                    optionButton.alpha = if (optionSelected) {
                        SELECTED_COLOR_BUTTON_ALPHA
                    } else {
                        UNSELECTED_COLOR_BUTTON_ALPHA
                    }
                }
            }
        }

        val widthSeekBarMax = ((PEN_WIDTH_MAX_DP - PEN_WIDTH_MIN_DP) * PEN_WIDTH_SCALE).roundToInt()
        mPenMinWidthSeekBar.max = widthSeekBarMax
        mPenMaxWidthSeekBar.max = widthSeekBarMax
        mPenMinWidthSeekBar.progress = ((penMinWidth - PEN_WIDTH_MIN_DP) * PEN_WIDTH_SCALE).roundToInt()
        mPenMaxWidthSeekBar.progress = ((penMaxWidth - PEN_WIDTH_MIN_DP) * PEN_WIDTH_SCALE).roundToInt()
        mPenMinWidthLabel.text = getString(R.string.pen_min_width_format, penMinWidth)
        mPenMaxWidthLabel.text = getString(R.string.pen_max_width_format, penMaxWidth)

        mPenMinWidthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                penMinWidth = PEN_WIDTH_MIN_DP + progress / PEN_WIDTH_SCALE
                if (penMaxWidth < penMinWidth) {
                    penMaxWidth = penMinWidth
                    mPenMaxWidthSeekBar.progress =
                        ((penMaxWidth - PEN_WIDTH_MIN_DP) * PEN_WIDTH_SCALE).roundToInt()
                }
                mSignaturePad.setMinWidth(penMinWidth)
                mPenMinWidthLabel.text = getString(R.string.pen_min_width_format, penMinWidth)
                mPenMaxWidthLabel.text = getString(R.string.pen_max_width_format, penMaxWidth)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        mPenMaxWidthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                penMaxWidth = PEN_WIDTH_MIN_DP + progress / PEN_WIDTH_SCALE
                if (penMinWidth > penMaxWidth) {
                    penMinWidth = penMaxWidth
                    mPenMinWidthSeekBar.progress =
                        ((penMinWidth - PEN_WIDTH_MIN_DP) * PEN_WIDTH_SCALE).roundToInt()
                }
                mSignaturePad.setMaxWidth(penMaxWidth)
                mPenMinWidthLabel.text = getString(R.string.pen_min_width_format, penMinWidth)
                mPenMaxWidthLabel.text = getString(R.string.pen_max_width_format, penMaxWidth)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        mShadowIntensitySeekBar.max = SHADOW_INTENSITY_SCALE.roundToInt()
        mShadowIntensitySeekBar.progress = (shadowIntensity * SHADOW_INTENSITY_SCALE).roundToInt()
        mShadowAngleSeekBar.max = SHADOW_ANGLE_MAX_DEGREES
        mShadowAngleSeekBar.progress = shadowAngleDegrees.roundToInt()
        mShadowIntensityLabel.text = getString(R.string.shadow_intensity_format, shadowIntensity)
        mShadowAngleLabel.text = getString(R.string.shadow_angle_format, shadowAngleDegrees)

        mShadowIntensitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                shadowIntensity = progress / SHADOW_INTENSITY_SCALE
                mSignaturePad.setShadowIntensity(shadowIntensity)
                mShadowIntensityLabel.text = getString(R.string.shadow_intensity_format, shadowIntensity)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        mShadowAngleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                shadowAngleDegrees = progress.toFloat()
                mSignaturePad.setShadowAngleDegrees(shadowAngleDegrees)
                mShadowAngleLabel.text = getString(R.string.shadow_angle_format, shadowAngleDegrees)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        mUseOverrideColorsCheckBox.isChecked = useOverrideColors
        mUseOverrideColorsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            useOverrideColors = isChecked
        }

        mSignaturePad.setOnSignedListener(object : SignedListener {

            override fun onStartSigning() {
                Log.d("SignedListener", "OnStartSigning")
            }

            override fun onSigning() {
                Log.d("SignedListener", "OnSigning")
            }

            override fun onSigned() {
                Log.d("SignedListener", "OnSigned")
                mSaveButton.isEnabled = !mSignaturePad.isEmpty
                mClearButton.isEnabled = !mSignaturePad.isEmpty
                mUndoButton.isEnabled = mSignaturePad.canUndo()
            }

            override fun onClear() {
                context?.let {
                    Toast.makeText(it, R.string.on_clear, Toast.LENGTH_SHORT).show()
                }
                mSaveButton.isEnabled = !mSignaturePad.isEmpty
                mClearButton.isEnabled = !mSignaturePad.isEmpty
                mUndoButton.isEnabled = mSignaturePad.canUndo()
            }
        })

        mClearButton.setOnClickListener { mSignaturePad.clear() }
        mUndoButton.setOnClickListener {
            mSignaturePad.undo()
            mSaveButton.isEnabled = !mSignaturePad.isEmpty
            mClearButton.isEnabled = !mSignaturePad.isEmpty
            mUndoButton.isEnabled = mSignaturePad.canUndo()
        }
        mSaveButton.setOnClickListener {
            val signatureBitmap = if (useOverrideColors) {
                mSignaturePad.getSignatureBitmap(imageBackgroundColor, imagePenColor)
            } else {
                mSignaturePad.getSignatureBitmap()
            }
            val signatureSvg = if (useOverrideColors) {
                mSignaturePad.getSignatureSvg(
                    penColor = imagePenColor,
                    backgroundColor = imageBackgroundColor,
                )
            } else {
                mSignaturePad.getSignatureSvg()
            }
            val transparentSignatureBitmap = if (useOverrideColors) {
                mSignaturePad.getTransparentSignatureBitmap(penColor = imagePenColor)
            } else {
                mSignaturePad.getTransparentSignatureBitmap()
            }
            mBitmapImageView.setImageBitmap(signatureBitmap)
            mTransparentBitmapImageView.setImageBitmap(transparentSignatureBitmap)
            mSvgTextView.text = signatureSvg
            savedOutputViews.forEach { it.visibility = View.VISIBLE }

            if (BuildConfig.DEBUG) {
                Log.d("ViewFragment", "Bitmap size: " + signatureBitmap.byteCount)
                Log.d(
                    "ViewFragment",
                    "Bitmap trasparent size: " + transparentSignatureBitmap.byteCount
                )
                Log.d("ViewFragment", "Svg length: " + signatureSvg.length)
            }
        }

        mSaveButton.isEnabled = !mSignaturePad.isEmpty
        mClearButton.isEnabled = !mSignaturePad.isEmpty
        mUndoButton.isEnabled = mSignaturePad.canUndo()
        savedOutputViews.forEach { it.visibility = View.GONE }
    }
}
