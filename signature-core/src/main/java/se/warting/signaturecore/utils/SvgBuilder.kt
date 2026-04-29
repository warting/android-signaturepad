package se.warting.signaturecore.utils

import kotlin.math.roundToInt

class SvgBuilder {
    private val mSvgPathsBuilder = StringBuilder()
    private var mCurrentPathBuilder: SvgPathBuilder? = null
    fun clear() {
        mSvgPathsBuilder.setLength(0)
        mCurrentPathBuilder = null
    }

    fun build(width: Int, height: Int): String = build(width, height, null, null)

    fun build(
        width: Int,
        height: Int,
        penColor: Int?,
        backgroundColor: Int?,
    ): String {
        val sb = StringBuilder()
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
        appendBackground(sb, backgroundColor)
        sb.append("<g ")
            .append("stroke-linejoin=\"round\" ")
            .append("stroke-linecap=\"round\" ")
            .append("fill=\"none\" ")
        appendStroke(sb, penColor)
        sb.append(">")
            .append(mSvgPathsBuilder)
        if (isPathStarted) {
            sb.append(mCurrentPathBuilder)
        }
        sb.append("</g>")
            .append("</svg>")
        return sb.toString()
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

    private fun appendBackground(sb: StringBuilder, backgroundColor: Int?) {
        if (backgroundColor == null) return
        sb.append("<rect width=\"100%\" height=\"100%\" fill=\"")
            .append(toRgbHex(backgroundColor))
            .append("\"")
        val alpha = alphaFraction(backgroundColor)
        if (alpha < 1f) {
            sb.append(" fill-opacity=\"").append(formatOpacity(alpha)).append("\"")
        }
        sb.append("/>")
    }

    private fun appendStroke(sb: StringBuilder, penColor: Int?) {
        if (penColor == null) {
            sb.append("stroke=\"black\"")
            return
        }
        sb.append("stroke=\"").append(toRgbHex(penColor)).append("\"")
        val alpha = alphaFraction(penColor)
        if (alpha < 1f) {
            sb.append(" stroke-opacity=\"").append(formatOpacity(alpha)).append("\"")
        }
    }

    @Suppress("MagicNumber")
    private fun toRgbHex(argb: Int): String {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return "#%02X%02X%02X".format(r, g, b)
    }

    @Suppress("MagicNumber")
    private fun alphaFraction(argb: Int): Float = ((argb shr 24) and 0xFF) / 255f

    @Suppress("MagicNumber")
    private fun formatOpacity(alpha: Float): String {
        val rounded = (alpha * 1000f).roundToInt() / 1000f
        return rounded.toString().trimEnd('0').trimEnd('.').ifEmpty { "0" }
    }
}
