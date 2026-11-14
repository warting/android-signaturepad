package se.warting.signaturepad

import android.graphics.Bitmap
import android.view.ViewConfiguration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
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
import se.warting.signaturecore.SignatureInput
import se.warting.signaturecore.SignatureSDK
import se.warting.signaturecore.utils.SignedListener
import se.warting.signaturepad.compose.BuildConfig
import kotlin.math.roundToInt

/**
 * Creates and remembers a [SignaturePadState]. The underlying signature events are preserved
 * across configuration changes via [rememberSaveable].
 */
@Composable
fun rememberSignaturePadState(): SignaturePadState {
    val sdk = rememberSaveable(saver = SignatureSdkSaver) { SignatureSDK() }
    return remember(sdk) { SignaturePadState.create(sdk) }
}

/** Keeps the original source and binary signature for existing callers. */
@SuppressWarnings("LongParameterList")
@Composable
fun SignaturePadView(
    modifier: Modifier = Modifier,
    state: SignaturePadState = rememberSignaturePadState(),
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
    SignaturePadView(
        modifier = modifier,
        state = state,
        penMinWidth = penMinWidth,
        penMaxWidth = penMaxWidth,
        penColor = penColor,
        velocityFilterWeight = velocityFilterWeight,
        clearOnDoubleClick = clearOnDoubleClick,
        shadowIntensity = 0f,
        onReady = onReady,
        onStartSigning = onStartSigning,
        onSigning = onSigning,
        onSigned = onSigned,
        onClear = onClear,
    )
}

/**
 * Signature pad with an optional live finger shadow on Android 13 and newer.
 * [shadowIntensity] opts in explicitly; zero disables it. Exports never contain the shadow.
 */
