package se.warting.signatureengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StrokeBuilderTest {
    private data class Segment(val curve: CubicCurve, val startWidth: Float, val endWidth: Float)

    @Test
    fun resetAndReplayReproduceGeometryAndWidths() {
        val segments = mutableListOf<Segment>()
        val builder = StrokeBuilder { c, s, e -> segments.add(Segment(c, s, e)) }
        builder.configure(3, 7, 0.9f)
        fun draw() {
            builder.beginStroke()
            builder.add(0f, 0f, 0)
            builder.add(0f, 0f, 0)
            builder.add(10f, 20f, 16)
            builder.add(30f, 40f, 32)
            builder.add(50f, 20f, 48)
        }
        draw()
        assertFalse(builder.isEmpty)
        val original = segments.toList()
        assertEquals(3, original.size)
        assertTrue(original.all { it.endWidth in 3f..7f })
        assertEquals(CurvePoint(0f, 0f), original.first().curve.start)
        assertEquals(CurvePoint(30f, 40f), original.last().curve.end)
        builder.reset()
        assertTrue(builder.isEmpty)
        segments.clear()
        draw()
        assertEquals(original, segments)
    }

    @Test
    fun duplicateCoordinatesAndTimestampsProduceFiniteCurves() {
        val segments = mutableListOf<Segment>()
        val builder = StrokeBuilder { c, s, e -> segments.add(Segment(c, s, e)) }
        builder.configure(3, 7, 0.9f)
        repeat(6) { builder.add(10f, 10f, 0) }
        assertTrue(segments.isNotEmpty())
        assertTrue(segments.all { it.curve.length() < 0.0001f && it.endWidth == 7f })
    }
}
