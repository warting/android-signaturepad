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

import kotlin.math.sqrt

internal data class Bezier(
    var startPoint: TimedPoint? = null,
    var control1: TimedPoint? = null,
    var control2: TimedPoint? = null,
    var endPoint: TimedPoint? = null,
) {

    operator fun set(
        startPoint: TimedPoint?,
        control1: TimedPoint?,
        control2: TimedPoint?,
        endPoint: TimedPoint?
    ): Bezier {
        this.startPoint = startPoint
        this.control1 = control1
        this.control2 = control2
        this.endPoint = endPoint
        return this
    }

    @SuppressWarnings("MagicNumber")
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
                t, startPoint!!.x, control1!!.x,
                control2!!.x, endPoint!!.x
            )
            cy = point(
                t, startPoint!!.y, control1!!.y,
                control2!!.y, endPoint!!.y
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

    @SuppressWarnings("MagicNumber")
    fun point(t: Float, start: Float, c1: Float, c2: Float, end: Float): Double {
        return start * (1.0 - t) * (1.0 - t) * (1.0 - t) + 3.0 * c1 * (1.0 - t) * (1.0 - t) * t +
                3.0 * c2 * (1.0 - t) * t * t + end * t * t * t
    }
}
