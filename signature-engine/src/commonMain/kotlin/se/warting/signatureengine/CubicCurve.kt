package se.warting.signatureengine

import kotlin.math.sqrt

/** Coordinates in the signature's logical drawing space, independent of a UI toolkit. */
data class CurvePoint(val x: Float, val y: Float)

/** Cubic geometry shared by Android rendering and multiplatform rendering. */
data class CubicCurve(
    val start: CurvePoint,
    val control1: CurvePoint,
    val control2: CurvePoint,
    val end: CurvePoint,
) {
    /** Matches the original Android engine's ten-segment length approximation. */
    fun length(): Float {
        var length = 0f
        var previousX = 0.0
        var previousY = 0.0
        for (index in 0..10) {
            val t = index.toFloat() / 10
            val x = coordinate(t, start.x, control1.x, control2.x, end.x)
            val y = coordinate(t, start.y, control1.y, control2.y, end.y)
            if (index > 0) {
                val dx = x - previousX
                val dy = y - previousY
                length += sqrt(dx * dx + dy * dy).toFloat()
            }
            previousX = x
            previousY = y
        }
        return length
    }

    companion object {
        fun coordinate(t: Float, start: Float, c1: Float, c2: Float, end: Float): Double =
            start * (1.0 - t) * (1.0 - t) * (1.0 - t) + 3.0 * c1 * (1.0 - t) * (1.0 - t) * t +
                3.0 * c2 * (1.0 - t) * t * t + end * t * t * t
    }
}
