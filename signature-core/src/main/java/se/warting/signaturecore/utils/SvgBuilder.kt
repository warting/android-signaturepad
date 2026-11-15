package se.warting.signaturecore.utils

import se.warting.signatureengine.CubicCurve
import se.warting.signatureengine.CurvePoint

/** Android API facade for the shared, platform-independent SVG writer. */
class SvgBuilder {
    private val delegate = se.warting.signatureengine.SvgBuilder()

    fun clear() = delegate.clear()

    fun build(width: Int, height: Int): String = delegate.build(width, height)

    fun build(width: Int, height: Int, penColor: Int?, backgroundColor: Int?): String =
        delegate.build(width, height, penColor, backgroundColor)

    fun append(curve: Bezier, strokeWidth: Float): SvgBuilder {
        fun TimedPoint.point() = CurvePoint(x, y)
        delegate.append(
            CubicCurve(
                curve.startPoint.point(),
                curve.control1.point(),
                curve.control2.point(),
                curve.endPoint.point()
            ),
            strokeWidth
        )
        return this
    }
}
