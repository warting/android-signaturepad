package se.warting.signatureview.utils

import kotlin.math.roundToInt

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
        val roundedStrokeWidth: Int = strokeWidth.roundToInt()
        val curveStartSvgPoint = SvgPoint(curve.startPoint)
        val curveControlSvgPoint1 = SvgPoint(curve.control1)
        val curveControlSvgPoint2 = SvgPoint(curve.control2)
        val curveEndSvgPoint = SvgPoint(curve.endPoint)
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
