package se.warting.signaturecore.utils

import kotlin.math.roundToInt

/**
 * Represent a point as it would be in the generated SVG document.
 */
internal class SvgPoint {
    val x: Int
    val y: Int

    constructor(point: TimedPoint) {
        // one optimisation is to get rid of decimals as they are mostly non-significant in the
        // produced SVG image
        x = point.x.roundToInt()
        y = point.y.roundToInt()
    }

    constructor(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    private fun toAbsoluteCoordinates(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(x)
        stringBuilder.append(",")
        stringBuilder.append(y)
        return stringBuilder.toString()
    }

    fun toRelativeCoordinates(referencePoint: SvgPoint): String {
        return SvgPoint(x - referencePoint.x, y - referencePoint.y).toString()
    }

    override fun toString(): String {
        return toAbsoluteCoordinates()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val svgPoint = other as SvgPoint
        return if (x != svgPoint.x) false else y == svgPoint.y
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }
}
