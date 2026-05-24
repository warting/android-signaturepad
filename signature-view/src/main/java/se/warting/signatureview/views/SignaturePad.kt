package se.warting.signatureview.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.FloatRange
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import se.warting.signaturecore.Event
import se.warting.signaturecore.ExperimentalSignatureApi
import se.warting.signaturecore.Signature
import se.warting.signaturecore.SignatureSDK
import se.warting.signaturecore.utils.SignedListener
import se.warting.signaturepad.view.BuildConfig
import se.warting.signaturepad.view.R
import kotlin.math.roundToInt

@SuppressWarnings("TooManyFunctions")
class SignaturePad(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val signatureSDK = SignatureSDK()

    // Configurable parameters
    private var mClearOnDoubleClick = false
    private var shadowPointerX = 0f
    private var shadowPointerY = 0f
    private var shadowPointerPressure = 0f

    // Double click detector
    private val doubleClickGestureDetector =
        GestureDetector(
            context,
            object : SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (mClearOnDoubleClick) {
                        clearView()
                        return true
                    }
                    return false
                }
            }
        )

    // Default attribute values now come from SignatureSDK class

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())

        bundle.putParcelableArray("events", signatureSDK.getEvents().toTypedArray())
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        var mutableState: Parcelable? = state
        if (state is Bundle) {
            val events: Array<Event> =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    state.getParcelableArray("events", Event::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    state.getParcelableArray("events")?.map { it as Event }?.toTypedArray()
                } ?: emptyArray()

            signatureSDK.restoreEvents(events.toList())

            mutableState =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    state.getParcelable("superState", Parcelable::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    state.getParcelable("superState")
                }
            invalidate()
        }
        super.onRestoreInstanceState(mutableState)
    }

    /**
     * Set the pen color from a given resource.
     *
     * @param colorRes the color resource.
     */
    fun setPenColorRes(@ColorRes colorRes: Int) {
        setPenColor(ContextCompat.getColor(context, colorRes))
    }

    /**
     * Set the pen color from a given color.
     *
     * @param color the color.
     */
    fun setPenColor(@ColorInt color: Int) {
        // Update the pen color in the SDK
        signatureSDK.configure(
            penColor = color,
        )
    }

    /**
     * Set the live finger shadow color from a given resource.
     *
     * @param colorRes the color resource.
     */
    fun setShadowColorRes(@ColorRes colorRes: Int) {
        setShadowColor(ContextCompat.getColor(context, colorRes))
    }

    /**
     * Set the live finger shadow color from a given color.
     *
     * @param color the color.
     */
    fun setShadowColor(@ColorInt color: Int) {
        signatureSDK.configureShadow(shadowColor = color)
        invalidate()
    }

    /**
     * Set the live finger shadow intensity. A value of 0 disables the shadow.
     *
     * @param intensity the shadow opacity multiplier between 0 and 1.
     */
    fun setShadowIntensity(@FloatRange(from = 0.0, to = 1.0) intensity: Float) {
        signatureSDK.configureShadow(shadowIntensity = intensity)
        invalidate()
    }

    /**
     * Set the live finger shadow angle in degrees.
     *
     * @param angleDegrees the shadow direction around the touch point.
     */
    fun setShadowAngleDegrees(@FloatRange(from = 0.0, to = 360.0) angleDegrees: Float) {
        signatureSDK.configureShadowAngle(angleDegrees)
        invalidate()
    }

    /**
     * Set the minimum width of the stroke in pixel.
     *
     * @param minWidth the width in dp.
     */
    fun setMinWidth(minWidth: Float) {
        val minWidthPx = convertDpToPx(minWidth)
        signatureSDK.configure(
            minWidth = minWidthPx,
        )
    }

    /**
     * Set the maximum width of the stroke in pixel.
     *
     * @param maxWidth the width in dp.
     */
    fun setMaxWidth(maxWidth: Float) {
        val maxWidthPx = convertDpToPx(maxWidth)
        signatureSDK.configure(
            maxWidth = maxWidthPx,
        )
    }

    /**
     * Set the velocity filter weight.
     *
     * @param velocityFilterWeight the weight.
     */
    fun setVelocityFilterWeight(velocityFilterWeight: Float) {
        signatureSDK.configure(
            velocityFilterWeight = velocityFilterWeight
        )
    }

    /**
     * Set clear on double tap.
     *
     * @param clearOnDoubleClick true if enabled.
     */
    fun setClearOnDoubleClick(clearOnDoubleClick: Boolean) {
        mClearOnDoubleClick = clearOnDoubleClick
    }

    fun clearView() {
        signatureSDK.clear()
        invalidate()
    }

    fun clear() {
        clearView()
    }

    /**
     * Undo the last stroke. Has no effect when there is nothing to undo.
     */
    fun undo() {
        signatureSDK.undo()
        invalidate()
    }

    /**
     * @return true if a stroke is available to undo.
     */
    fun canUndo(): Boolean = signatureSDK.canUndo()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val didDoubleClick = doubleClickGestureDetector.onTouchEvent(event)
        if (!isEnabled || didDoubleClick) {
            clearPointerShadow()
            invalidate()
            return false
        }

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                updatePointerShadow(event)
                val downEvent = Event(System.currentTimeMillis(), event.action, event.x, event.y)
                signatureSDK.addEvent(downEvent)
                invalidate()
                true
            }

            MotionEvent.ACTION_MOVE -> {
                updatePointerShadow(event)
                val moveEvent = Event(System.currentTimeMillis(), event.action, event.x, event.y)
                signatureSDK.addEvent(moveEvent)
                invalidate()
                true
            }

            MotionEvent.ACTION_UP -> {
                updatePointerShadow(event, pressure = 0f)
                val upEvent = Event(System.currentTimeMillis(), event.action, event.x, event.y)
                signatureSDK.addEvent(upEvent)
                invalidate()
                true
            }

            MotionEvent.ACTION_CANCEL -> {
                clearPointerShadow()
                invalidate()
                true
            }

            else -> {
                false
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        ensureSignatureBitmapInDraw()
        signatureSDK.drawPointerShadow(
            canvas = canvas,
            width = width,
            height = height,
            pointerX = shadowPointerX,
            pointerY = shadowPointerY,
            pressure = shadowPointerPressure,
        )
        signatureSDK.drawSignature(canvas)
    }

    fun setOnSignedListener(listener: SignedListener?) {
        signatureSDK.setOnSignedListener(listener)
    }

    val isEmpty: Boolean
        get() = signatureSDK.isEmpty

    fun getSignatureSvg(): String {
        return signatureSDK.getSignatureSvg(width, height)
    }

    /**
     * Returns the current signature as an SVG document with optional coloring.
     *
     * @param penColor ARGB color of the signature stroke. If null the stroke defaults to black.
     * @param backgroundColor ARGB color filled behind the signature. If null the SVG is transparent.
     */
    fun getSignatureSvg(
        @ColorInt penColor: Int? = null,
        @ColorInt backgroundColor: Int? = null,
    ): String {
        return signatureSDK.getSignatureSvg(width, height, penColor, backgroundColor)
    }

    @ExperimentalSignatureApi
    fun getSignature(): Signature {
        return Signature(BuildConfig.VERSION_CODE, signatureSDK.getEvents())
    }

    @ExperimentalSignatureApi
    fun setSignature(signature: Signature) {
        clear()
        signatureSDK.restoreEvents(signature.events)
    }

    fun getSignatureBitmap(): Bitmap {
        return signatureSDK.getSignatureBitmap() ?: createBitmap(1, 1)
    }

    fun getSignatureBitmap(
        backgroundColor: Int,
        penColor: Int? = null,
    ): Bitmap {
        return signatureSDK.getSignatureBitmap(backgroundColor, penColor)
            ?: createBitmap(1, 1)
    }

    fun getTransparentSignatureBitmap(
        trimBlankSpace: Boolean = false,
        penColor: Int? = null,
    ): Bitmap {
        return signatureSDK.getTransparentSignatureBitmap(trimBlankSpace, penColor)
            ?: createBitmap(1, 1)
    }

    private fun ensureSignatureBitmapInDraw() {
        if (!signatureSDK.hasBitmap() && width > 0 && height > 0) {
            signatureSDK.initializeBitmap(width, height)
        }
    }

    private fun convertDpToPx(dp: Float): Int {
        return (context.resources.displayMetrics.density * dp).roundToInt()
    }

    private fun updatePointerShadow(event: MotionEvent, pressure: Float = event.pressure) {
        shadowPointerX = event.x
        shadowPointerY = event.y
        shadowPointerPressure = pressure
    }

    private fun clearPointerShadow() {
        shadowPointerPressure = 0f
    }

    init {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SignaturePad,
            0,
            0
        )

        // Configurable parameters
        try {
            val minWidth = a.getDimensionPixelSize(
                R.styleable.SignaturePad_penMinWidth,
                convertDpToPx(SignatureSDK.DEFAULT_ATTR_PEN_MIN_WIDTH_PX.toFloat())
            )
            val maxWidth = a.getDimensionPixelSize(
                R.styleable.SignaturePad_penMaxWidth,
                convertDpToPx(SignatureSDK.DEFAULT_ATTR_PEN_MAX_WIDTH_PX.toFloat())
            )
            val penColor = a.getColor(
                R.styleable.SignaturePad_penColor,
                SignatureSDK.DEFAULT_ATTR_PEN_COLOR
            )
            val velocityFilterWeight = a.getFloat(
                R.styleable.SignaturePad_velocityFilterWeight,
                SignatureSDK.DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT
            )
            val shadowColor = a.getColor(
                R.styleable.SignaturePad_shadowColor,
                SignatureSDK.DEFAULT_ATTR_SHADOW_COLOR
            )
            val shadowIntensity = a.getFloat(
                R.styleable.SignaturePad_shadowIntensity,
                SignatureSDK.DEFAULT_ATTR_SHADOW_INTENSITY
            )
            val shadowAngleDegrees = a.getFloat(
                R.styleable.SignaturePad_shadowAngleDegrees,
                SignatureSDK.DEFAULT_ATTR_SHADOW_ANGLE_DEGREES
            )
            mClearOnDoubleClick = a.getBoolean(
                R.styleable.SignaturePad_clearOnDoubleClick,
                SignatureSDK.DEFAULT_ATTR_CLEAR_ON_DOUBLE_CLICK
            )

            // Configure the SignatureSDK with the attributes
            signatureSDK.configure(
                minWidth = minWidth,
                maxWidth = maxWidth,
                penColor = penColor,
                velocityFilterWeight = velocityFilterWeight
            )
            signatureSDK.configureShadow(
                shadowColor = shadowColor,
                shadowIntensity = shadowIntensity,
            )
            signatureSDK.configureShadowAngle(shadowAngleDegrees)
        } finally {
            a.recycle()
        }

        clearView()
    }
}
