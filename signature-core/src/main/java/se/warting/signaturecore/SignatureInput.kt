package se.warting.signaturecore

/**
 * Live input for a [SignatureSDK]. UI adapters supply coordinates in pixels and timestamps
 * in milliseconds. Only the first pointer signs; secondary pointers never become strokes.
 * Persisted [Event]s remain separate from the transient pointer/shadow state.
 */
class SignatureInput internal constructor(sdk: SignatureSDK) {
    private val delegate = se.warting.signatureengine.SignatureInput { event ->
        sdk.addEvent(Event(event.timestamp, event.action, event.x, event.y))
    }

    val activePointerId: Long? get() = delegate.activePointerId
    val x: Float get() = delegate.x
    val y: Float get() = delegate.y
    val pressure: Float get() = delegate.pressure

    fun isActive(pointerId: Long): Boolean = delegate.isActive(pointerId)

    /** Start a new gesture. The adapter calls this only for the gesture's first down. */
    fun start(pointerId: Long, x: Float, y: Float, pressure: Float, timestamp: Long) =
        delegate.start(pointerId, x, y, pressure, timestamp)

    /** Returns false for secondary pointers or input invalidated by clear/restore/cancel. */
    fun move(pointerId: Long, x: Float, y: Float, pressure: Float, timestamp: Long): Boolean =
        delegate.move(pointerId, x, y, pressure, timestamp)

    /** End the signing pointer's stroke, even when other fingers remain on the surface. */
    fun end(pointerId: Long, x: Float, y: Float, timestamp: Long): Boolean =
        delegate.end(pointerId, x, y, timestamp)

    /** Stop accepting this gesture without synthesizing an UP or discarding existing ink. */
    fun cancel() = delegate.cancel()
}
