package se.warting.signaturecore

import android.view.MotionEvent

/**
 * Live input for a [SignatureSDK]. UI adapters supply coordinates in pixels and timestamps
 * in milliseconds. Only the first pointer signs; secondary pointers never become strokes.
 * Persisted [Event]s remain separate from the transient pointer/shadow state.
 */
class SignatureInput internal constructor(private val sdk: SignatureSDK) {
    var activePointerId: Long? = null
        private set
    var x: Float = 0f
        private set
    var y: Float = 0f
        private set
    var pressure: Float = 0f
        private set

    fun isActive(pointerId: Long): Boolean = activePointerId == pointerId

    /** Start a new gesture. The adapter calls this only for the gesture's first down. */
    fun start(pointerId: Long, x: Float, y: Float, pressure: Float, timestamp: Long) {
        if (activePointerId != null) return
        activePointerId = pointerId
        updatePointer(x, y, pressure)
        sdk.addEvent(Event(timestamp, MotionEvent.ACTION_DOWN, x, y))
    }

    /** Returns false for secondary pointers or input invalidated by clear/restore/cancel. */
    fun move(pointerId: Long, x: Float, y: Float, pressure: Float, timestamp: Long): Boolean {
        if (!isActive(pointerId)) return false
        updatePointer(x, y, pressure)
        sdk.addEvent(Event(timestamp, MotionEvent.ACTION_MOVE, x, y))
        return true
    }

    /** End the signing pointer's stroke, even when other fingers remain on the surface. */
    fun end(pointerId: Long, x: Float, y: Float, timestamp: Long): Boolean {
        if (!isActive(pointerId)) return false
        cancel()
        sdk.addEvent(Event(timestamp, MotionEvent.ACTION_UP, x, y))
        return true
    }

    /** Stop accepting this gesture without synthesizing an UP or discarding existing ink. */
    fun cancel() {
        activePointerId = null
        pressure = 0f
    }

    private fun updatePointer(x: Float, y: Float, pressure: Float) {
        this.x = x
        this.y = y
        this.pressure = if (pressure.isFinite() && pressure > 0f) pressure else 1f
    }
}
