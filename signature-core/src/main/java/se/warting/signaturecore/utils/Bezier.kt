@file:Suppress("MagicNumber")

package se.warting.signaturecore.utils

import se.warting.signatureengine.CubicCurve
import se.warting.signatureengine.CurvePoint

data class Bezier(
    var startPoint: TimedPoint,
    var control1: TimedPoint,
    var control2: TimedPoint,
    var endPoint: TimedPoint,
) {

    operator fun set(
        startPoint: TimedPoint,
        control1: TimedPoint,
        control2: TimedPoint,
        endPoint: TimedPoint
    ): Bezier {
        this.startPoint = startPoint
        this.control1 = control1
        this.control2 = control2
        this.endPoint = endPoint
        return this
    }

    fun length(): Float = CubicCurve(
        CurvePoint(startPoint.x, startPoint.y),
        CurvePoint(control1.x, control1.y),
        CurvePoint(control2.x, control2.y),
        CurvePoint(endPoint.x, endPoint.y),
    ).length()

    fun point(t: Float, start: Float, c1: Float, c2: Float, end: Float): Double =
        CubicCurve.coordinate(t, start, c1, c2, end)
}
