package se.warting.signaturecompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import se.warting.signatureengine.SignatureDocument
import se.warting.signatureengine.SignatureEvent
import se.warting.signatureengine.SignatureSegment
import se.warting.signatureengine.forEachDab

/** Observable portable signature state. Coordinates and pen widths use density-independent units. */
class SignaturePadState(minWidth: Int = 3, maxWidth: Int = 7, velocityFilterWeight: Float = 0.9f) {
    private var revision by mutableIntStateOf(0)
    internal val document = SignatureDocument(minWidth, maxWidth, velocityFilterWeight) { revision++ }
    val isEmpty: Boolean get() { revision; return document.isEmpty }
    val canUndo: Boolean get() { revision; return document.canUndo }
    val canRedo: Boolean get() { revision; return document.canRedo }
    val events: List<SignatureEvent> get() { revision; return document.events }
    internal val segments: List<SignatureSegment> get() { revision; return document.segments }
    fun clear() = document.clear()
    fun undo() = document.undo()
    fun redo() = document.redo()
    fun restore(events: List<SignatureEvent>) = document.restore(events)
    fun toSvg(width: Int, height: Int, penColor: Int? = null, backgroundColor: Int? = null): String =
        document.toSvg(width, height, penColor, backgroundColor)
}

@Composable
fun rememberSignaturePadState(): SignaturePadState = remember { SignaturePadState() }

/** Native Compose canvas on Android, iOS, desktop and WebAssembly; no embedded Android View. */
@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    state: SignaturePadState = rememberSignaturePadState(),
    penColor: Color = Color.Black,
) {
    val density = LocalDensity.current.density
    Canvas(modifier.size(320.dp, 160.dp).clipToBounds().pointerInput(state, density) {
        val input = state.document.input
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            input.start(down.id.value, down.position.x / density, down.position.y / density,
                down.pressure, down.uptimeMillis)
            down.consume()
            try {
                while (input.isActive(down.id.value)) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    if (change.isConsumed) break
                    if (!change.pressed) {
                        input.end(down.id.value, change.position.x / density, change.position.y / density,
                            change.uptimeMillis)
                        change.consume()
                        break
                    }
                    input.move(down.id.value, change.position.x / density, change.position.y / density,
                        change.pressure, change.uptimeMillis)
                    change.consume()
                }
            } finally {
                input.cancel()
            }
        }
    }) {
        state.segments.forEach { segment ->
            segment.forEachDab { x, y, width ->
                drawCircle(penColor, width * density / 2, Offset(x * density, y * density))
            }
        }
    }
}
