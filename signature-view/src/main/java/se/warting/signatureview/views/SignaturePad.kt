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
import se.warting.signaturecore.Event
import se.warting.signaturecore.ExperimentalSignatureApi
import se.warting.signaturecore.Signature
import se.warting.signatureview.BuildConfig
import se.warting.signatureview.R
import se.warting.signatureview.utils.Bezier
import se.warting.signatureview.utils.ControlTimedPoints
import se.warting.signatureview.utils.SvgBuilder
import se.warting.signatureview.utils.TimedPoint
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

@SuppressWarnings("TooManyFunctions")
class SignaturePad(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val originalEvents = mutableListOf<Event>()
    private var iter: MutableIterator<Event> = mutableListOf<Event>().iterator()

    // View state
    private val points = mutableListOf<TimedPoint>()

    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mLastVelocity = 0f
    private var mLastWidth = 0f

    private val mSvgBuilder = SvgBuilder()

    // Cache
    private val mPointsCache: MutableList<TimedPoint?> = ArrayList()

    // Configurable parameters
    private var mMinWidth = 0
    private var mMaxWidth = 0
    private var mVelocityFilterWeight = 0f
    private var mSignedListener: SignedListener? = null
    private var mClearOnDoubleClick = false

    // Double click detector
    private val doubleClickGestureDetector =
        GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (mClearOnDoubleClick) {
                    clearView()
                    return true
                }
                return false
            }
        })

    // Default attribute values
    companion object {
        private const val DEFAULT_ATTR_PEN_MIN_WIDTH_PX = 3
        private const val DEFAULT_ATTR_PEN_MAX_WIDTH_PX = 7
        private const val DEFAULT_ATTR_PEN_COLOR = Color.BLACK
        private const val DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT = 0.9f
        private const val DEFAULT_ATTR_CLEAR_ON_DOUBLE_CLICK = false
    }

    private val mPaint = Paint()

    private var mSignatureTransparentBitmap: Bitmap? = null
    private var mSignatureBitmapCanvas: Canvas? = null
    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())

        bundle.putParcelableArray("events", originalEvents.toTypedArray())
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

            originalEvents.clear()
            originalEvents.addAll(events)
            iter = originalEvents.iterator()

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
        mPaint.color = color
    }

    /**
     * Set the minimum width of the stroke in pixel.
     *
     * @param minWidth the width in dp.
     */
    fun setMinWidth(minWidth: Float) {
        mMinWidth = convertDpToPx(minWidth)
        mLastWidth = (mMinWidth + mMaxWidth) / 2f
    }

    /**
     * Set the maximum width of the stroke in pixel.
     *
     * @param maxWidth the width in dp.
     */
    fun setMaxWidth(maxWidth: Float) {
        mMaxWidth = convertDpToPx(maxWidth)
        mLastWidth = (mMinWidth + mMaxWidth) / 2f
    }

    /**
     * Set the velocity filter weight.
     *
     * @param velocityFilterWeight the weight.
     */
    fun setVelocityFilterWeight(velocityFilterWeight: Float) {
        mVelocityFilterWeight = velocityFilterWeight
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
        mSvgBuilder.clear()
        points.clear()
        originalEvents.clear()
        iter = originalEvents.iterator()
        mLastVelocity = 0f
        mLastWidth = (mMinWidth + mMaxWidth) / 2f
        mLastWidth = ((mMinWidth + mMaxWidth) / 2).toFloat()
        mSignatureTransparentBitmap = null
        notifyListeners()
        invalidate()
    }

    fun clear() {
        clearView()
    }

    private fun addEvent(event: Event) {
        originalEvents.add(event)
        current(event)
    }

    private fun forward() {
        while (iter.hasNext()) {
            current(iter.next())
        }
    }

    private fun current(event: Event) {

        val timestamp = event.timestamp
        val eventX = event.x
        val eventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

                points.clear()
                mLastTouchX = eventX
                mLastTouchY = eventY

                addTimedPoint(
                    getNewTimedPoint(eventX, eventY, System.currentTimeMillis()),
                    timestamp
                )

                mSignedListener?.onStartSigning()
                addTimedPoint(
                    getNewTimedPoint(eventX, eventY, System.currentTimeMillis()),
                    timestamp
                )
            }

            MotionEvent.ACTION_MOVE -> {
                addTimedPoint(
                    getNewTimedPoint(eventX, eventY, System.currentTimeMillis()),
                    timestamp
                )

                mSignedListener?.onSigning()
            }

            MotionEvent.ACTION_UP -> {
                addTimedPoint(
                    getNewTimedPoint(eventX, eventY, System.currentTimeMillis()),
                    timestamp
                )

                mSignedListener?.onSigned()
            }

            else -> {
                throw IllegalStateException("Unknown Motion " + event.action)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val didDoubleClick = doubleClickGestureDetector.onTouchEvent(event)
        if (!isEnabled || didDoubleClick) return false

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                val downEvent = Event(System.currentTimeMillis(), event.action, event.x, event.y)
                addEvent(downEvent)
                invalidate()
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val moveEvent = Event(System.currentTimeMillis(), event.action, event.x, event.y)
                addEvent(moveEvent)
                invalidate()
                true
            }
            MotionEvent.ACTION_UP -> {
                val upEvent = Event(System.currentTimeMillis(), event.action, event.x, event.y)
                addEvent(upEvent)
                invalidate()
                true
            }
            else -> {
                false
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        ensureSignatureBitmapInOnDraw()
        mSignatureTransparentBitmap?.let {
            forward()
            canvas.drawBitmap(it, 0f, 0f, mPaint)
        }
    }

    fun setOnSignedListener(listener: SignedListener?) {
        mSignedListener = listener
    }

    val isEmpty: Boolean
        get() = points.isEmpty()

    private fun notifyListeners() {
        if (points.isEmpty()) {
            mSignedListener?.onClear()
        } else {
            mSignedListener?.onSigned()
        }
    }

    fun getSignatureSvg(): String {
        val width = mSignatureTransparentBitmap!!.width
        val height = mSignatureTransparentBitmap!!.height
        return mSvgBuilder.build(width, height)
    }

    @ExperimentalSignatureApi
    fun getSignature(): Signature {
        return Signature(BuildConfig.VERSION_CODE, originalEvents)
    }

    @ExperimentalSignatureApi
    fun setSignature(signature: Signature) {
        clear()
        originalEvents.addAll(signature.events)
        iter = originalEvents.iterator()
    }

    fun getSignatureBitmap(): Bitmap {
        val originalBitmap = mSignatureTransparentBitmap!!
        val whiteBgBitmap = Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(whiteBgBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        return whiteBgBitmap
    }

    @SuppressWarnings("LongMethod", "ComplexMethod", "ReturnCount")
    fun getTransparentSignatureBitmap(trimBlankSpace: Boolean = false): Bitmap {
        if (!trimBlankSpace) {
            return mSignatureTransparentBitmap!!
        }
        val imgHeight = mSignatureTransparentBitmap!!.height
        val imgWidth = mSignatureTransparentBitmap!!.width
        val backgroundColor = Color.TRANSPARENT
        var xMin = Int.MAX_VALUE
        var xMax = Int.MIN_VALUE
        var yMin = Int.MAX_VALUE
        var yMax = Int.MIN_VALUE
        var foundPixel = false

        // Find xMin
        for (x in 0 until imgWidth) {
            var stop = false
            for (y in 0 until imgHeight) {
                if (mSignatureTransparentBitmap!!.getPixel(x, y) != backgroundColor) {
                    xMin = x
                    stop = true
                    foundPixel = true
                    break
                }
            }
            if (stop) break
        }

        // Image is empty...
        if (!foundPixel) return mSignatureTransparentBitmap!!

        // Find yMin
        for (y in 0 until imgHeight) {
            var stop = false
            for (x in xMin until imgWidth) {
                if (mSignatureTransparentBitmap!!.getPixel(x, y) != backgroundColor) {
                    yMin = y
                    stop = true
                    break
                }
            }
            if (stop) break
        }

        // Find xMax
        for (x in imgWidth - 1 downTo xMin) {
            var stop = false
            for (y in yMin until imgHeight) {
                if (mSignatureTransparentBitmap!!.getPixel(x, y) != backgroundColor) {
                    xMax = x
                    stop = true
                    break
                }
            }
            if (stop) break
        }

        // Find yMax
        for (y in imgHeight - 1 downTo yMin) {
            var stop = false
            for (x in xMin..xMax) {
                if (mSignatureTransparentBitmap!!.getPixel(x, y) != backgroundColor) {
                    yMax = y
                    stop = true
                    break
                }
            }
            if (stop) break
        }
        return Bitmap.createBitmap(
            mSignatureTransparentBitmap!!,
            xMin,
            yMin,
            xMax - xMin,
            yMax - yMin
        )
    }

    private fun getNewTimedPoint(x: Float, y: Float, timestamp: Long): TimedPoint {
        val cacheSize = mPointsCache.size
        val timedPoint: TimedPoint? = if (cacheSize == 0) {
            // Cache is empty, create a new point
            TimedPoint()
        } else {
            // Get point from cache
            mPointsCache.removeAt(cacheSize - 1)
        }
        return timedPoint!!.set(x, y, timestamp)
    }

    private fun recyclePoint(point: TimedPoint?) {
        mPointsCache.add(point)
    }

    @SuppressWarnings("MagicNumber")
    private fun addTimedPoint(timedPoint: TimedPoint, timestamp: Long) {
        points.add(timedPoint)
        val pointsCount = points.size
        if (pointsCount > 3) {
            var tmp: ControlTimedPoints =
                calculateCurveControlPoints(points[0], points[1], points[2], timestamp)
            val c2 = tmp.c2
            recyclePoint(tmp.c1)
            tmp = calculateCurveControlPoints(points[1], points[2], points[3], timestamp)
            val c3 = tmp.c1
            recyclePoint(tmp.c2)
            val curve = Bezier(points[1], c2, c3, points[2])
            val startPoint = curve.startPoint
            val endPoint = curve.endPoint
            var velocity = endPoint.velocityFrom(startPoint)

            velocity = (mVelocityFilterWeight * velocity +
                    (1 - mVelocityFilterWeight) * mLastVelocity)

            // The new width is a function of the velocity. Higher velocities
            // correspond to thinner strokes.
            val newWidth = strokeWidth(velocity)

            // The Bezier's width starts out as last curve's final width, and
            // gradually changes to the stroke width just calculated. The new
            // width calculation is based on the velocity between the Bezier's
            // start and end mPoints.
            addBezier(curve, mLastWidth, newWidth)
            mLastVelocity = velocity
            mLastWidth = newWidth

            // Remove the first element from the list,
            // so that we always have no more than 4 mPoints in mPoints array.
            recyclePoint(points.removeAt(0))
            recyclePoint(c2)
            recyclePoint(c3)
        } else if (pointsCount == 1) {
            // To reduce the initial lag make it work with 3 mPoints
            // by duplicating the first point
            val firstPoint = points[0]
            points.add(getNewTimedPoint(firstPoint.x, firstPoint.y, timestamp))
        }
    }

    @SuppressWarnings("MagicNumber")
    private fun addBezier(curve: Bezier, startWidth: Float, endWidth: Float) {
        mSvgBuilder.append(curve, (startWidth + endWidth) / 2)
        val originalWidth = mPaint.strokeWidth
        val widthDelta = endWidth - startWidth
        val drawSteps = ceil(curve.length().toDouble()).toFloat()
        var i = 0
        while (i < drawSteps) {

            // Calculate the Bezier (x, y) coordinate for this step.
            val t = i.toFloat() / drawSteps
            val tt = t * t
            val ttt = tt * t
            val u = 1 - t
            val uu = u * u
            val uuu = uu * u
            var x = uuu * curve.startPoint.x
            x += 3 * uu * t * curve.control1.x
            x += 3 * u * tt * curve.control2.x
            x += ttt * curve.endPoint.x
            var y = uuu * curve.startPoint.y
            y += 3 * uu * t * curve.control1.y
            y += 3 * u * tt * curve.control2.y
            y += ttt * curve.endPoint.y

            // Set the incremental stroke width and draw.
            mPaint.strokeWidth = startWidth + ttt * widthDelta
            mSignatureBitmapCanvas!!.drawPoint(x, y, mPaint)
            i++
        }
        mPaint.strokeWidth = originalWidth
    }

    private fun calculateCurveControlPoints(
        s1: TimedPoint,
        s2: TimedPoint,
        s3: TimedPoint,
        timestamp: Long
    ): ControlTimedPoints {
        val dx1 = s1.x - s2.x
        val dy1 = s1.y - s2.y
        val dx2 = s2.x - s3.x
        val dy2 = s2.y - s3.y
        val m1X = (s1.x + s2.x) / 2.0f
        val m1Y = (s1.y + s2.y) / 2.0f
        val m2X = (s2.x + s3.x) / 2.0f
        val m2Y = (s2.y + s3.y) / 2.0f
        val l1 = sqrt((dx1 * dx1 + dy1 * dy1).toDouble()).toFloat()
        val l2 = sqrt((dx2 * dx2 + dy2 * dy2).toDouble()).toFloat()
        val dxm = m1X - m2X
        val dym = m1Y - m2Y
        var k = l2 / (l1 + l2)
        if (k.isNaN()) k = 0.0f
        val cmX = m2X + dxm * k
        val cmY = m2Y + dym * k
        val tx = s2.x - cmX
        val ty = s2.y - cmY
        return ControlTimedPoints(
            getNewTimedPoint(m1X + tx, m1Y + ty, timestamp),
            getNewTimedPoint(m2X + tx, m2Y + ty, timestamp)
        )
    }

    private fun strokeWidth(velocity: Float): Float {
        return max(mMaxWidth / (velocity + 1), mMinWidth.toFloat())
    }

    private fun ensureSignatureBitmapInOnDraw(): Boolean {
        if (mSignatureTransparentBitmap == null && width > 0 && height > 0) {
            mSignatureTransparentBitmap = Bitmap.createBitmap(
                width, height,
                Bitmap.Config.ARGB_8888
            ).also {
                mSignatureBitmapCanvas = Canvas(it)
            }
        }
        return mSignatureTransparentBitmap != null
    }

    private fun convertDpToPx(dp: Float): Int {
        return (context.resources.displayMetrics.density * dp).roundToInt()
    }

    init {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SignaturePad,
            0, 0
        )

        // Configurable parameters
        try {
            mMinWidth = a.getDimensionPixelSize(
                R.styleable.SignaturePad_penMinWidth,
                convertDpToPx(DEFAULT_ATTR_PEN_MIN_WIDTH_PX.toFloat())
            )
            mMaxWidth = a.getDimensionPixelSize(
                R.styleable.SignaturePad_penMaxWidth,
                convertDpToPx(DEFAULT_ATTR_PEN_MAX_WIDTH_PX.toFloat())
            )
            mPaint.color = a.getColor(R.styleable.SignaturePad_penColor, DEFAULT_ATTR_PEN_COLOR)
            mVelocityFilterWeight = a.getFloat(
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
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeJoin = Paint.Join.ROUND

        clearView()
    }
}
