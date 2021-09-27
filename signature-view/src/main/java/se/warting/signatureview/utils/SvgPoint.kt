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
