package se.warting.signaturepad

import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import se.warting.signaturecore.Event
import se.warting.signaturecore.ExperimentalSignatureApi
import se.warting.signaturecore.Signature
import se.warting.signaturecore.SignatureSDK
import se.warting.signaturecore.utils.SignedListener
import se.warting.signaturepad.compose.BuildConfig
import kotlin.math.roundToInt

@SuppressWarnings("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@Composable
fun SignaturePadView(
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
) {
    val density = LocalDensity.current
    val sdk = rememberSaveable(saver = SignatureSdkSaver) { SignatureSDK() }
    var redrawTick by remember { mutableIntStateOf(0) }
    val adapter = remember(sdk) { SignaturePadAdapter(sdk) { redrawTick++ } }
    var bitmapInitialized by remember(sdk) { mutableStateOf(sdk.hasBitmap()) }

    val minWidthPx = with(density) { penMinWidth.toPx() }.roundToInt()
    val maxWidthPx = with(density) { penMaxWidth.toPx() }.roundToInt()
    val penColorArgb = penColor.toArgb()

    LaunchedEffect(sdk, minWidthPx, maxWidthPx, penColorArgb, velocityFilterWeight) {
        sdk.configure(
            minWidth = minWidthPx,
            maxWidth = maxWidthPx,
            penColor = penColorArgb,
            velocityFilterWeight = velocityFilterWeight,
        )
    }

    // After bitmap initialization, replay any restored events so they render
    // onto the new bitmap. Without this, a saved signature survives a config
    // change in originalEvents but never gets drawn.
    LaunchedEffect(sdk, bitmapInitialized) {
        if (bitmapInitialized) {
            val restored = sdk.getEvents()
            if (restored.isNotEmpty()) {
                sdk.restoreEvents(restored)
                redrawTick++
            }
        }
    }

    val currentOnStartSigning by rememberUpdatedState(onStartSigning)
    val currentOnSigning by rememberUpdatedState(onSigning)
    val currentOnSigned by rememberUpdatedState(onSigned)
    val currentOnClear by rememberUpdatedState(onClear)

    DisposableEffect(sdk) {
        sdk.setOnSignedListener(object : SignedListener {
            override fun onStartSigning() {
                currentOnStartSigning()
            }

            override fun onSigning() {
                currentOnSigning()
            }

            override fun onSigned() {
                currentOnSigned()
            }

            override fun onClear() {
                currentOnClear()
            }
        })
        onDispose { sdk.setOnSignedListener(null) }
    }

    // The legacy AndroidView-backed implementation called onReady on every
    // recomposition (via AndroidView's update block). Some sample code captures
    // the adapter into a non-remembered local var that gets reset on each
    // recomposition, so we match the legacy cadence to keep that pattern
    // working.
    SideEffect {
        onReady(adapter)
    }

    val currentClearOnDoubleClick by rememberUpdatedState(clearOnDoubleClick)

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { newSize ->
                if (newSize.width > 0 && newSize.height > 0) {
                    adapter.size = newSize
                    canvasSize = newSize
                    if (!sdk.hasBitmap()) {
                        sdk.initializeBitmap(newSize.width, newSize.height)
                        bitmapInitialized = true
                    }
                }
            }
            .pointerInput(sdk) {
                val doubleTapTimeoutMs = ViewConfiguration.getDoubleTapTimeout().toLong()
                var lastTapUpTime = 0L

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val isDoubleTap = currentClearOnDoubleClick &&
                        (downTime - lastTapUpTime) < doubleTapTimeoutMs

                    if (isDoubleTap) {
                        sdk.clear()
                        redrawTick++
                        lastTapUpTime = 0L
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                change?.consume()
                                break
                            }
                            change.consume()
                        }
                        return@awaitEachGesture
                    }

                    sdk.addEvent(
                        Event(
                            downTime,
                            MotionEvent.ACTION_DOWN,
                            down.position.x,
                            down.position.y,
                        ),
                    )
                    redrawTick++
                    down.consume()

                    var moved = false
                    var done = false
                    while (!done) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null) {
                            done = true
                        } else if (!change.pressed) {
                            sdk.addEvent(
                                Event(
                                    System.currentTimeMillis(),
                                    MotionEvent.ACTION_UP,
                                    change.position.x,
                                    change.position.y,
                                ),
                            )
                            redrawTick++
                            change.consume()
                            // Track tap time only when the gesture had no
                            // movement, so a double-stroke (drag-drag) does
                            // not also trigger clear.
                            lastTapUpTime = if (moved) 0L else System.currentTimeMillis()
                            done = true
                        } else {
                            if (change.positionChanged()) moved = true
                            sdk.addEvent(
                                Event(
                                    System.currentTimeMillis(),
                                    MotionEvent.ACTION_MOVE,
                                    change.position.x,
                                    change.position.y,
                                ),
                            )
                            redrawTick++
                            change.consume()
                        }
                    }
                }
            },
    ) {
        @Suppress("UNUSED_EXPRESSION")
        redrawTick
        if (!sdk.hasBitmap() && canvasSize.width > 0 && canvasSize.height > 0) {
            sdk.initializeBitmap(canvasSize.width, canvasSize.height)
        }
        sdk.drawSignature(drawContext.canvas.nativeCanvas)
    }
}

