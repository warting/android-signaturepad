@file:Suppress("MagicNumber")

package se.warting.signaturecore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import se.warting.signaturecore.utils.Bezier
import se.warting.signaturecore.utils.ControlTimedPoints
import se.warting.signaturecore.utils.SignedListener
import se.warting.signaturecore.utils.SvgBuilder
import se.warting.signaturecore.utils.TimedPoint
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter

class SignatureSDK {

    // Event storage
    private val originalEvents = mutableListOf<Event>()
    private var iter: MutableIterator<Event> = mutableListOf<Event>().iterator()

    // Point tracking
    private val points = mutableListOf<TimedPoint>()
    private val pointsCache: MutableList<TimedPoint?> = ArrayList()

    companion object {
        const val DEFAULT_ATTR_PEN_MIN_WIDTH_PX = 3
        const val DEFAULT_ATTR_PEN_MAX_WIDTH_PX = 7
        const val DEFAULT_ATTR_PEN_COLOR = Color.BLACK
        const val DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT = 0.9f
        const val DEFAULT_ATTR_CLEAR_ON_DOUBLE_CLICK = false
    }

    // Touch tracking
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastVelocity = 0f
    private var lastWidth = 0f

    // SVG building
    private val svgBuilder = SvgBuilder()

    // Configuration
    private var minWidth = 0
    private var maxWidth = 0
    private var velocityFilterWeight = 0f
    private var signedListener: SignedListener? = null

    // Canvas and bitmap management
    private var signatureTransparentBitmap: Bitmap? = null
    private var signatureBitmapCanvas: Canvas? = null
    private val paint = Paint()

    init {
        // Fixed paint parameters
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
    }

    fun configure(
        minWidth: Int? = null,
        maxWidth: Int? = null,
        penColor: Int? = null,
        velocityFilterWeight: Float? = null
    ) {
        minWidth?.let {
            this.minWidth = it
        }
        maxWidth?.let {
            this.maxWidth = it
        }
        penColor?.let {
            this.paint.color = it
        }

        velocityFilterWeight?.let {
            this.velocityFilterWeight = it
        }

        maxWidth?.let { maxW ->
            minWidth?.let { minW ->
                this.lastWidth = (minW + maxW) / 2f
            }
        }
    }

    fun setOnSignedListener(listener: SignedListener?) {
        signedListener = listener
    }

    fun addEvent(event: Event) {
        originalEvents.add(event)
        processCurrentEvent(event)
    }

    private fun processCurrentEvent(event: Event) {
        val timestamp = event.timestamp
        val eventX = event.x
        val eventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                points.clear()
                lastTouchX = eventX
                lastTouchY = eventY

                addTimedPoint(
                    getNewTimedPoint(eventX, eventY, timestamp),
                    timestamp
                )

                signedListener?.onStartSigning()
                addTimedPoint(
                    getNewTimedPoint(eventX, eventY, timestamp),
                    timestamp
                )
            }

            MotionEvent.ACTION_MOVE -> {
                addTimedPoint(
                    getNewTimedPoint(eventX, eventY, timestamp),
                    timestamp
                )

                signedListener?.onSigning()
            }

            MotionEvent.ACTION_UP -> {
                addTimedPoint(
                    getNewTimedPoint(eventX, eventY, timestamp),
                    timestamp
                )

                signedListener?.onSigned()
            }

