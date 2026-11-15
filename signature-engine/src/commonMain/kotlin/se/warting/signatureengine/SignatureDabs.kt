package se.warting.signatureengine

import kotlin.math.ceil

/** Visit the circular marks that rasterize a variable-width curve using the original algorithm. */
inline fun SignatureSegment.forEachDab(draw: (x: Float, y: Float, width: Float) -> Unit) {
    // Imported finite coordinates can still span billions of units. Preserve normal
    // sampling while bounding UI-thread work independently of untrusted geometry.
    val steps = ceil(curve.length().toDouble()).toInt().coerceIn(0, 16_384)
    for (index in 0 until steps) {
        val t = index.toFloat() / steps
        val tt = t * t
        val ttt = tt * t
        val u = 1 - t
        val uu = u * u
        val uuu = uu * u
        var x = uuu * curve.start.x
        x += 3 * uu * t * curve.control1.x
        x += 3 * u * tt * curve.control2.x
        x += ttt * curve.end.x
        var y = uuu * curve.start.y
        y += 3 * uu * t * curve.control1.y
        y += 3 * u * tt * curve.control2.y
        y += ttt * curve.end.y
        draw(x, y, startWidth + ttt * (endWidth - startWidth))
    }
}
