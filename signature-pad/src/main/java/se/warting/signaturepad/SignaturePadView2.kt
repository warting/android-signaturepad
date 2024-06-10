package se.warting.signaturepad

import android.os.Parcelable
import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.parcelize.Parcelize
import se.warting.signaturecore.DrawEvent
import se.warting.signaturecore.Event
import kotlin.math.roundToInt

@Preview
@Composable
fun SignaturePadView2Preview() {
    val initialEvents: List<Event> = listOf(
        Event(timestamp = 1718546886266, action = 0, x = 83.958984f, y = 207.9419f),
        Event(timestamp = 1718546886321, action = 2, x = 90.53905f, y = 207.9419f),
        Event(timestamp = 1718546886337, action = 2, x = 123.69079f, y = 197.7146f),
        Event(timestamp = 1718546886354, action = 2, x = 175.25813f, y = 184.66736f),
        Event(timestamp = 1718546886371, action = 2, x = 213.87732f, y = 180.93262f),
        Event(timestamp = 1718546886387, action = 2, x = 259.1513f, y = 180.93262f),
        Event(timestamp = 1718546886404, action = 2, x = 298.65097f, y = 180.93262f),
        Event(timestamp = 1718546886413, action = 2, x = 302.95312f, y = 180.93262f),
        Event(timestamp = 1718546886413, action = 1, x = 302.95312f, y = 180.93262f),
    )

    SignaturePadView2()
}

/**
 * SignatureLine is a data class that represents a line in a signature.
 * @param start The starting point of the line.
 * @param end The ending point of the line.
 */
@Immutable
public data class SignatureLine(
    val start: Offset,
    val end: Offset,
)

@SuppressWarnings("LongParameterList")
@Composable
fun SignaturePadView2(
    modifier: Modifier = Modifier,
    penMinWidth: Dp = 3.dp,
    penMaxWidth: Dp = 7.dp,
    penColor: Color = Color.Black,
    velocityFilterWeight: Float = 0.9F,
    clearOnDoubleClick: Boolean = false,
    onReady: (svg: SignaturePadAdapter) -> Unit = {},
    onStartSigning: () -> Unit = {},
    onSigning: () -> Unit = {},
    onSigned: () -> Unit = {},
    onClear: () -> Unit = {},
    signatureColor: Color = Color.Black,
    signatureThickness: Dp = 5.dp,
) {

    val resources = LocalContext.current.resources

    fun convertDpToPx(dp: Float): Int {
        return (resources.displayMetrics.density * dp).roundToInt()
    }

    val minWithPx = convertDpToPx(penMinWidth.value)
    val maxWithPx = convertDpToPx(penMaxWidth.value)
    val state by rememberSaveable(stateSaver = SignatureStateSaver) {
        mutableStateOf(
            SignatureState(
                minWithPx,
                maxWithPx,
                velocityFilterWeight
            )
        )
    }
    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                val size: IntSize = coordinates.size
                state.setBitmapSize(size.width, size.height)
            }
            .background(Color.Red)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    state.addEvent(down.position.x, down.position.y, MotionEvent.ACTION_DOWN)
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        // consume all changes
                        event.changes.forEach {
                            if (!it.pressed) {
                                pressed = false
                                state.addEvent(it.position.x, it.position.y, MotionEvent.ACTION_UP)
                            } else {
                                state.addEvent(
                                    it.position.x,
                                    it.position.y,
                                    MotionEvent.ACTION_MOVE
                                )
                            }
                            it.consume()
                        }
                    }
                }
            }
            .fillMaxSize()
    ) {
        Image(
            bitmap = state.b,
            contentDescription = "Signature",
            modifier = Modifier.fillMaxSize(),
        )
    }
}

val SignatureStateSaver = run {
    val savableKey = "savable"
    mapSaver(
        save = {
            mapOf(
                savableKey to SaveThis(
                    it.min,
                    it.max,
                    it.velocityFilterWeight,
                    it._signatureEvents
                )
            )
        },
        restore = {
            val saved = it[savableKey] as SaveThis
            SignatureState(
                saved.min,
                saved.max,
                saved.velocityFilterWeight
            ).also { state ->
                saved.drawEvents.forEach {
                    state.addEvent(it.x, it.y, it.action)
                }
            }
        }
    )
}

@Parcelize
data class SaveThis(
    val min: Int,
    val max: Int,
    val velocityFilterWeight: Float,
    val drawEvents: List<Event>,
) : Parcelable

@Stable
class SignatureState(
    val min: Int,
    val max: Int,
    val velocityFilterWeight: Float
) {

    val bezierBitmapThing =
        BezierBitmapThing(min, max, velocityFilterWeight)

    val bitmap = bezierBitmapThing.bmp
    private val _drawEvents = mutableStateListOf<DrawEvent>()
    val _signatureEvents = mutableStateListOf<Event>()
    val drawEvents: List<DrawEvent> get() = _drawEvents.toList()
    val b: ImageBitmap get() = bitmap.value


    fun addEvent(x: Float, y: Float, action: Int) {
        val event = Event(System.currentTimeMillis(), action, x, y)
        _signatureEvents.add(event)
        bezierBitmapThing.current(event)
        val drawThis: MutableList<DrawEvent> = bezierBitmapThing.drawThis
        _drawEvents.clear()
        _drawEvents.addAll(drawThis)
    }

    private val _signature = mutableStateOf<ImageBitmap?>(null)
    val signature: ImageBitmap? get() = _signature.value

    fun updateSignature(bitmap: ImageBitmap) {
        _signature.value = bitmap
    }

    fun setBitmapSize(width: Int, height: Int) {
        bezierBitmapThing.setBitmapSize(width, height)
    }
}
