package se.warting.signatureview.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import kotlinx.coroutines.Dispatchers
import se.warting.signaturecore.CanvasChangedListener
import se.warting.signaturecore.Event
import se.warting.signaturecore.EventManager
import se.warting.signaturecore.ExperimentalSignatureApi
import se.warting.signaturecore.Signature
import se.warting.signatureview.R
import java.util.UUID
import kotlin.math.roundToInt

@SuppressWarnings("TooManyFunctions")
class SignaturePad(context: Context, attrs: AttributeSet?) :
    View(context, attrs),
    CanvasChangedListener {

    val eventManager = EventManager(Dispatchers.IO, context, this)

    // View state

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

    // Default attribute values
    companion object {
        private const val DEFAULT_ATTR_PEN_MIN_WIDTH_PX = 3
        private const val DEFAULT_ATTR_PEN_MAX_WIDTH_PX = 7
        private const val DEFAULT_ATTR_PEN_COLOR = Color.BLACK
        private const val DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT = 0.9f
        private const val DEFAULT_ATTR_CLEAR_ON_DOUBLE_CLICK = false
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())
        bundle.putString("events", eventManager.sessionId().toString())
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val mutableState: Parcelable?
        if (state is Bundle) {
            state.getString("events")?.let {
                eventManager.restoreSession(UUID.fromString(it))
            }

            mutableState =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    state.getParcelable("superState", Parcelable::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    state.getParcelable("superState")
                }
        } else {
            mutableState = state
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
        eventManager.mPaint.color = color
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
        eventManager.clearView()
        eventManager.ensureSignatureBitmapInOnDraw(width, height)
        notifyListeners()
        invalidate()
    }

    /**
     * Set the minimum width of the stroke in pixel.
     *
     * @param minWidth the width in dp.
     */
    fun setMinWidth(minWidth: Float) {
        eventManager.mMinWidth = convertDpToPx(minWidth)
        eventManager.mLastWidth = (eventManager.mMinWidth + eventManager.mMaxWidth) / 2f
    }

    /**
     * Set the maximum width of the stroke in pixel.
     *
     * @param maxWidth the width in dp.
     */
    fun setMaxWidth(maxWidth: Float) {
        eventManager.mMaxWidth = convertDpToPx(maxWidth)
        eventManager.mLastWidth = (eventManager.mMinWidth + eventManager.mMaxWidth) / 2f
    }

    fun clear() {
        clearView()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val didDoubleClick = doubleClickGestureDetector.onTouchEvent(event)
        if (!isEnabled || didDoubleClick) return false

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                val downEvent = Event(
                    System.currentTimeMillis(),
                    event.action,
                    event.x,
                    event.y
                )
                eventManager.addEvent(downEvent)
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val moveEvent = Event(
                    System.currentTimeMillis(),
                    event.action,
                    event.x,
                    event.y
                )
                eventManager.addEvent(moveEvent)
                true
            }

            MotionEvent.ACTION_UP -> {
                val upEvent = Event(
                    System.currentTimeMillis(),
                    event.action,
                    event.x,
                    event.y
                )
                eventManager.addEvent(upEvent)
                true
            }

            else -> {
                false
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        eventManager.drawBitmap(canvas)
    }

    override fun onCanvasChanged() {
        invalidate2()
    }

    override fun invalidate2() {
        postInvalidate()
    }

    fun setOnSignedListener(listener: SignedListener?) {
        eventManager.mSignedListener = listener
    }

    val isEmpty: Boolean
        get() = eventManager.points.isEmpty()

    private fun notifyListeners() {
        if (eventManager.points.isEmpty()) {
            eventManager.mSignedListener?.onClear()
        } else {
            eventManager.mSignedListener?.onSigned()
        }
    }

    fun getSignatureSvg(): String {
        return eventManager.getSignatureSvg()
    }

    @ExperimentalSignatureApi
    fun getSignature(): Signature {
        return eventManager.getSignature()
    }

    @ExperimentalSignatureApi
    fun setSignature(signature: Signature) {
        clearView()
        eventManager.setSignature(signature)
    }

    fun getSignatureBitmap(): Bitmap {
        return eventManager.getSignatureBitmap()
    }

    fun getTransparentSignatureBitmap(trimBlankSpace: Boolean = false): Bitmap {
        return eventManager.getTransparentSignatureBitmap(trimBlankSpace)
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
            eventManager.mMinWidth = a.getDimensionPixelSize(
                R.styleable.SignaturePad_penMinWidth,
                convertDpToPx(DEFAULT_ATTR_PEN_MIN_WIDTH_PX.toFloat())
            )
            eventManager.mMaxWidth = a.getDimensionPixelSize(
                R.styleable.SignaturePad_penMaxWidth,
                convertDpToPx(DEFAULT_ATTR_PEN_MAX_WIDTH_PX.toFloat())
            )
            eventManager.mPaint.color =
                a.getColor(R.styleable.SignaturePad_penColor, DEFAULT_ATTR_PEN_COLOR)
            eventManager.mVelocityFilterWeight = a.getFloat(
                R.styleable.SignaturePad_velocityFilterWeight,
                DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT
            )
            mClearOnDoubleClick = a.getBoolean(
                R.styleable.SignaturePad_clearOnDoubleClick,
                DEFAULT_ATTR_CLEAR_ON_DOUBLE_CLICK
            )
        } finally {
            a.recycle()
        }

        // Fixed parameters
        eventManager.mPaint.isAntiAlias = true
        eventManager.mPaint.style = Paint.Style.STROKE
        eventManager.mPaint.strokeCap = Paint.Cap.ROUND
        eventManager.mPaint.strokeJoin = Paint.Join.ROUND

        clearView()
    }
}
