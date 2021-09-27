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

import kotlin.math.pow
import kotlin.math.sqrt

internal class TimedPoint {
    var x = 0f
    var y = 0f
    var timestamp: Long = 0

    operator fun set(x: Float, y: Float): TimedPoint {
        this.x = x
        this.y = y
        timestamp = System.currentTimeMillis()
        return this
    }

    fun velocityFrom(start: TimedPoint): Float {
        var diff = timestamp - start.timestamp
        if (diff <= 0) {
            diff = 1
        }
        var velocity = distanceTo(start) / diff
        if (java.lang.Float.isInfinite(velocity) || java.lang.Float.isNaN(velocity)) {
            velocity = 0f
        }
        return velocity
    }

    private fun distanceTo(point: TimedPoint): Float {
        return sqrt(
            (point.x - x).toDouble().pow(2.0) + (point.y - y).toDouble().pow(2.0)
        ).toFloat()
    }
}
