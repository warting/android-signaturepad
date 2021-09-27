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
package se.warting.signatureview.utils

/**
 * Build a SVG path as a string.
 *
 * https://www.w3.org/TR/SVGTiny12/paths.html
 */
internal class SvgPathBuilder(private val mStartPoint: SvgPoint, val strokeWidth: Int) {
    private val mStringBuilder: StringBuilder
    var lastPoint: SvgPoint
        private set

    fun append(
        controlPoint1: SvgPoint,
        controlPoint2: SvgPoint,
        endPoint: SvgPoint
    ): SvgPathBuilder {
        mStringBuilder.append(makeRelativeCubicBezierCurve(controlPoint1, controlPoint2, endPoint))
        lastPoint = endPoint
        return this
    }

    override fun toString(): String {
        return StringBuilder()
            .append("<path ")
            .append("stroke-width=\"")
            .append(strokeWidth)
            .append("\" ")
            .append("d=\"")
            .append(SVG_MOVE)
            .append(mStartPoint)
            .append(mStringBuilder)
            .append("\"/>")
            .toString()
    }

    private fun makeRelativeCubicBezierCurve(
        controlPoint1: SvgPoint,
        controlPoint2: SvgPoint,
        endPoint: SvgPoint
    ): String {
        val sControlPoint1 = controlPoint1.toRelativeCoordinates(lastPoint)
        val sControlPoint2 = controlPoint2.toRelativeCoordinates(lastPoint)
        val sEndPoint = endPoint.toRelativeCoordinates(lastPoint)
        val sb = StringBuilder()
        sb.append(sControlPoint1)
        sb.append(" ")
        sb.append(sControlPoint2)
        sb.append(" ")
        sb.append(sEndPoint)
        sb.append(" ")

        // discard zero curve
        val svg = sb.toString()
        return if ("c0 0 0 0 0 0" == svg) {
            ""
        } else {
            svg
        }
    }

    companion object {
        const val SVG_RELATIVE_CUBIC_BEZIER_CURVE = 'c'
        const val SVG_MOVE = 'M'
    }

    init {
        lastPoint = mStartPoint
        mStringBuilder = StringBuilder()
        mStringBuilder.append(SVG_RELATIVE_CUBIC_BEZIER_CURVE)
    }
}
