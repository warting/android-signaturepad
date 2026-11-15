package se.warting.signatureengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignatureDocumentTest {
    @Test
    fun hugeRestoredCoordinatesHaveBoundedRasterWork() {
        val document = SignatureDocument()
        document.restore(listOf(
            SignatureEvent(0, 0, 0f, 0f),
            SignatureEvent(16, 2, 100_000_000f, 0f),
            SignatureEvent(32, 2, 200_000_000f, 0f),
            SignatureEvent(48, 1, 300_000_000f, 0f),
        ))
        assertTrue(document.segments.isNotEmpty())
        var total = 0
        document.segments.forEach { segment ->
            var calls = 0
            segment.forEachDab { x, y, _ ->
                calls++
                assertTrue(x.isFinite() && y.isFinite())
            }
            assertTrue(calls in 0..16_384)
            total += calls
        }
        assertTrue(total > 0)
    }

    private fun stroke(x: Float) = listOf(
        SignatureEvent(0, 0, x, 10f),
        SignatureEvent(16, 2, x + 10, 20f),
        SignatureEvent(32, 2, x + 20, 10f),
        SignatureEvent(48, 1, x + 30, 20f),
    )

    @Test
    fun undoRedoAndRestorePreserveEventsCurvesAndSvg() {
        val document = SignatureDocument()
        val events = stroke(10f) + stroke(60f)
        document.restore(events)
        val curves = document.segments
        val svg = document.toSvg(120, 80)
        document.undo()
        assertEquals(stroke(10f), document.events)
        assertTrue(document.canRedo)
        document.redo()
        assertEquals(events, document.events)
        assertEquals(curves, document.segments)
        assertEquals(svg, document.toSvg(120, 80))
        document.clear()
        assertTrue(document.isEmpty)
        assertFalse(document.canRedo)
        document.restore(events)
        assertEquals(svg, document.toSvg(120, 80))
    }

    @Test
    fun invalidMoveDoesNotDiscardRedoButNewStrokeDoes() {
        val document = SignatureDocument()
        document.restore(stroke(10f))
        document.undo()
        document.add(SignatureEvent(100, 2, Float.NaN, 0f))
        assertTrue(document.canRedo)
        document.input.start(1, 50f, 20f, 1f, 120)
        assertFalse(document.canRedo)
        document.clear()
        assertFalse(document.input.move(1, 70f, 30f, 1f, 140))
        assertTrue(document.events.isEmpty())
    }

    @Test
    fun undoDuringGestureDoesNotLoseCurrentPointer() {
        val document = SignatureDocument()
        document.restore(stroke(10f))
        document.input.start(42, 70f, 20f, 1f, 100)
        document.undo()
        assertTrue(document.input.isActive(42))
        assertEquals(1, document.events.size)
        document.redo()
        assertTrue(document.input.end(42, 90f, 40f, 120))
        assertEquals(6, document.events.size)
        assertTrue(document.canUndo)
    }
}