class SignaturePadAdapter internal constructor(
    private val sdk: SignatureSDK,
    private val invalidate: () -> Unit,
) {

    internal var size: IntSize = IntSize.Zero

    fun clear() {
        sdk.clear()
        ensureBitmap()
        invalidate()
    }

    /**
     * Undo the last stroke. Has no effect when there is nothing to undo.
     */
    fun undo() {
        sdk.undo()
        ensureBitmap()
        invalidate()
    }

    private fun ensureBitmap() {
        if (!sdk.hasBitmap() && size.width > 0 && size.height > 0) {
            sdk.initializeBitmap(size.width, size.height)
        }
    }

    /**
     * @return true if a stroke is available to undo.
     */
    fun canUndo(): Boolean = sdk.canUndo()

    val isEmpty: Boolean
        get() = sdk.isEmpty

    @Suppress("unused")
    fun getSignatureBitmap(): Bitmap {
        return sdk.getSignatureBitmap() ?: createBitmap(1, 1)
    }

    /**
     * Returns a bitmap containing the current signature.
     *
     * @param backgroundColor Color of the bitmap's surface behind the signature
     * @param penColor Color of the signature line in the bitmap
     */
    fun getSignatureBitmap(backgroundColor: Int, penColor: Int? = null): Bitmap {
        return sdk.getSignatureBitmap(backgroundColor, penColor) ?: createBitmap(1, 1)
    }

    @Suppress("unused")
    fun getTransparentSignatureBitmap(
        trimBlankSpace: Boolean = false,
        penColor: Int? = null,
    ): Bitmap {
        return sdk.getTransparentSignatureBitmap(trimBlankSpace, penColor) ?: createBitmap(1, 1)
    }

    fun getSignatureSvg(): String {
        return sdk.getSignatureSvg(size.width, size.height)
    }

    /**
     * Returns the current signature as an SVG document with optional coloring.
     *
     * @param penColor ARGB color of the signature stroke. If null the stroke defaults to black.
     * @param backgroundColor ARGB color filled behind the signature. If null the SVG is transparent.
     */
    fun getSignatureSvg(penColor: Int? = null, backgroundColor: Int? = null): String {
        return sdk.getSignatureSvg(size.width, size.height, penColor, backgroundColor)
    }

    @ExperimentalSignatureApi
    fun getSignature(): Signature {
        return Signature(BuildConfig.VERSION_CODE, sdk.getEvents())
    }

    @ExperimentalSignatureApi
    fun setSignature(signature: Signature) {
        sdk.clear()
        ensureBitmap()
        sdk.restoreEvents(signature.events)
        invalidate()
    }
}

private val SignatureSdkSaver: Saver<SignatureSDK, ArrayList<Event>> = Saver(
    save = { ArrayList(it.getEvents()) },
    restore = { events ->
        SignatureSDK().apply {
            if (events.isNotEmpty()) {
                restoreEvents(events)
            }
        }
    },
)
