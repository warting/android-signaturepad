package se.warting.signaturecore

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureInputTest {
    @Test
    fun secondaryPointers_neverModifySignatureOrFinishTheSigningPointer() {
        val sdk = SignatureSDK()
        val input = sdk.input
        input.start(7L, 10f, 20f, 0.5f, 100L)
        input.start(9L, 80f, 90f, 1f, 105L)
        assertFalse(input.move(9L, 80f, 90f, 1f, 110L))
        assertFalse(input.end(9L, 80f, 90f, 120L))
        assertTrue(input.isActive(7L))
        assertEquals(0.5f, input.pressure)
        input.move(7L, 15f, 25f, 0.7f, 130L)
        assertTrue(input.end(7L, 20f, 30f, 140L))
        assertFalse(input.move(9L, 90f, 100f, 1f, 150L))
        assertEquals(
            listOf(
                Event(100L, MotionEvent.ACTION_DOWN, 10f, 20f),
                Event(130L, MotionEvent.ACTION_MOVE, 15f, 25f),
                Event(140L, MotionEvent.ACTION_UP, 20f, 30f),
            ),
            sdk.getEvents(),
        )
        assertEquals(0f, input.pressure)
    }

    @Test
    fun clear_invalidatesInputUntilANewDown() {
        val sdk = SignatureSDK()
        sdk.input.start(1L, 10f, 20f, 1f, 100L)
        sdk.clear()
        assertFalse(sdk.input.move(1L, 30f, 40f, 1f, 110L))
        assertFalse(sdk.input.end(1L, 30f, 40f, 120L))
        assertTrue(sdk.getEvents().isEmpty())
        sdk.input.start(1L, 50f, 60f, 1f, 130L)
        assertTrue(sdk.input.end(1L, 70f, 80f, 140L))
        assertEquals(2, sdk.getEvents().size)
    }

    @Test
    fun restoringEvents_invalidatesInputWithoutAppendingAnUp() {
        val sdk = SignatureSDK()
        sdk.input.start(1L, 10f, 20f, 1f, 100L)
        val saved = listOf(Event(0L, MotionEvent.ACTION_DOWN, 1f, 2f), Event(10L, MotionEvent.ACTION_UP, 3f, 4f))
        sdk.restoreEvents(saved)
        assertFalse(sdk.input.move(1L, 30f, 40f, 1f, 110L))
        assertFalse(sdk.input.end(1L, 30f, 40f, 120L))
        assertEquals(saved, sdk.getEvents())
        assertEquals(0f, sdk.input.pressure)
    }

    @Test
    fun cancel_preservesExistingInkAndDoesNotEmitAnUp() {
        val sdk = SignatureSDK()
        sdk.input.start(1L, 10f, 20f, 1f, 100L)
        sdk.input.move(1L, 30f, 40f, 1f, 110L)
        val saved = sdk.getEvents()
        sdk.input.cancel()
        assertFalse(sdk.input.end(1L, 30f, 40f, 120L))
        assertEquals(saved, sdk.getEvents())
        assertEquals(0f, sdk.input.pressure)
        sdk.input.start(2L, 10f, 20f, 1f, 130L)
        assertTrue(sdk.input.isActive(2L))
    }

    @Test
    fun undo_keepsTheActivePointerAndItsStroke() {
        val sdk = SignatureSDK()
        sdk.input.start(1L, 10f, 20f, 1f, 0L)
        sdk.input.end(1L, 30f, 40f, 10L)
        sdk.input.start(2L, 50f, 60f, 1f, 20L)
        sdk.undo()
        assertTrue(sdk.input.isActive(2L))
        assertTrue(sdk.input.move(2L, 70f, 80f, 1f, 30L))
        assertTrue(sdk.input.end(2L, 90f, 100f, 40L))
        assertEquals(listOf(20L, 30L, 40L), sdk.getEvents().map { it.timestamp })
        assertTrue(sdk.canUndo())
    }

    @Test
    fun activePressureFallback_isSharedAcrossInputAdapters() {
        val sdk = SignatureSDK()
        for (pressure in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            sdk.input.start(1L, 10f, 20f, pressure, 0L)
            assertEquals(1f, sdk.input.pressure)
            sdk.input.cancel()
            assertEquals(0f, sdk.input.pressure)
        }
    }
}