@SuppressWarnings("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@Composable
fun SignaturePadView(
    modifier: Modifier = Modifier,
    state: SignaturePadState = rememberSignaturePadState(),
    penMinWidth: Dp = 3.dp,
    penMaxWidth: Dp = 7.dp,
    penColor: Color = Color.Black,
    velocityFilterWeight: Float = 0.9F,
    clearOnDoubleClick: Boolean = false,
    shadowColor: Color = Color.Black,
    shadowIntensity: Float,
    shadowAngleDegrees: Float = SignatureSDK.DEFAULT_ATTR_SHADOW_ANGLE_DEGREES,
    onReady: (svg: SignaturePadAdapter) -> Unit = {},
    onStartSigning: () -> Unit = {},
    onSigning: () -> Unit = {},
    onSigned: () -> Unit = {},
    onClear: () -> Unit = {},
) {
    val density = LocalDensity.current
    val sdk = state.sdk
    val input = sdk.input
    var penConfigured by remember(sdk) { mutableStateOf(false) }

    val minWidthPx = with(density) { penMinWidth.toPx() }.roundToInt()
    val maxWidthPx = with(density) { penMaxWidth.toPx() }.roundToInt()
    val penColorArgb = penColor.toArgb()
    val shadowColorArgb = shadowColor.toArgb()

    LaunchedEffect(
        sdk,
        minWidthPx,
        maxWidthPx,
        penColorArgb,
        velocityFilterWeight,
    ) {
        sdk.configure(
            minWidth = minWidthPx,
            maxWidth = maxWidthPx,
            penColor = penColorArgb,
            velocityFilterWeight = velocityFilterWeight,
        )
        penConfigured = true
        sdk.initializeBitmap(state.size.width, state.size.height)
        state.invalidate()
    }

    // Shadow controls must not reset the pen's in-progress stroke width.
    LaunchedEffect(sdk, shadowColorArgb, shadowIntensity, shadowAngleDegrees) {
        sdk.configureShadow(
            shadowColor = shadowColorArgb,
            shadowIntensity = shadowIntensity,
        )
        sdk.configureShadowAngle(shadowAngleDegrees)
        if (sdk.hasBitmap()) {
            state.invalidate()
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

    @Suppress("DEPRECATION")
    val adapter = remember(state) { SignaturePadAdapter.create(state) }

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
            .onSizeChanged { newSize ->
                if (newSize.width > 0 && newSize.height > 0) {
                    state.size = newSize
                    canvasSize = newSize
                    if (penConfigured) sdk.initializeBitmap(newSize.width, newSize.height)
                }
            }
            .pointerInput(sdk) {
                val doubleTapTimeoutMs = ViewConfiguration.getDoubleTapTimeout().toLong()
                var lastTapUpTime = 0L

                awaitEachGesture {
                    try {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val isDoubleTap = currentClearOnDoubleClick &&
                            (downTime - lastTapUpTime) < doubleTapTimeoutMs

                        if (isDoubleTap) {
                            state.clear()
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

                        input.start(down.id.value, down.position.x, down.position.y, down.pressure, downTime)
                        state.invalidate()
                        down.consume()

                        var moved = false
                        var done = false
                        while (!done) {
                            val event = awaitPointerEvent()
                            // A clear/restore invalidates this gesture. awaitEachGesture
                            // waits for all pointers to lift before accepting another down.
                            if (!input.isActive(down.id.value)) {
                                lastTapUpTime = 0L
                                break
                            }
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) {
                                input.cancel()
                                done = true
                            } else if (!change.pressed) {
                                input.end(
                                    down.id.value,
                                    change.position.x,
                                    change.position.y,
                                    System.currentTimeMillis()
                                )
                                state.invalidate()
                                change.consume()
                                // Track tap time only when the gesture had no
                                // movement, so a double-stroke (drag-drag) does
                                // not also trigger clear.
                                lastTapUpTime = if (moved) 0L else System.currentTimeMillis()
                                done = true
                            } else {
                                if (change.positionChanged()) moved = true
                                input.move(
                                    down.id.value,
                                    change.position.x,
                                    change.position.y,
                                    change.pressure,
                                    System.currentTimeMillis()
                                )
                                state.invalidate()
                                change.consume()
                            }
                        }
                    } finally {
                        input.cancel()
                        state.invalidate()
                    }
                }
            },
    ) {
        @Suppress("UNUSED_EXPRESSION")
        state.redrawTick
        // Core replays restored events once, after the pen is configured and size is known.
        if (penConfigured) sdk.initializeBitmap(canvasSize.width, canvasSize.height)
        val nativeCanvas = drawContext.canvas.nativeCanvas
        sdk.drawPointerShadow(
            canvas = nativeCanvas,
            width = canvasSize.width,
            height = canvasSize.height,
            pointerX = input.x,
            pointerY = input.y,
            pressure = input.pressure,
        )
        sdk.drawSignature(nativeCanvas)
    }
}

/**
 * Hoisted state for [SignaturePadView]. Use [rememberSignaturePadState] to create one.
 */
@Stable
class SignaturePadState private constructor(
    internal val sdk: SignatureSDK,
) {

    internal companion object {
        @JvmSynthetic
        internal fun create(sdk: SignatureSDK): SignaturePadState = SignaturePadState(sdk)
    }

    internal val shadowPointer: SignatureInput get() = sdk.input

    internal var size: IntSize = IntSize.Zero

    internal var redrawTick: Int by mutableIntStateOf(0)

    internal fun invalidate() {
        redrawTick++
    }

    private fun ensureBitmap() {
        if (!sdk.hasBitmap() && size.width > 0 && size.height > 0) {
            sdk.initializeBitmap(size.width, size.height)
        }
    }

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

    /**
     * @return true if a stroke is available to undo.
     */
    fun canUndo(): Boolean = sdk.canUndo()

    val isEmpty: Boolean
        get() = sdk.isEmpty

    @Suppress("unused")
    fun getSignatureBitmap(): Bitmap = sdk.getSignatureBitmap() ?: createBitmap(1, 1)

    /**
     * Returns a bitmap containing the current signature.
     *
     * @param backgroundColor Color of the bitmap's surface behind the signature
     * @param penColor Color of the signature line in the bitmap
     */
    fun getSignatureBitmap(backgroundColor: Int, penColor: Int? = null): Bitmap =
        sdk.getSignatureBitmap(backgroundColor, penColor) ?: createBitmap(1, 1)

    @Suppress("unused")
    fun getTransparentSignatureBitmap(
        trimBlankSpace: Boolean = false,
        penColor: Int? = null,
    ): Bitmap = sdk.getTransparentSignatureBitmap(trimBlankSpace, penColor) ?: createBitmap(1, 1)

    fun getSignatureSvg(): String = sdk.getSignatureSvg(size.width, size.height)

    /**
     * Returns the current signature as an SVG document with optional coloring.
     *
     * @param penColor ARGB color of the signature stroke. If null the stroke defaults to black.
     * @param backgroundColor ARGB color filled behind the signature. If null the SVG is transparent.
     */
    fun getSignatureSvg(penColor: Int? = null, backgroundColor: Int? = null): String =
        sdk.getSignatureSvg(size.width, size.height, penColor, backgroundColor)

    @ExperimentalSignatureApi
    fun getSignature(): Signature = Signature(BuildConfig.VERSION_CODE, sdk.getEvents())

    @ExperimentalSignatureApi
    fun setSignature(signature: Signature) {
        sdk.restoreSignature(signature.events, size.width, size.height)
        invalidate()
    }
}

@Deprecated(
    "Hoist a SignaturePadState via rememberSignaturePadState() and pass it to " +
        "SignaturePadView instead. SignaturePadAdapter will be removed in a future release.",
    ReplaceWith("SignaturePadState"),
)
class SignaturePadAdapter private constructor(
    private val state: SignaturePadState,
) {

    internal companion object {
        @JvmSynthetic
        internal fun create(state: SignaturePadState): SignaturePadAdapter =
            SignaturePadAdapter(state)
    }

    fun clear() = state.clear()

    fun undo() = state.undo()

    fun canUndo(): Boolean = state.canUndo()

    val isEmpty: Boolean get() = state.isEmpty

    @Suppress("unused")
    fun getSignatureBitmap(): Bitmap = state.getSignatureBitmap()

    fun getSignatureBitmap(backgroundColor: Int, penColor: Int? = null): Bitmap =
        state.getSignatureBitmap(backgroundColor, penColor)

    @Suppress("unused")
    fun getTransparentSignatureBitmap(
        trimBlankSpace: Boolean = false,
        penColor: Int? = null,
    ): Bitmap = state.getTransparentSignatureBitmap(trimBlankSpace, penColor)

    fun getSignatureSvg(): String = state.getSignatureSvg()

    fun getSignatureSvg(penColor: Int? = null, backgroundColor: Int? = null): String =
        state.getSignatureSvg(penColor, backgroundColor)

    @ExperimentalSignatureApi
    fun getSignature(): Signature = state.getSignature()

    @ExperimentalSignatureApi
    fun setSignature(signature: Signature) = state.setSignature(signature)
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
