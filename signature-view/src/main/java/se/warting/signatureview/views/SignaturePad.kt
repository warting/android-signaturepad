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
                    state.getParcelable("events", Parcelable::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    state.getParcelable("events")
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val didDoubleClick = doubleClickGestureDetector.onTouchEvent(event)
        if (!isEnabled || didDoubleClick) return false

        // Validate coordinates to prevent NaN/infinite values from entering the system
        val x = if (event.x.isNaN() || event.x.isInfinite()) 0f else event.x
        val y = if (event.y.isNaN() || event.y.isInfinite()) 0f else event.y

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                val downEvent = Event(System.currentTimeMillis(), event.action, x, y)
                signatureSDK.addEvent(downEvent)
                invalidate()
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val moveEvent = Event(System.currentTimeMillis(), event.action, x, y)
                signatureSDK.addEvent(moveEvent)
                invalidate()
                true
            }

            MotionEvent.ACTION_UP -> {
                val upEvent = Event(System.currentTimeMillis(), event.action, x, y)
                signatureSDK.addEvent(upEvent)
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

    fun getTransparentSignatureBitmap(trimBlankSpace: Boolean = false): Bitmap {
        return signatureSDK.getTransparentSignatureBitmap(trimBlankSpace)
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
            val penColor = a.getColor(R.styleable.SignaturePad_penColor, SignatureSDK.DEFAULT_ATTR_PEN_COLOR)
            val velocityFilterWeight = a.getFloat(
                R.styleable.SignaturePad_velocityFilterWeight,
                SignatureSDK.DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT
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
        } finally {
            a.recycle()
        }

        clearView()
    }
}
