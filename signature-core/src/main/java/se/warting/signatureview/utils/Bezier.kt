package se.warting.signatureview.utils

import kotlin.math.sqrt

@SuppressWarnings("MagicNumber")
data class Bezier(
    var startPoint: TimedPoint,
    var control1: TimedPoint,
    var control2: TimedPoint,
    var endPoint: TimedPoint,
) {
    fun length(): Float {
        val steps = 10
        var length = 0f
        var cx: Double
        var cy: Double
        var px = 0.0
        var py = 0.0
        var xDiff: Double
        var yDiff: Double
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            cx = point(
                t, startPoint.x, control1.x,
                control2.x, endPoint.x
            )
            cy = point(
                t, startPoint.y, control1.y,
                control2.y, endPoint.y
            )
            if (i > 0) {
                xDiff = cx - px
                yDiff = cy - py
                length += sqrt(xDiff * xDiff + yDiff * yDiff).toFloat()
            }
            px = cx
            py = cy
        }
        return length
    }

    fun point(t: Float, start: Float, c1: Float, c2: Float, end: Float): Double {
        return start * (1.0 - t) * (1.0 - t) * (1.0 - t) + 3.0 * c1 * (1.0 - t) * (1.0 - t) * t +
                3.0 * c2 * (1.0 - t) * t * t + end * t * t * t
    }
}