            else -> {
                throw IllegalStateException("Unknown Motion " + event.action)
            }
        }
    }

    private fun forward() {
        while (iter.hasNext()) {
            processCurrentEvent(iter.next())
        }
    }

    fun clear() {
        svgBuilder.clear()
        points.clear()
        originalEvents.clear()
        iter = originalEvents.iterator()
        lastVelocity = 0f
        lastWidth = (minWidth + maxWidth) / 2f
        signatureTransparentBitmap = null
        notifyListeners()
    }

    fun restoreEvents(events: List<Event>) {
        originalEvents.clear()
        originalEvents.addAll(events)

        // Clear current state to ensure clean slate
        svgBuilder.clear()
        points.clear()
        lastVelocity = 0f
        lastWidth = (minWidth + maxWidth) / 2f

        // Reset the iterator to beginning of events
        iter = originalEvents.iterator()

        // Clear bitmap to prevent drawing artifacts
        if (signatureTransparentBitmap != null) {
            // Get current dimensions
            val width = signatureTransparentBitmap!!.width
            val height = signatureTransparentBitmap!!.height

            // Clear and reinitialize bitmap
            signatureTransparentBitmap = null
            signatureBitmapCanvas = null
            initializeBitmap(width, height)

            // Process all events to recreate the signature with proper curves
            if (!originalEvents.isEmpty()) {
                forward()
            }
        }
    }

    fun getEvents(): List<Event> {
        return originalEvents.toList()
    }

    private fun notifyListeners() {
        if (points.isEmpty()) {
            signedListener?.onClear()
        } else {
            signedListener?.onSigned()
        }
    }

    val isEmpty: Boolean
        get() = points.isEmpty()

    fun getSignatureSvg(width: Int, height: Int): String {
        return svgBuilder.build(width, height)
    }

    fun initializeBitmap(width: Int, height: Int) {
        if (signatureTransparentBitmap == null && width > 0 && height > 0) {
            signatureTransparentBitmap = createBitmap(width, height).also {
                signatureBitmapCanvas = Canvas(it)
            }
        }
    }

    fun hasBitmap(): Boolean = signatureTransparentBitmap != null

    fun drawSignature(canvas: Canvas) {
        signatureTransparentBitmap?.let {
            forward()
            canvas.drawBitmap(it, 0f, 0f, paint)
        }
    }

    /**
     * Returns a bitmap containing the current signature.
     *
     * @param backgroundColor Color placed behind the signature
     * @param penColor Color of the signature itself
     */
    fun getSignatureBitmap(
        backgroundColor: Int = Color.WHITE,
        penColor: Int? = null,
    ): Bitmap? {
        signatureTransparentBitmap?.let { originalBitmap ->
            val bitmapToReturn = createBitmap(originalBitmap.width, originalBitmap.height)
            val canvas = Canvas(bitmapToReturn)
            canvas.drawColor(backgroundColor)
            canvas.drawBitmap(originalBitmap, 0f, 0f, penColor?.adjustPaint())
            return bitmapToReturn
        }
        return null
    }

    /**
     * Returns a bitmap containing the current signature.
     *
     * @param trimBlankSpace If true, bitmap is cropped to the signature bounds and surrounding blank space is removed
     * @param penColor Color of the signature line in the bitmap
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod", "ReturnCount")
    fun getTransparentSignatureBitmap(
        trimBlankSpace: Boolean = false,
        penColor: Int? = null,
    ): Bitmap? {
        val originalTransparentBitmap = signatureTransparentBitmap ?: return null

        val processedBitmap: Bitmap = penColor?.let { color ->
            val recoloured = createBitmap(originalTransparentBitmap.width, originalTransparentBitmap.height)
            val canvas = Canvas(recoloured)
            canvas.drawBitmap(originalTransparentBitmap, 0f, 0f, color.adjustPaint())
            recoloured
        } ?: originalTransparentBitmap

        if (!trimBlankSpace) {
            return processedBitmap
        }

        val bitmap = processedBitmap
        val imgHeight = bitmap.height
        val imgWidth = bitmap.width
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
                if (bitmap[x, y] != backgroundColor) {
                    xMin = x
                    stop = true
                    foundPixel = true
                    break
                }
            }
            if (stop) break
        }

        // Image is empty...
        if (!foundPixel) return bitmap

        // Find yMin
        for (y in 0 until imgHeight) {
            var stop = false
            for (x in xMin until imgWidth) {
                if (bitmap[x, y] != backgroundColor) {
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
                if (bitmap[x, y] != backgroundColor) {
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
                if (bitmap[x, y] != backgroundColor) {
                    yMax = y
                    stop = true
                    break
                }
            }
            if (stop) break
        }
        return Bitmap.createBitmap(
            bitmap,
            xMin,
            yMin,
            xMax - xMin,
            yMax - yMin
        )
    }

    private fun getNewTimedPoint(x: Float, y: Float, timestamp: Long): TimedPoint {
        val cacheSize = pointsCache.size
        val timedPoint: TimedPoint? = if (cacheSize == 0) {
            // Cache is empty, create a new point
            TimedPoint()
        } else {
            // Get point from cache
            pointsCache.removeAt(cacheSize - 1)
        }
        return timedPoint!!.set(x, y, timestamp)
    }

    private fun recyclePoint(point: TimedPoint?) {
        pointsCache.add(point)
    }

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

            velocity = (
                velocityFilterWeight * velocity +
                    (1 - velocityFilterWeight) * lastVelocity
                )

            // The new width is a function of the velocity. Higher velocities
            // correspond to thinner strokes.
            val newWidth = strokeWidth(velocity)

            // The Bezier's width starts out as last curve's final width, and
            // gradually changes to the stroke width just calculated. The new
            // width calculation is based on the velocity between the Bezier's
            // start and end mPoints.
            addBezier(curve, lastWidth, newWidth)
            lastVelocity = velocity
            lastWidth = newWidth

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

    private fun addBezier(curve: Bezier, startWidth: Float, endWidth: Float) {
        svgBuilder.append(curve, (startWidth + endWidth) / 2)
        val originalWidth = paint.strokeWidth
        val widthDelta = endWidth - startWidth
        val drawSteps = ceil(curve.length().toDouble()).toFloat()

        for (i in 0 until drawSteps.toInt()) {
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
            paint.strokeWidth = startWidth + ttt * widthDelta
            signatureBitmapCanvas?.drawPoint(x, y, paint)
        }
        paint.strokeWidth = originalWidth
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
        return max(maxWidth / (velocity + 1), minWidth.toFloat())
    }

    private fun Int.adjustPaint(): Paint = Paint().apply {
        colorFilter = PorterDuffColorFilter(this@adjustPaint, PorterDuff.Mode.SRC_IN)
        isAntiAlias = true
    }
}
