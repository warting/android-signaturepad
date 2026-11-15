package se.warting.signaturecore

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureRedoTest {
    @Test
    fun multipleUndosAndRedos_restoreOriginalOrder() {
        val sdk = SignatureSDK()
        stroke(sdk, 0L)
        stroke(sdk, 100L)
        stroke(sdk, 200L)
        val original = sdk.getEvents()
        repeat(3) { sdk.undo() }
        assertTrue(sdk.isEmpty)
        assertTrue(sdk.canRedo())
        repeat(3) { sdk.redo() }
        assertEquals(original, sdk.getEvents())
        assertFalse(sdk.canRedo())
        sdk.redo()
        assertEquals(original, sdk.getEvents())
    }

    @Test
    fun redoDuringInput_restoresCompletedStrokeBeforeLiveStroke() {
        val sdk = SignatureSDK()
        stroke(sdk, 0L)
        sdk.input.start(7L, 30f, 40f, 0.5f, 100L)
        val original = sdk.getEvents()
        sdk.undo()
        sdk.redo()
        assertEquals(original, sdk.getEvents())
        assertTrue(sdk.input.isActive(7L))
        assertTrue(sdk.input.end(7L, 50f, 60f, 120L))
        assertEquals(MotionEvent.ACTION_UP, sdk.getEvents().last().action)
    }

    @Test
    fun newInputClearAndRestore_discardRedoHistory() {
        for (action in listOf<(SignatureSDK) -> Unit>(
            { stroke(it, 100L) },
            { it.clear() },
            { it.restoreEvents(emptyList()) },
            { it.restoreSignature(emptyList(), 0, 0) },
        )) {
            val sdk = SignatureSDK()
            stroke(sdk, 0L)
            sdk.undo()
            assertTrue(sdk.canRedo())
            action(sdk)
            val events = sdk.getEvents()
            assertFalse(sdk.canRedo())
            sdk.redo()
            assertEquals(events, sdk.getEvents())
        }
    }

    private fun stroke(sdk: SignatureSDK, time: Long) {
        sdk.input.start(1L, 10f, 20f, 1f, time)
        sdk.input.end(1L, 20f, 30f, time + 16L)
    }
}
