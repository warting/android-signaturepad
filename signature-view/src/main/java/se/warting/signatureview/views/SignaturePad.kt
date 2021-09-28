/*
 * MIT License
 *
 * Copyright (c) 2021. Stefan Wärting
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package se.warting.signatureview.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import java.util.ArrayList
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import se.warting.signatureview.R
import se.warting.signatureview.utils.Bezier
import se.warting.signatureview.utils.ControlTimedPoints
import se.warting.signatureview.utils.SvgBuilder
import se.warting.signatureview.utils.TimedPoint
import se.warting.signatureview.view.ViewCompat.isLaidOut
import se.warting.signatureview.view.ViewTreeObserverCompat.removeOnGlobalLayoutListener

@SuppressWarnings("TooManyFunctions")
class SignaturePad(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    // View state
    private var mPoints: MutableList<TimedPoint>? = null
    private var mIsEmpty = false
    private var mHasEditState: Boolean? = null
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mLastVelocity = 0f
    private var mLastWidth = 0f
    private val mDirtyRect: RectF
    private var mBitmapSavedState: Bitmap? = null
    private val mSvgBuilder = SvgBuilder()

    // Cache
    private val mPointsCache: MutableList<TimedPoint?> = ArrayList()
    private val mControlTimedPointsCached = ControlTimedPoints()
    private val mBezierCached = Bezier()

    // Configurable parameters
    private var mMinWidth = 0
    private var mMaxWidth = 0
    private var mVelocityFilterWeight = 0f
    private var mSignedListener: SignedListener? = null
    private var mClearOnDoubleClick = false

    // Double click detector
    private val mGestureDetector: GestureDetector

    // Default attribute values
    companion object {
        private const val DEFAULT_ATTR_PEN_MIN_WIDTH_PX = 3
        private const val DEFAULT_ATTR_PEN_MAX_WIDTH_PX = 7
        private const val DEFAULT_ATTR_PEN_COLOR = Color.BLACK
        private const val DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT = 0.9f
        private const val DEFAULT_ATTR_CLEAR_ON_DOUBLE_CLICK = false
    }

    private val mPaint = Paint()

    private var mSignatureBitmap: Bitmap? = null
    private var mSignatureBitmapCanvas: Canvas? = null
    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())
        if (mHasEditState == null || mHasEditState!!) {
            mBitmapSavedState = transparentSignatureBitmap
        }
        bundle.putParcelable("signatureBitmap", mBitmapSavedState)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        var mutableState: Parcelable? = state
        if (mutableState is Bundle) {
            val bundle = mutableState
            setSignatureBitmap(bundle.getParcelable<Parcelable>("signatureBitmap") as Bitmap?)
            mBitmapSavedState = bundle.getParcelable("signatureBitmap")
            mutableState = bundle.getParcelable("superState")
        }
        mHasEditState = false
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
    fun setPenColor(color: Int) {
        mPaint.color = color
    }

    /**
     * Set the minimum width of the stroke in pixel.
     *
     * @param minWidth the width in dp.
     */
    fun setMinWidth(minWidth: Float) {
        mMinWidth = convertDpToPx(minWidth)
    }

    /**
     * Set the maximum width of the stroke in pixel.
     *
     * @param maxWidth the width in dp.
     */
    fun setMaxWidth(maxWidth: Float) {
        mMaxWidth = convertDpToPx(maxWidth)
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
        mPoints = ArrayList()
        mLastVelocity = 0f
        mLastWidth = ((mMinWidth + mMaxWidth) / 2).toFloat()
        if (mSignatureBitmap != null) {
            mSignatureBitmap = null
            ensureSignatureBitmap()
        }
        isEmpty = true
        invalidate()
    }

    fun clear() {
        clearView()
        mHasEditState = true
    }

    @SuppressWarnings("ReturnCount")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        val eventX = event.x
        val eventY = event.y
        val bool = mGestureDetector.onTouchEvent(event)
        if (bool) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                mPoints!!.clear()
                mLastTouchX = eventX
                mLastTouchY = eventY
                addPoint(getNewPoint(eventX, eventY))
                if (mSignedListener != null) mSignedListener!!.onStartSigning()
                resetDirtyRect(eventX, eventY)
                addPoint(getNewPoint(eventX, eventY))
                isEmpty = false
            }
            MotionEvent.ACTION_MOVE -> {
                resetDirtyRect(eventX, eventY)
                addPoint(getNewPoint(eventX, eventY))
                isEmpty = false
            }
            MotionEvent.ACTION_UP -> {
                resetDirtyRect(eventX, eventY)
                addPoint(getNewPoint(eventX, eventY))
                parent.requestDisallowInterceptTouchEvent(true)
            }
            else -> return false
        }

        // invalidate();
        invalidate(
            (mDirtyRect.left - mMaxWidth).toInt(),
            (mDirtyRect.top - mMaxWidth).toInt(),
            (mDirtyRect.right + mMaxWidth).toInt(),
            (mDirtyRect.bottom + mMaxWidth).toInt()
        )
        return true
    }

    override fun onDraw(canvas: Canvas) {
        if (mSignatureBitmap != null) {
            canvas.drawBitmap(mSignatureBitmap!!, 0f, 0f, mPaint)
        }
    }

    fun setOnSignedListener(listener: SignedListener?) {
        mSignedListener = listener
    }

    var isEmpty: Boolean
        get() = mIsEmpty
        private set(newValue) {
            mIsEmpty = newValue
            if (mSignedListener != null) {
                if (mIsEmpty) {
                    mSignedListener!!.onClear()
                } else {
                    mSignedListener!!.onSigned()
                }
            }
        }
    val signatureSvg: String
        get() {
            val width = transparentSignatureBitmap.width
            val height = transparentSignatureBitmap.height
            return mSvgBuilder.build(width, height)
        }
    val signatureBitmap: Bitmap
        get() {
            val originalBitmap = transparentSignatureBitmap
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

    fun setSignatureBitmap(signature: Bitmap?) {
        // View was laid out...
        if (isLaidOut(this)) {
            clearView()
            ensureSignatureBitmap()
            val tempSrc = RectF()
            val tempDst = RectF()
            val dWidth = signature!!.width
            val dHeight = signature.height
            val vWidth = width
            val vHeight = height

            // Generate the required transform.
            tempSrc[0f, 0f, dWidth.toFloat()] = dHeight.toFloat()
            tempDst[0f, 0f, vWidth.toFloat()] = vHeight.toFloat()
            val drawMatrix = Matrix()
            drawMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER)
            val canvas = Canvas(mSignatureBitmap!!)
            canvas.drawBitmap(signature, drawMatrix, null)
            isEmpty = false
            invalidate()
        } else {
            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    // Remove layout listener...
                    removeOnGlobalLayoutListener(viewTreeObserver, this)

                    // Signature bitmap...
                    setSignatureBitmap(signature)
                }
            })
        }
    }

    val transparentSignatureBitmap: Bitmap
        get() {
            ensureSignatureBitmap()
            return mSignatureBitmap!!
        }

    @SuppressWarnings("LongMethod", "ComplexMethod", "ReturnCount")
    fun getTransparentSignatureBitmap(trimBlankSpace: Boolean): Bitmap? {
        if (!trimBlankSpace) {
            return transparentSignatureBitmap
        }
        ensureSignatureBitmap()
        val imgHeight = mSignatureBitmap!!.height
        val imgWidth = mSignatureBitmap!!.width
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
                if (mSignatureBitmap!!.getPixel(x, y) != backgroundColor) {
                    xMin = x
                    stop = true
                    foundPixel = true
                    break
                }
            }
            if (stop) break
        }

        // Image is empty...
        if (!foundPixel) return null

        // Find yMin
        for (y in 0 until imgHeight) {
            var stop = false
            for (x in xMin until imgWidth) {
                if (mSignatureBitmap!!.getPixel(x, y) != backgroundColor) {
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
                if (mSignatureBitmap!!.getPixel(x, y) != backgroundColor) {
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
                if (mSignatureBitmap!!.getPixel(x, y) != backgroundColor) {
                    yMax = y
                    stop = true
                    break
                }
            }
            if (stop) break
        }
        return Bitmap.createBitmap(mSignatureBitmap!!, xMin, yMin, xMax - xMin, yMax - yMin)
    }

    private fun onDoubleClick(): Boolean {
        if (mClearOnDoubleClick) {
            clearView()
            return true
        }
        return false
    }

    private fun getNewPoint(x: Float, y: Float): TimedPoint {
        val mCacheSize = mPointsCache.size
        val timedPoint: TimedPoint? = if (mCacheSize == 0) {
            // Cache is empty, create a new point
            TimedPoint()
        } else {
            // Get point from cache
            mPointsCache.removeAt(mCacheSize - 1)
        }
        return timedPoint!!.set(x, y)
    }

    private fun recyclePoint(point: TimedPoint?) {
        mPointsCache.add(point)
    }

    @SuppressWarnings("MagicNumber")
    private fun addPoint(newPoint: TimedPoint) {
        mPoints!!.add(newPoint)
        val pointsCount = mPoints!!.size
        if (pointsCount > 3) {
            var tmp = calculateCurveControlPoints(mPoints!![0], mPoints!![1], mPoints!![2])
            val c2 = tmp.c2
            recyclePoint(tmp.c1)
            tmp = calculateCurveControlPoints(mPoints!![1], mPoints!![2], mPoints!![3])
            val c3 = tmp.c1
            recyclePoint(tmp.c2)
            val curve = mBezierCached.set(mPoints!![1], c2, c3, mPoints!![2])
            val startPoint = curve.startPoint
            val endPoint = curve.endPoint
            var velocity = endPoint!!.velocityFrom(startPoint!!)
            velocity = if (java.lang.Float.isNaN(velocity)) 0.0f else velocity
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
            recyclePoint(mPoints!!.removeAt(0))
            recyclePoint(c2)
            recyclePoint(c3)
        } else if (pointsCount == 1) {
            // To reduce the initial lag make it work with 3 mPoints
            // by duplicating the first point
            val firstPoint = mPoints!![0]
            mPoints!!.add(getNewPoint(firstPoint.x, firstPoint.y))
        }
        mHasEditState = true
    }

    @SuppressWarnings("MagicNumber")
    private fun addBezier(curve: Bezier, startWidth: Float, endWidth: Float) {
        mSvgBuilder.append(curve, (startWidth + endWidth) / 2)
        ensureSignatureBitmap()
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
            var x = uuu * curve.startPoint!!.x
            x += 3 * uu * t * curve.control1!!.x
            x += 3 * u * tt * curve.control2!!.x
            x += ttt * curve.endPoint!!.x
            var y = uuu * curve.startPoint!!.y
            y += 3 * uu * t * curve.control1!!.y
            y += 3 * u * tt * curve.control2!!.y
            y += ttt * curve.endPoint!!.y

            // Set the incremental stroke width and draw.
            mPaint.strokeWidth = startWidth + ttt * widthDelta
            mSignatureBitmapCanvas!!.drawPoint(x, y, mPaint)
            expandDirtyRect(x, y)
            i++
        }
        mPaint.strokeWidth = originalWidth
    }

    private fun calculateCurveControlPoints(
        s1: TimedPoint,
        s2: TimedPoint,
        s3: TimedPoint
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
        if (java.lang.Float.isNaN(k)) k = 0.0f
        val cmX = m2X + dxm * k
        val cmY = m2Y + dym * k
        val tx = s2.x - cmX
        val ty = s2.y - cmY
        return mControlTimedPointsCached.set(
            getNewPoint(m1X + tx, m1Y + ty),
            getNewPoint(m2X + tx, m2Y + ty)
        )
    }

    private fun strokeWidth(velocity: Float): Float {
        return max(mMaxWidth / (velocity + 1), mMinWidth.toFloat())
    }

    /**
     * Called when replaying history to ensure the dirty region includes all
     * mPoints.
     *
     * @param historicalX the previous x coordinate.
     * @param historicalY the previous y coordinate.
     */
    private fun expandDirtyRect(historicalX: Float, historicalY: Float) {
        if (historicalX < mDirtyRect.left) {
            mDirtyRect.left = historicalX
        } else if (historicalX > mDirtyRect.right) {
            mDirtyRect.right = historicalX
        }
        if (historicalY < mDirtyRect.top) {
            mDirtyRect.top = historicalY
        } else if (historicalY > mDirtyRect.bottom) {
            mDirtyRect.bottom = historicalY
        }
    }

    /**
     * Resets the dirty region when the motion event occurs.
     *
     * @param eventX the event x coordinate.
     * @param eventY the event y coordinate.
     */
    private fun resetDirtyRect(eventX: Float, eventY: Float) {

        // The mLastTouchX and mLastTouchY were set when the ACTION_DOWN motion event occurred.
        mDirtyRect.left = min(mLastTouchX, eventX)
        mDirtyRect.right = max(mLastTouchX, eventX)
        mDirtyRect.top = min(mLastTouchY, eventY)
        mDirtyRect.bottom = max(mLastTouchY, eventY)
    }

    private fun ensureSignatureBitmap() {
        if (mSignatureBitmap == null) {
            mSignatureBitmap = Bitmap.createBitmap(
                width, height,
                Bitmap.Config.ARGB_8888
            ).also {
                mSignatureBitmapCanvas = Canvas(it)
            }
        }
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

        // Dirty rectangle to update only the changed portion of the view
        mDirtyRect = RectF()
        clearView()
        mGestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                return onDoubleClick()
            }
        })
    }
}
