package se.warting.signaturecore.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SvgBuilderTest {

    @Test
    fun build_withDefaults_emitsBlackStrokeAndNoBackground() {
        val svg = SvgBuilder().build(width = 100, height = 50)

        assertTrue("Expected black fallback stroke", svg.contains("stroke=\"black\""))
        assertFalse("Expected no background rect by default", svg.contains("<rect"))
        assertFalse("Expected no opacity attrs", svg.contains("stroke-opacity"))
    }

    @Test
    fun build_withOpaquePenColor_emitsHexStroke() {
        // Opaque red (ARGB 0xFFFF0000)
        val svg = SvgBuilder().build(
            width = 10,
            height = 10,
            penColor = 0xFFFF0000.toInt(),
            backgroundColor = null,
        )

        assertTrue(svg.contains("stroke=\"#FF0000\""))
        assertFalse("Opaque colors should not include stroke-opacity", svg.contains("stroke-opacity"))
    }

    @Test
    fun build_withTranslucentPenColor_emitsStrokeOpacity() {
        // 50% green (alpha 0x80)
        val svg = SvgBuilder().build(
            width = 10,
            height = 10,
            penColor = 0x8000FF00.toInt(),
            backgroundColor = null,
        )

        assertTrue(svg.contains("stroke=\"#00FF00\""))
        assertTrue("Translucent colors should include stroke-opacity", svg.contains("stroke-opacity=\""))
    }

    @Test
    fun build_withBackgroundColor_emitsRectBeforePaths() {
        val svg = SvgBuilder().build(
            width = 20,
            height = 20,
            penColor = null,
            backgroundColor = 0xFFFFFFFF.toInt(),
        )

        val rectIndex = svg.indexOf("<rect")
        val groupIndex = svg.indexOf("<g ")
        assertTrue("Expected <rect> before <g>", rectIndex in 0 until groupIndex)
        assertTrue(svg.contains("fill=\"#FFFFFF\""))
        assertTrue(svg.contains("width=\"100%\""))
        assertTrue(svg.contains("height=\"100%\""))
    }

    @Test
    fun build_withTranslucentBackground_emitsFillOpacity() {
        val svg = SvgBuilder().build(
            width = 1,
            height = 1,
            penColor = null,
            backgroundColor = 0x40123456,
        )

        assertTrue(svg.contains("fill=\"#123456\""))
        assertTrue(svg.contains("fill-opacity=\""))
    }

    @Test
    fun build_isStableForSameInput() {
        val first = SvgBuilder().build(50, 25, penColor = 0xFF112233.toInt(), backgroundColor = null)
        val second = SvgBuilder().build(50, 25, penColor = 0xFF112233.toInt(), backgroundColor = null)
        assertEquals(first, second)
    }

    @Test
    fun append_skipsCurveWithNonFiniteCoordinates() {
        val builder = SvgBuilder()

        // A NaN coordinate anywhere in the curve must not crash the builder.
        // Regression test for issue #336 — previously SvgPoint.<init> threw
        // IllegalArgumentException("Cannot round NaN value") when downstream
        // arithmetic produced NaN/Inf for a control point.
        builder.append(makeBezier(0f, 0f, Float.NaN, 0f, 10f, 5f, 15f, 10f), strokeWidth = 4f)
        builder.append(makeBezier(0f, 0f, 5f, 0f, 10f, 5f, 15f, Float.POSITIVE_INFINITY), strokeWidth = 4f)
        builder.append(makeBezier(0f, 0f, 5f, 0f, 10f, 5f, 15f, 10f), strokeWidth = Float.NaN)

        val svg = builder.build(width = 50, height = 50, penColor = null, backgroundColor = null)
        assertFalse("Skipped curves must not emit a <path>", svg.contains("<path "))
    }

    @Test
    fun append_emitsPathWhenCurveIsValidAfterSkippedOne() {
        val builder = SvgBuilder().apply {
            append(makeBezier(Float.NaN, 0f, 5f, 0f, 10f, 5f, 15f, 10f), strokeWidth = 4f)
            append(makeBezier(0f, 0f, 5f, 0f, 10f, 5f, 15f, 10f), strokeWidth = 4f)
        }

        val svg = builder.build(width = 50, height = 50, penColor = null, backgroundColor = null)
        assertTrue("Valid curve should still produce a <path>", svg.contains("<path "))
    }

    @Test
    fun build_isIdempotentWithAppendedCurves() {
        val builder = SvgBuilder().apply {
            append(makeBezier(0f, 0f, 5f, 0f, 10f, 5f, 15f, 10f), strokeWidth = 4f)
            append(makeBezier(15f, 10f, 20f, 15f, 25f, 20f, 30f, 25f), strokeWidth = 4f)
        }

        val firstSvg = builder.build(width = 50, height = 50, penColor = null, backgroundColor = null)
        val secondSvg = builder.build(width = 50, height = 50, penColor = null, backgroundColor = null)

        assertTrue("Expected at least one <path> in SVG output", firstSvg.contains("<path "))
        assertEquals("Repeated build() must produce identical output", firstSvg, secondSvg)
    }

    private fun makeBezier(
        sx: Float,
        sy: Float,
        c1x: Float,
        c1y: Float,
        c2x: Float,
        c2y: Float,
        ex: Float,
        ey: Float,
    ): Bezier = Bezier(
        TimedPoint().set(sx, sy, 0L),
        TimedPoint().set(c1x, c1y, 0L),
        TimedPoint().set(c2x, c2y, 0L),
        TimedPoint().set(ex, ey, 0L),
    )
}
