package se.warting.signatureengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignatureInputTest {
    @Test
    fun secondaryPointersNeverProduceInk() {
        val events = mutableListOf<SignatureEvent>()
        val input = SignatureInput(events::add)
        input.start(10, 1f, 2f, 0.4f, 0)
        input.start(20, 8f, 9f, 1f, 1)
        assertFalse(input.move(20, 9f, 10f, 1f, 2))
        assertFalse(input.end(20, 9f, 10f, 3))
        assertEquals(0.4f, input.pressure)
        assertTrue(input.move(10, 3f, 4f, 0.8f, 4))
        assertTrue(input.end(10, 5f, 6f, 5))
        assertEquals(listOf(0, 2, 1), events.map { it.action })
        assertEquals(listOf(0L, 4L, 5L), events.map { it.timestamp })
        assertNull(input.activePointerId)
        assertEquals(0f, input.pressure)
    }

    @Test
    fun cancellationInvalidatesGestureWithoutInventingAnUp() {
        val events = mutableListOf<SignatureEvent>()
        val input = SignatureInput(events::add)
        input.start(1, 1f, 2f, Float.NaN, 0)
        assertEquals(1f, input.pressure)
        input.cancel()
        assertFalse(input.move(1, 5f, 5f, 1f, 1))
        assertFalse(input.end(1, 5f, 5f, 2))
        assertEquals(1, events.size)
        input.start(2, 3f, 4f, -1f, 3)
        assertTrue(input.isActive(2))
        assertEquals(1f, input.pressure)
        assertEquals(2, events.size)
    }
}
