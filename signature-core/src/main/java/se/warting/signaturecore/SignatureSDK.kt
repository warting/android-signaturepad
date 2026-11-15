@file:Suppress("MagicNumber")

package se.warting.signaturecore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import se.warting.signaturecore.utils.Bezier
import se.warting.signaturecore.utils.SignedListener
import se.warting.signaturecore.utils.SvgBuilder
import se.warting.signaturecore.utils.TimedPoint
import kotlin.math.ceil

class SignatureSDK {

    /** Shared live-input lifecycle for Compose, Views and other input adapters. */
    val input: SignatureInput = SignatureInput(this)

    // Event storage
    private val originalEvents = mutableListOf<Event>()
    private val undoneStrokes = mutableListOf<UndoneStroke>()

    private data class UndoneStroke(val index: Int, val events: List<Event>)

    private val stroke = se.warting.signatureengine.StrokeBuilder { curve, startWidth, endWidth ->
        fun se.warting.signatureengine.CurvePoint.toTimedPoint() = TimedPoint().set(x, y, 0L)
        addBezier(
            Bezier(
                curve.start.toTimedPoint(),
                curve.control1.toTimedPoint(),
                curve.control2.toTimedPoint(),
                curve.end.toTimedPoint()
            ),
            startWidth,
            endWidth
        )
    }

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

    // Most recent finite touch coordinate. Used to substitute on a glitched
    // ACTION_UP so the stroke still terminates cleanly.
    private var lastValidX = 0f
    private var lastValidY = 0f

    // SVG building
    private val svgBuilder = SvgBuilder()

    // Configuration
    private var shadowColor = DEFAULT_ATTR_SHADOW_COLOR
    private var shadowIntensity = DEFAULT_ATTR_SHADOW_INTENSITY
    private var shadowAngleDegrees = DEFAULT_ATTR_SHADOW_ANGLE_DEGREES
    private var signedListener: SignedListener? = null

    // Canvas and bitmap management
    private var signatureTransparentBitmap: Bitmap? = null
    private var signatureBitmapCanvas: Canvas? = null
    private val paint = Paint()

    private val shadowPointerRenderer by lazy { createPointerShadowRenderer() }

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
        stroke.configure(minWidth, maxWidth, velocityFilterWeight)
        penColor?.let { paint.color = it }
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
            val clampedIntensity = if (it.isFinite()) it.coerceIn(0f, 1f) else 0f
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
        undoneStrokes.clear()
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
                stroke.beginStroke()

                stroke.add(eventX, eventY, timestamp)

