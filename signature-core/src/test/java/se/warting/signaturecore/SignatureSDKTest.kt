package se.warting.signaturecore

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureSDKTest {

    @Test
    fun undo_preservesInProgressStroke() {
        val first = stroke(0L)
        val second = stroke(100L)
        val inProgress = stroke(200L).dropLast(1)
        val sdk = SignatureSDK().apply { restoreEvents(first + second + inProgress) }

        sdk.undo()

        assertEquals(first + inProgress, sdk.getEvents())
        assertTrue(sdk.canUndo())
        assertFalse(sdk.isEmpty)
    }

    @Test
    fun undo_canFinishPreservedStroke() {
        val completed = stroke(0L)
        val active = stroke(100L)
        val sdk = SignatureSDK().apply { restoreEvents(completed + active.dropLast(1)) }

        sdk.undo()
        assertFalse(sdk.canUndo())
        sdk.addEvent(active.last())

        assertEquals(active, sdk.getEvents())
        assertTrue(sdk.canUndo())
        sdk.undo()
        assertTrue(sdk.isEmpty)
    }

    @Test
    fun undo_withoutCompletedStroke_isNoOp() {
        val inProgress = stroke(0L).dropLast(1)
        val sdk = SignatureSDK().apply { restoreEvents(inProgress) }

        sdk.undo()

        assertEquals(inProgress, sdk.getEvents())
        assertFalse(sdk.canUndo())
    }

    @Test
    fun undo_removesOnlyLastCompletedStroke() {
        val first = stroke(0L)
        val sdk = SignatureSDK().apply { restoreEvents(first + stroke(100L)) }

        sdk.undo()

        assertEquals(first, sdk.getEvents())
        sdk.undo()
        assertTrue(sdk.isEmpty)
        assertFalse(sdk.canUndo())
    }

    private fun stroke(start: Long) = listOf(
        Event(start, MotionEvent.ACTION_DOWN, 10f, 10f),
        Event(start + 16, MotionEvent.ACTION_MOVE, 20f, 20f),
        Event(start + 32, MotionEvent.ACTION_UP, 30f, 30f),
    )
}
