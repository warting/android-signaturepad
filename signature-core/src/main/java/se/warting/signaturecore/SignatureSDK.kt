@file:Suppress("MagicNumber")

package se.warting.signaturecore

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RuntimeShader
import android.os.Build
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import se.warting.signaturecore.utils.Bezier
import se.warting.signaturecore.utils.ControlTimedPoints
import se.warting.signaturecore.utils.SignedListener
import se.warting.signaturecore.utils.SvgBuilder
import se.warting.signaturecore.utils.TimedPoint
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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
        const val DEFAULT_ATTR_SHADOW_COLOR = Color.BLACK
        const val DEFAULT_ATTR_SHADOW_INTENSITY = 0f
        const val DEFAULT_ATTR_SHADOW_ANGLE_DEGREES = 50.710594f

        private const val FULL_CIRCLE_DEGREES = 360f
    }

    // Touch tracking
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastVelocity = 0f
    private var lastWidth = 0f

    // Most recent finite touch coordinate. Used to substitute on a glitched
    // ACTION_UP so the stroke still terminates cleanly.
    private var lastValidX = 0f
    private var lastValidY = 0f

    // SVG building
    private val svgBuilder = SvgBuilder()

    // Configuration
    private var minWidth = 0
    private var maxWidth = 0
    private var velocityFilterWeight = 0f
    private var shadowColor = DEFAULT_ATTR_SHADOW_COLOR
    private var shadowIntensity = DEFAULT_ATTR_SHADOW_INTENSITY
    private var shadowAngleDegrees = DEFAULT_ATTR_SHADOW_ANGLE_DEGREES
    private var signedListener: SignedListener? = null

    // Canvas and bitmap management
    private var signatureTransparentBitmap: Bitmap? = null
    private var signatureBitmapCanvas: Canvas? = null
    private val paint = Paint()
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadowPointerRenderer = ShadowPointerRenderer()

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

    /**
     * Configure the live finger/pointer shadow drawn at the active touch.
     *
     * The shadow is disabled when [shadowIntensity] is 0f. Values above 1f are
     * clamped so callers can safely drive this from sliders or animations.
     */
    fun configureShadow(
        @ColorInt shadowColor: Int? = null,
        @FloatRange(from = 0.0, to = 1.0) shadowIntensity: Float? = null,
    ) {
        shadowColor?.let {
            this.shadowColor = it
        }
        shadowIntensity?.let {
            val clampedIntensity = it.coerceIn(0f, 1f)
            this.shadowIntensity = clampedIntensity
        }
    }

    /**
     * Configure the direction of the live finger/pointer shadow in degrees.
     *
     * The default angle preserves the shader direction used by Romain Guy's
     * shadow-pointer sample adaptation.
     */
    fun configureShadowAngle(
        @FloatRange(from = 0.0, to = 360.0) shadowAngleDegrees: Float,
    ) {
        if (shadowAngleDegrees.isFinite()) {
            this.shadowAngleDegrees = normalizeAngleDegrees(shadowAngleDegrees)
        }
    }

    private fun normalizeAngleDegrees(angleDegrees: Float): Float {
        val normalized = angleDegrees % FULL_CIRCLE_DEGREES
        return if (normalized < 0f) normalized + FULL_CIRCLE_DEGREES else normalized
    }

    fun setOnSignedListener(listener: SignedListener?) {
        signedListener = listener
    }

    fun addEvent(event: Event) {
        // Some devices/drivers occasionally deliver NaN or infinite touch
        // coordinates (e.g. palm rejection, stylus glitches). Propagating
        // those values produces NaN curve control points and crashes
        // downstream in roundToInt(). Filter at the input boundary.
        val sanitized = sanitizeEvent(event) ?: return
        originalEvents.add(sanitized)
        processCurrentEvent(sanitized)
    }

    private fun sanitizeEvent(event: Event): Event? {
        if (event.x.isFinite() && event.y.isFinite()) {
            lastValidX = event.x
            lastValidY = event.y
            return event
        }
        // ACTION_DOWN and ACTION_MOVE are safe to drop — we just lose one
        // sample. ACTION_UP must still be delivered or onSigned() never
        // fires and the stroke is left dangling (undo state, originalEvents
        // become inconsistent). Replay it with the last valid coordinate.
        return if (event.action == MotionEvent.ACTION_UP) {
            Event(event.timestamp, event.action, lastValidX, lastValidY)
        } else {
            null
        }
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

                notifyStartSigning()
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

                notifySigning()
            }

            MotionEvent.ACTION_UP -> {
                addTimedPoint(
                    getNewTimedPoint(eventX, eventY, timestamp),
                    timestamp
                )

                notifySigned()
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
        @Suppress("MemberExtensionConflict") // False positive? Try remove later
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
            val width = signatureTransparentBitmap!!.width
            val height = signatureTransparentBitmap!!.height
            signatureTransparentBitmap = null
            signatureBitmapCanvas = null
            initializeBitmap(width, height)
        }

        // Process all events so both the bitmap (if any) and the SVG builder
        // reflect the restored signature.
        if (originalEvents.isNotEmpty()) {
            forward()
        }
    }

    /**
     * Returns true when there is at least one completed stroke that can be
     * removed by [undo]. A stroke is completed once its [MotionEvent.ACTION_UP]
     * has been recorded.
     */
    fun canUndo(): Boolean = findLastCompletedStrokeStartIndex() >= 0

    /**
     * Removes the last completed stroke (from its [MotionEvent.ACTION_DOWN]
     * through its terminating [MotionEvent.ACTION_UP] and any events in
     * between) and redraws the signature. No-op when there is no completed
     * stroke to undo. An in-progress stroke is left untouched so the active
     * touch sequence stays consistent.
     */
    fun undo() {
        val lastCompletedStrokeStart = findLastCompletedStrokeStartIndex()
        if (lastCompletedStrokeStart < 0) return
        val remaining = originalEvents.subList(0, lastCompletedStrokeStart).toList()
        restoreEvents(remaining)
        if (remaining.isEmpty()) {
            signedListener?.onClear()
        } else {
            signedListener?.onSigned()
        }
    }

    private fun findLastCompletedStrokeStartIndex(): Int {
        val lastUp = originalEvents.indexOfLast { it.action == MotionEvent.ACTION_UP }
        return if (lastUp < 0) {
            -1
        } else {
            originalEvents.subList(0, lastUp + 1)
                .indexOfLast { it.action == MotionEvent.ACTION_DOWN }
        }
    }

    fun getEvents(): List<Event> {
        return originalEvents.toList()
    }

    private fun notifyListeners() {
        if (points.isEmpty()) {
            notifyClear()
        } else {
            notifySigned()
        }
    }

    private fun notifyStartSigning() {
        signedListener?.onStartSigning()
    }

    private fun notifySigning() {
        signedListener?.onSigning()
    }

    private fun notifySigned() {
        signedListener?.onSigned()
    }

    private fun notifyClear() {
        signedListener?.onClear()
    }

    val isEmpty: Boolean
        get() = points.isEmpty()

    fun getSignatureSvg(width: Int, height: Int): String {
        return svgBuilder.build(width, height)
    }

    /**
     * Returns the current signature as an SVG document.
     *
     * @param width Width of the SVG canvas in pixels.
     * @param height Height of the SVG canvas in pixels.
     * @param penColor ARGB color used for the signature stroke. If null, the stroke defaults to black.
     * @param backgroundColor ARGB color drawn as a filled rect behind the signature. If null, the
     *                        SVG has no background (the area is transparent).
     */
    fun getSignatureSvg(
        width: Int,
        height: Int,
        penColor: Int?,
        backgroundColor: Int? = null,
    ): String {
        return svgBuilder.build(width, height, penColor, backgroundColor)
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
            canvas.drawBitmap(it, 0f, 0f, bitmapPaint)
        }
    }

    /**
     * Draws a RuntimeShader-based finger shadow under the active pointer.
     *
     * This follows Romain Guy's capsule/cone soft-shadow shader on Android 13+
     * and is a no-op on older Android versions.
     */
    fun drawPointerShadow(
        canvas: Canvas,
        width: Int,
        height: Int,
        pointerX: Float,
        pointerY: Float,
        pressure: Float,
    ) {
        shadowPointerRenderer.draw(
            canvas = canvas,
            width = width,
            height = height,
            pointerX = pointerX,
            pointerY = pointerY,
            pressure = pressure,
            shadowColor = shadowColor,
            shadowIntensity = shadowIntensity,
            shadowAngleDegrees = shadowAngleDegrees,
        )
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
            return processedBitmap.copy(Bitmap.Config.ARGB_8888, false)
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

private data class ShadowFloat3(
    val x: Float,
    val y: Float,
    val z: Float,
) {
    fun magnitude(): Float = sqrt(x * x + y * y + z * z)

    operator fun minus(value: ShadowFloat3) = ShadowFloat3(
        x = x - value.x,
        y = y - value.y,
        z = z - value.z,
    )
}

private class ShadowPointerRenderer {
    private var shader: RuntimeShader? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    @SuppressLint("NewApi")
    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        pointerX: Float,
        pointerY: Float,
        pressure: Float,
        @ColorInt shadowColor: Int,
        shadowIntensity: Float,
        shadowAngleDegrees: Float,
    ) {
        if (shouldSkipDraw(width, height, pressure, shadowIntensity)) return

        val runtimeShader = shader ?: RuntimeShader(CAPSULE_SOFT_SHADOW_SHADER).also {
            shader = it
        }
        val normalizedPressure = pressure.coerceIn(0f, 1f)
        val shadowAlpha = (Color.alpha(shadowColor) / MAX_COLOR_COMPONENT) *
            shadowIntensity.coerceIn(0f, 1f) *
            sqrt(normalizedPressure)
        if (shadowAlpha <= 0f) return

        val maxDimension = 1.0f / max(width, height).toFloat()
        val fingerPosition = ShadowFloat3(
            x = (2.0f * pointerX - width) * maxDimension,
            y = (2.0f * pointerY - height) * maxDimension,
            z = FINGER_Z,
        )

        configureShader(
            shader = runtimeShader,
            fingerPosition = fingerPosition,
            shadowColor = Color.argb(
                (shadowAlpha * MAX_COLOR_COMPONENT).roundToInt().coerceIn(0, MAX_COLOR_COMPONENT_INT),
                Color.red(shadowColor),
                Color.green(shadowColor),
                Color.blue(shadowColor),
            ),
            shadowAngleDegrees = shadowAngleDegrees,
        )
        runtimeShader.setFloatUniform("size", width.toFloat(), height.toFloat())

        paint.shader = runtimeShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
    }

    private fun shouldSkipDraw(
        width: Int,
        height: Int,
        pressure: Float,
        shadowIntensity: Float,
    ): Boolean = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> true
        width <= 0 || height <= 0 -> true
        pressure <= 0f || shadowIntensity <= 0f -> true
        else -> false
    }

    @SuppressLint("NewApi")
    private fun configureShader(
        shader: RuntimeShader,
        fingerPosition: ShadowFloat3,
        @ColorInt shadowColor: Int,
        shadowAngleDegrees: Float,
    ) {
        val fingerDirection = rotateDefaultVector(FINGER_DIRECTION, shadowAngleDegrees)
        val lightOffset = rotateDefaultVector(LIGHT_POSITION - fingerPosition, shadowAngleDegrees)
        val lightPosition = ShadowFloat3(
            x = fingerPosition.x + lightOffset.x,
            y = fingerPosition.y + lightOffset.y,
            z = fingerPosition.z + lightOffset.z,
        )
        val fingerDirectionMagnitude = 1.0f / fingerDirection.magnitude()
        val lightDirection = lightPosition - fingerPosition
        val lightDirectionMagnitude = 1.0f / lightDirection.magnitude()

        shader.setColorUniform("backgroundColor", Color.TRANSPARENT)
        shader.setColorUniform("shadowColor", shadowColor)
        shader.setFloatUniform(
            "fingerPosition",
            fingerPosition.x,
            fingerPosition.y,
            fingerPosition.z,
        )
        shader.setFloatUniform(
            "fingerDirection",
            fingerDirection.x * fingerDirectionMagnitude,
            fingerDirection.y * fingerDirectionMagnitude,
            fingerDirection.z * fingerDirectionMagnitude,
        )
        shader.setFloatUniform("fingerLength", FINGER_LENGTH)
        shader.setFloatUniform("fingerSquareRadius", FINGER_RADIUS * FINGER_RADIUS)
        shader.setFloatUniform(
            "lightConeDirection",
            lightDirection.x * lightDirectionMagnitude,
            lightDirection.y * lightDirectionMagnitude,
            lightDirection.z * lightDirectionMagnitude,
        )

        val coneAngle = radians(LIGHT_ANGLE) * 0.5f
        shader.setFloatUniform("lightConeAngle", cos(coneAngle), coneAngle)
        shader.setFloatUniform(
            "fadeDistance",
            lightPosition.x,
            lightPosition.y,
            lightPosition.z,
            1.0f / (FADE_DISTANCE * FADE_DISTANCE),
        )
    }

    private fun rotateDefaultVector(vector: ShadowFloat3, angleDegrees: Float): ShadowFloat3 {
        val delta = radians(angleDegrees - SignatureSDK.DEFAULT_ATTR_SHADOW_ANGLE_DEGREES)
        val cosDelta = cos(delta)
        val sinDelta = sin(delta)
        return ShadowFloat3(
            x = vector.x * cosDelta - vector.y * sinDelta,
            y = vector.x * sinDelta + vector.y * cosDelta,
            z = vector.z,
        )
    }

    private fun radians(degrees: Float): Float = degrees * (FLOAT_PI / 180.0f)

    private companion object {
        private const val MAX_COLOR_COMPONENT = 255f
        private const val MAX_COLOR_COMPONENT_INT = 255
        private const val FLOAT_PI = 3.1415927f
        private const val FINGER_Z = -0.1f
        private const val FINGER_LENGTH = 0.9f
        private const val FINGER_RADIUS = 0.06f
        private const val LIGHT_ANGLE = 55.0f
        private const val FADE_DISTANCE = 3.0f

        private val FINGER_DIRECTION = ShadowFloat3(0.18f, 0.22f, -0.12f)
        private val LIGHT_POSITION = ShadowFloat3(0.0f, -1.0f, -1.3f)
    }
}