                signedListener?.onStartSigning()
                stroke.add(eventX, eventY, timestamp)
            }

            MotionEvent.ACTION_MOVE -> {
                stroke.add(eventX, eventY, timestamp)

                signedListener?.onSigning()
            }

            MotionEvent.ACTION_UP -> {
                stroke.add(eventX, eventY, timestamp)

                signedListener?.onSigned()
            }

            else -> {
                throw IllegalStateException("Unknown Motion " + event.action)
            }
        }
    }

    fun clear() {
        undoneStrokes.clear()
        input.cancel()
        resetStrokeState()
        originalEvents.clear()
        // The view may have resized since allocation. Recreate at its current size.
        signatureTransparentBitmap = null
        signatureBitmapCanvas = null
        notifyListeners()
    }

    fun restoreEvents(events: List<Event>) {
        undoneStrokes.clear()
        input.cancel()
        replayEvents(events)
    }

    /**
     * Replace a signature and prepare its bitmap when the surface has been measured.
     * If dimensions are unavailable, [initializeBitmap] replays the events on the first draw.
     * Explicit restoration retains the existing clear and signing notifications; bitmap
     * initialization never repeats them.
     */
    fun restoreSignature(events: List<Event>, width: Int, height: Int) {
        clear()
        initializeBitmap(width, height)
        restoreEvents(events)
    }

    private fun replayEvents(events: List<Event>) {
        originalEvents.clear()
        @Suppress("MemberExtensionConflict") // False positive? Try remove later
        originalEvents.addAll(events)

        resetStrokeState()
        signatureTransparentBitmap?.eraseColor(Color.TRANSPARENT)

        // Process all events so both the bitmap (if any) and the SVG builder
        // reflect the restored signature.
        originalEvents.forEach(::processCurrentEvent)
    }

    private fun resetStrokeState() {
        svgBuilder.clear()
        stroke.reset()
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
        val lastUp = originalEvents.indexOfLast { it.action == MotionEvent.ACTION_UP }
        undoneStrokes.add(
            UndoneStroke(
                lastCompletedStrokeStart,
                originalEvents.subList(lastCompletedStrokeStart, lastUp + 1).toList()
            )
        )
        val remaining = originalEvents.take(lastCompletedStrokeStart) + originalEvents.drop(lastUp + 1)
        // Undo must preserve a live stroke and its active input pointer.
        replayEvents(remaining)
        if (remaining.isEmpty()) {
            signedListener?.onClear()
        } else {
            signedListener?.onSigned()
        }
    }

    /** Whether a stroke removed by [undo] can be restored by [redo]. */
    fun canRedo(): Boolean = undoneStrokes.isNotEmpty()

    /**
     * Restore the most recently undone stroke at its original position, preserving
     * any active input. New accepted input, [clear] and explicit restoration discard
     * redo history. Does nothing when there is no undone stroke.
     * Like legacy undo/restore, replay uses the current global pen settings; history
     * stores geometry and timing, not individual stroke styles.
     */
    fun redo() {
        if (undoneStrokes.isEmpty()) return
        val stroke = undoneStrokes.removeAt(undoneStrokes.lastIndex)
        val restored = originalEvents.take(stroke.index) + stroke.events + originalEvents.drop(stroke.index)
        replayEvents(restored)
        signedListener?.onSigned()
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
        if (stroke.isEmpty) {
            signedListener?.onClear()
        } else {
            signedListener?.onSigned()
        }
    }

    val isEmpty: Boolean
        get() = stroke.isEmpty

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

    /**
     * Match the raster surface to the measured size without scaling existing ink.
     * Resizing copies the overlapping pixels (cropping on shrink), preserving pen
     * colors, the active stroke, events and SVG without sending signing callbacks.
     */
    fun initializeBitmap(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val previousBitmap = signatureTransparentBitmap
        if (previousBitmap != null) {
            if (previousBitmap.width != width || previousBitmap.height != height) {
                val resizedBitmap = createBitmap(width, height)
                signatureBitmapCanvas = Canvas(resizedBitmap).apply {
                    drawBitmap(previousBitmap, 0f, 0f, null)
                }
                signatureTransparentBitmap = resizedBitmap
            }
        } else {
            signatureTransparentBitmap = createBitmap(width, height).also {
                signatureBitmapCanvas = Canvas(it)
            }
            // Events may have been restored before the view was measured.
            // Replay them once onto the new bitmap without repeating input callbacks.
            val listener = signedListener
            signedListener = null
            try {
                resetStrokeState()
                originalEvents.forEach(::processCurrentEvent)
            } finally {
                signedListener = listener
            }
        }
    }

    fun hasBitmap(): Boolean = signatureTransparentBitmap != null

    fun drawSignature(canvas: Canvas) {
        signatureTransparentBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, paint)
        }
    }

    /**
     * Draws a RuntimeShader-based finger shadow under the active pointer.
     *
     * This follows Romain Guy's capsule/cone soft-shadow shader on Android 13+
     * and is a no-op on older Android versions or software canvases.
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
        if (!foundPixel) return bitmap.copy(Bitmap.Config.ARGB_8888, false)

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
        val cropped = Bitmap.createBitmap(
            bitmap,
            xMin,
            yMin,
            xMax - xMin + 1,
            yMax - yMin + 1
        )
        // Android may return the source bitmap when the crop covers the whole image.
        return if (cropped === originalTransparentBitmap) {
            cropped.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            cropped
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

    private fun Int.adjustPaint(): Paint = Paint().apply {
        colorFilter = PorterDuffColorFilter(this@adjustPaint, PorterDuff.Mode.SRC_IN)
        isAntiAlias = true
    }
}
