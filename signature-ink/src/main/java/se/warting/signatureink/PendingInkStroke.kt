package se.warting.signatureink

import android.view.MotionEvent
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput

/** Real samples retained only until the asynchronous renderer hands a stroke to state. */
internal class PendingInkStroke(private val brush: Brush, private val pointerId: Int) {
    private val events = mutableListOf<MotionEvent>()
    var ended = false

    fun add(event: MotionEvent) { events.add(MotionEvent.obtain(event)) }

    fun toStroke(): Stroke {
        val inputs = MutableStrokeInputBatch()
        val start = events.first().eventTime
        events.forEach { event ->
            val pointer = event.findPointerIndex(pointerId)
            if (pointer < 0) return@forEach
            val tool = event.getToolType(pointer)
            val stylus = tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER
            val first = if (event.actionMasked == MotionEvent.ACTION_MOVE) 0 else event.historySize
            for (history in first..event.historySize) {
                val current = history == event.historySize
                fun axis(axis: Int): Float = if (current) event.getAxisValue(axis, pointer)
                    else event.getHistoricalAxisValue(axis, pointer, history)
                val time = if (current) event.eventTime else event.getHistoricalEventTime(history)
                val input = StrokeInput()
                input.update(
                    x = axis(MotionEvent.AXIS_X), y = axis(MotionEvent.AXIS_Y),
                    elapsedTimeMillis = time - start,
                    toolType = when (tool) {
                        MotionEvent.TOOL_TYPE_MOUSE -> InputToolType.MOUSE
                        MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_ERASER -> InputToolType.STYLUS
                        else -> InputToolType.TOUCH
                    },
                    pressure = if (stylus) axis(MotionEvent.AXIS_PRESSURE).coerceIn(0f, 1f)
                        else StrokeInput.NO_PRESSURE,
                    tiltRadians = if (stylus) axis(MotionEvent.AXIS_TILT).coerceIn(0f, Math.PI.toFloat() / 2)
                        else StrokeInput.NO_TILT,
                    orientationRadians = if (stylus)
                        (axis(MotionEvent.AXIS_ORIENTATION) + 2.5f * Math.PI.toFloat()).mod(2 * Math.PI.toFloat())
                        else StrokeInput.NO_ORIENTATION,
                )
                // Ink itself ignores invalid or out-of-order historical samples.
                try { inputs.add(input) } catch (_: IllegalArgumentException) { /* Ignore rejected sample. */ }
            }
        }
        return Stroke(brush, inputs)
    }

    fun recycle() { events.forEach(MotionEvent::recycle); events.clear() }
}
