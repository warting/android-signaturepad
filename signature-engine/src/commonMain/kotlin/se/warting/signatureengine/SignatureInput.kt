package se.warting.signatureengine

/** Portable input event. Action values retain Android's persisted DOWN/UP/MOVE encoding. */
data class SignatureEvent(val timestamp: Long, val action: Int, val x: Float, val y: Float) {
    companion object {
        const val DOWN = 0
        const val UP = 1
        const val MOVE = 2
    }
}

/** Single-pointer gesture state, shared by every rendering backend. */
class SignatureInput(private val emit: (SignatureEvent) -> Unit) {
    var activePointerId: Long? = null
        private set
    var x: Float = 0f
        private set
    var y: Float = 0f
        private set
    var pressure: Float = 0f
        private set

    fun isActive(pointerId: Long): Boolean = activePointerId == pointerId

    fun start(pointerId: Long, x: Float, y: Float, pressure: Float, timestamp: Long) {
        if (activePointerId != null) return
        activePointerId = pointerId
        updatePointer(x, y, pressure)
        emit(SignatureEvent(timestamp, SignatureEvent.DOWN, x, y))
    }

    fun move(pointerId: Long, x: Float, y: Float, pressure: Float, timestamp: Long): Boolean {
        if (!isActive(pointerId)) return false
        updatePointer(x, y, pressure)
        emit(SignatureEvent(timestamp, SignatureEvent.MOVE, x, y))
        return true
    }

    fun end(pointerId: Long, x: Float, y: Float, timestamp: Long): Boolean {
        if (!isActive(pointerId)) return false
        cancel()
        emit(SignatureEvent(timestamp, SignatureEvent.UP, x, y))
        return true
    }

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
