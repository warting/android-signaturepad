package se.warting.signatureengine

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/** Streaming smoothing and velocity-dependent widths, independent of a rendering surface. */
class StrokeBuilder(private val render: (CubicCurve, Float, Float) -> Unit) {
    private data class Sample(val x: Float, val y: Float, val timestamp: Long) {
        fun point() = CurvePoint(x, y)
        fun velocityFrom(start: Sample): Float {
            val distance = sqrt((start.x - x).toDouble().pow(2.0) + (start.y - y).toDouble().pow(2.0)).toFloat()
            return (distance / (timestamp - start.timestamp).coerceAtLeast(1L)).let {
                if (it.isFinite()) it else 0f
            }
        }
    }

    private val points = mutableListOf<Sample>()
    private var minWidth = 0
    private var maxWidth = 0
    private var velocityFilterWeight = 0f
    private var lastVelocity = 0f
    private var lastWidth = 0f
    val isEmpty: Boolean get() = points.isEmpty()

    fun configure(minWidth: Int? = null, maxWidth: Int? = null, velocityFilterWeight: Float? = null) {
        minWidth?.let { this.minWidth = it }
        maxWidth?.let { this.maxWidth = it }
        velocityFilterWeight?.let { this.velocityFilterWeight = it }
        if (minWidth != null && maxWidth != null) lastWidth = (minWidth + maxWidth) / 2f
    }

    /** A new gesture retains the historical velocity, matching the Android renderer. */
    fun beginStroke() { points.clear() }

    fun reset() {
        points.clear()
        lastVelocity = 0f
        lastWidth = (minWidth + maxWidth) / 2f
    }

    fun add(x: Float, y: Float, timestamp: Long) {
        points.add(Sample(x, y, timestamp))
        if (points.size > 3) {
            val c2 = controls(points[0], points[1], points[2]).second
            val c3 = controls(points[1], points[2], points[3]).first
            val curve = CubicCurve(points[1].point(), c2, c3, points[2].point())
            val velocity = velocityFilterWeight * points[2].velocityFrom(points[1]) +
                (1 - velocityFilterWeight) * lastVelocity
            val width = max(maxWidth / (velocity + 1), minWidth.toFloat())
            render(curve, lastWidth, width)
            lastVelocity = velocity
            lastWidth = width
            points.removeAt(0)
        } else if (points.size == 1) {
            points.add(points[0].copy(timestamp = timestamp))
        }
    }

    private fun controls(s1: Sample, s2: Sample, s3: Sample): Pair<CurvePoint, CurvePoint> {
        val dx1 = s1.x - s2.x
        val dy1 = s1.y - s2.y
        val dx2 = s2.x - s3.x
        val dy2 = s2.y - s3.y
        val m1X = (s1.x + s2.x) / 2f
        val m1Y = (s1.y + s2.y) / 2f
        val m2X = (s2.x + s3.x) / 2f
        val m2Y = (s2.y + s3.y) / 2f
        val l1 = sqrt((dx1 * dx1 + dy1 * dy1).toDouble()).toFloat()
        val l2 = sqrt((dx2 * dx2 + dy2 * dy2).toDouble()).toFloat()
        var k = l2 / (l1 + l2)
        if (k.isNaN()) k = 0f
        val tx = s2.x - (m2X + (m1X - m2X) * k)
        val ty = s2.y - (m2Y + (m1Y - m2Y) * k)
        return CurvePoint(m1X + tx, m1Y + ty) to CurvePoint(m2X + tx, m2Y + ty)
    }
}
