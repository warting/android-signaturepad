package se.warting.signaturecore.utils

import kotlin.math.pow
import kotlin.math.sqrt

class TimedPoint {
    var x = 0f
    var y = 0f
    var timestamp: Long = 0

    operator fun set(x: Float, y: Float, timestamp: Long): TimedPoint {
        this.x = x
        this.y = y
        this.timestamp = timestamp
        return this
    }

    fun velocityFrom(start: TimedPoint): Float {
        var diff = timestamp - start.timestamp
        if (diff <= 0) {
            diff = 1
        }
        var velocity = distanceTo(start) / diff
        if (velocity.isInfinite() || velocity.isNaN()) {
            velocity = 0f
        }
        return velocity
    }

    private fun distanceTo(point: TimedPoint): Float {
        return sqrt(
            (point.x - x).toDouble().pow(2.0) + (point.y - y).toDouble().pow(2.0)
        ).toFloat()
    }
}