// Adapted from Romain Guy's shadow-pointer sample:
// https://github.com/romainguy/shadow-pointer (Apache-2.0).
private const val CAPSULE_SOFT_SHADOW_SHADER = """
layout(color) uniform half4 backgroundColor;
layout(color) uniform half4 shadowColor;
uniform vec3 fingerPosition;
uniform float fingerSquareRadius;
uniform vec3 fingerDirection;
uniform float fingerLength;
uniform vec3 lightConeDirection;
uniform vec2 lightConeAngle;
uniform vec2 size;
uniform vec4 fadeDistance;

const float PI = 3.1415927;

float sq(float x) {
    return x * x;
}

float acosFast(float x) {
    float y = abs(x);
    float p = -0.1565827 * y + 1.570796;
    p *= sqrt(1.0 - y);
    return x >= 0.0 ? p : PI - p;
}

float acosFastPositive(float x) {
    float p = -0.1565827 * x + 1.570796;
    return p * sqrt(1.0 - x);
}

float sphericalCapsIntersection(float cosCap1, float cosCap2, float cap2, float cosDistance) {
    float r1 = acosFastPositive(cosCap1);
    float r2 = cap2;
    float d  = acosFast(cosDistance);

    if (min(r1, r2) <= max(r1, r2) - d) {
        return 1.0 - max(cosCap1, cosCap2);
    } else if (r1 + r2 <= d) {
        return 0.0;
    }

    float delta = abs(r1 - r2);
    float x = 1.0 - saturate((d - delta) / max(r1 + r2 - delta, 0.0001));
    float area = sq(x) * (-2.0 * x + 3.0);
    return area * (1.0 - max(cosCap1, cosCap2));
}

float directionalOcclusionSphere(
    in vec3 pos,
    in vec4 sphere,
    in vec3 coneDirection,
    in vec2 coneAngle
) {
    vec3 occluder = sphere.xyz - pos;
    float occluderLength2 = dot(occluder, occluder);
    vec3 occluderDir = occluder * inversesqrt(occluderLength2);

    float cosPhi = dot(occluderDir, coneDirection);
    float cosTheta = sqrt(occluderLength2 / (sphere.w + occluderLength2));

    float occlusion =
        sphericalCapsIntersection(cosTheta, coneAngle.x, coneAngle.y, cosPhi) / (1.0 - coneAngle.x);
    return occlusion;
}

float directionalOcclusionCapsule(
    in vec3 pos,
    in vec3 capsuleA,
    in vec3 capsuleB,
    in float capsuleRadius,
    in vec3 coneDirection,
    in vec2 coneAngle
) {
    vec3 Ld = capsuleB - capsuleA;
    vec3 L0 = capsuleA - pos;
    float a = dot(coneDirection, Ld);
    float t = saturate(dot(L0, a * coneDirection - Ld) / (dot(Ld, Ld) - a * a));
    vec3 posToRay = capsuleA + t * Ld;

    return directionalOcclusionSphere(pos, vec4(posToRay, capsuleRadius), coneDirection, coneAngle);
}

half4 main(float2 fragCoord) {
    vec3 position = vec3((2.0 * fragCoord - size) / vec2(max(size.x, size.y)), 0.0);
    vec3 fingerEnd = fingerPosition + fingerDirection * fingerLength;
    float occlusion = directionalOcclusionCapsule(
        position,
        fingerPosition,
        fingerEnd,
        fingerSquareRadius,
        lightConeDirection,
        lightConeAngle
    );

    vec3 posToLight = fadeDistance.xyz - position;
    float distanceSquare = dot(posToLight, posToLight);
    float factor = distanceSquare * fadeDistance.w;
    float smoothFactor = max(1.0 - factor * factor, 0.0);
    float attenuation = (smoothFactor * smoothFactor) / max(distanceSquare, 1e-4);
    attenuation *= occlusion;
    return shadowColor * attenuation + (1.0 - attenuation * shadowColor.a) * backgroundColor;
}
"""
