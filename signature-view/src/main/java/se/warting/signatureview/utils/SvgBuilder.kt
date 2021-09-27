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

internal class SvgBuilder {
    private val mSvgPathsBuilder = StringBuilder()
    private var mCurrentPathBuilder: SvgPathBuilder? = null
    fun clear() {
        mSvgPathsBuilder.setLength(0)
        mCurrentPathBuilder = null
    }

    fun build(width: Int, height: Int): String {
        if (isPathStarted) {
            appendCurrentPath()
        }
        return StringBuilder()
            .append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
            .append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.2\" baseProfile=\"tiny\" ")
            .append("height=\"")
            .append(height)
            .append("\" ")
            .append("width=\"")
            .append(width)
            .append("\" ")
            .append("viewBox=\"")
            .append(0)
            .append(" ")
            .append(0)
            .append(" ")
            .append(width)
            .append(" ")
            .append(height)
            .append("\">")
            .append("<g ")
            .append("stroke-linejoin=\"round\" ")
            .append("stroke-linecap=\"round\" ")
            .append("fill=\"none\" ")
            .append("stroke=\"black\"")
            .append(">")
            .append(mSvgPathsBuilder)
            .append("</g>")
            .append("</svg>")
            .toString()
    }

    fun append(curve: Bezier, strokeWidth: Float): SvgBuilder {
        val roundedStrokeWidth = Math.round(strokeWidth)
        val curveStartSvgPoint = SvgPoint(curve.startPoint!!)
        val curveControlSvgPoint1 = SvgPoint(curve.control1!!)
        val curveControlSvgPoint2 = SvgPoint(curve.control2!!)
        val curveEndSvgPoint = SvgPoint(curve.endPoint!!)
        if (!isPathStarted) {
            startNewPath(roundedStrokeWidth, curveStartSvgPoint)
        }
        if (!curveStartSvgPoint.equals(mCurrentPathBuilder?.lastPoint) ||
            roundedStrokeWidth != mCurrentPathBuilder?.strokeWidth
        ) {
            appendCurrentPath()
            startNewPath(roundedStrokeWidth, curveStartSvgPoint)
        }
        mCurrentPathBuilder!!.append(curveControlSvgPoint1, curveControlSvgPoint2, curveEndSvgPoint)
        return this
    }

    private fun startNewPath(roundedStrokeWidth: Int, curveStartSvgPoint: SvgPoint) {
        mCurrentPathBuilder = SvgPathBuilder(curveStartSvgPoint, roundedStrokeWidth)
    }

    private fun appendCurrentPath() {
        mSvgPathsBuilder.append(mCurrentPathBuilder)
    }

    private val isPathStarted: Boolean
        get() = mCurrentPathBuilder != null
}
