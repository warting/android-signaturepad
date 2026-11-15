package se.warting.signatureink

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import java.io.ByteArrayOutputStream

/**
 * Main-thread state for the optional Ink renderer. Retains Ink's pressure/stylus metadata.
 * Export after [isDrawing] becomes false so the asynchronous stroke handoff is complete.
 */
class InkSignatureState {
    private val completed = mutableStateListOf<Stroke>()
    private val undone = mutableStateListOf<Stroke>()
    private val pending = mutableSetOf<InProgressStrokeId>()
    private var pendingCount by mutableIntStateOf(0)
    internal var generation = 0
        private set
    internal val observers = mutableSetOf<() -> Unit>()
    val strokes: List<Stroke> get() = completed.toList()
    val isDrawing: Boolean get() = pendingCount > 0
    val isEmpty: Boolean get() = completed.isEmpty() && !isDrawing
    val canUndo: Boolean get() = completed.isNotEmpty() && !isDrawing
    val canRedo: Boolean get() = undone.isNotEmpty() && !isDrawing

    fun clear() = restore(emptyList())

    fun restore(strokes: List<Stroke>) {
        val snapshot = strokes.toList()
        generation++
        pending.clear()
        pendingCount = 0
        completed.clear()
        completed.addAll(snapshot)
        undone.clear()
        notifyChanged()
    }

    fun undo() {
        if (!canUndo) return
        undone.add(completed.removeAt(completed.lastIndex))
        notifyChanged()
    }

    fun redo() {
        if (!canRedo) return
        completed.add(undone.removeAt(undone.lastIndex))
        notifyChanged()
    }

    /** Returns a caller-owned bitmap of completed strokes, in view pixel coordinates. */
    fun toBitmap(width: Int, height: Int, backgroundColor: Int = Color.TRANSPARENT): Bitmap {
        require(width > 0 && height > 0)
        check(!isDrawing) { "Wait for the Ink stroke handoff before exporting" }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(backgroundColor)
        val canvas = Canvas(bitmap)
        val renderer = CanvasStrokeRenderer.create()
        completed.forEach { renderer.draw(canvas, it, Matrix()) }
        return bitmap
    }

    fun toPngByteArray(width: Int, height: Int, backgroundColor: Int = Color.TRANSPARENT): ByteArray {
        val bitmap = toBitmap(width, height, backgroundColor)
        return try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    internal fun begin(id: InProgressStrokeId) {
        pending.add(id)
        pendingCount = pending.size
        notifyChanged()
    }

    internal fun cancel(id: InProgressStrokeId) {
        pending.remove(id)
        pendingCount = pending.size
        notifyChanged()
    }

    internal fun finish(strokes: Map<InProgressStrokeId, Stroke>) {
        strokes.forEach { (id, stroke) ->
            if (pending.remove(id)) {
                undone.clear()
                completed.add(stroke)
            }
        }
        pendingCount = pending.size
        notifyChanged()
    }

    private fun notifyChanged() { observers.toList().forEach { it() } }
}

// PNG is lossless; Android ignores this quality parameter.
private const val PNG_QUALITY = 100
