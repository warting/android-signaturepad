package se.warting.signaturepad

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import se.warting.signaturecore.ExperimentalSignatureApi
import se.warting.signaturecore.SignatureSDK

@OptIn(ExperimentalSignatureApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PointerShadowComposeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun restoredBeforeMeasurement_rendersOnceWithConfiguredPenAndNoRepeatedCallbacks() {
        val events = listOf(
            se.warting.signaturecore.Event(0L, android.view.MotionEvent.ACTION_DOWN, 10f, 10f),
            se.warting.signaturecore.Event(16L, android.view.MotionEvent.ACTION_MOVE, 40f, 40f),
            se.warting.signaturecore.Event(32L, android.view.MotionEvent.ACTION_UP, 70f, 70f),
        )
        val state = SignaturePadState.create(SignatureSDK().apply { restoreEvents(events) })
        var callbacks = 0
        var width = 0
        compose.setContent {
            width = with(LocalDensity.current) { 5.dp.roundToPx() }
            SignaturePadView(
                modifier = Modifier.size(200.dp).testTag("pad"),
                state = state,
                penMinWidth = 5.dp,
                penMaxWidth = 5.dp,
                onStartSigning = { callbacks++ },
                onSigning = { callbacks++ },
                onSigned = { callbacks++ },
                onClear = { callbacks++ },
            )
        }
        compose.runOnIdle {
            val expected = SignatureSDK().apply {
                configure(width, width, android.graphics.Color.BLACK, 0.9f)
                initializeBitmap(state.size.width, state.size.height)
                restoreEvents(events)
            }
            assertTrue(expected.getTransparentSignatureBitmap()!!.sameAs(state.getTransparentSignatureBitmap()))
            assertEquals(expected.getSignatureSvg(state.size.width, state.size.height), state.getSignatureSvg())
            assertEquals(events, state.getSignature().events)
            assertEquals(0, callbacks)
        }
    }

    @Test
    fun changingShadowMidStroke_preservesBitmapAndSvg() {
        lateinit var state: SignaturePadState
        var minWidth = 0
        var maxWidth = 0
        val intensity = mutableStateOf(0f)
        compose.setContent {
            state = rememberSignaturePadState()
            minWidth = with(LocalDensity.current) { 3.dp.roundToPx() }
            maxWidth = with(LocalDensity.current) { 7.dp.roundToPx() }
            SignaturePadView(
                modifier = Modifier.size(200.dp).testTag("pad"),
                state = state,
                shadowIntensity = intensity.value,
            )
        }
        compose.onNodeWithTag("pad").performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(40f, 40f))
            moveTo(Offset(70f, 70f))
            moveTo(Offset(100f, 80f))
        }
        compose.runOnIdle { intensity.value = 0.5f }
        compose.onNodeWithTag("pad").performTouchInput {
            moveTo(Offset(130f, 90f))
            moveTo(Offset(150f, 110f))
            up()
        }
        compose.runOnIdle {
            val expected = SignatureSDK().apply {
                configure(minWidth, maxWidth, android.graphics.Color.BLACK, 0.9f)
                initializeBitmap(state.size.width, state.size.height)
                state.sdk.getEvents().forEach(::addEvent)
            }
            assertEquals(expected.getSignatureSvg(state.size.width, state.size.height), state.getSignatureSvg())
            assertTrue(state.getTransparentSignatureBitmap().sameAs(expected.getTransparentSignatureBitmap()))
        }
    }

    @Test
    fun cancelAndClear_removeActivePointer() {
        lateinit var state: SignaturePadState
        compose.setContent {
            state = rememberSignaturePadState()
            SignaturePadView(
                modifier = Modifier.size(200.dp).testTag("pad"),
                state = state,
                shadowIntensity = 0.5f,
            )
        }
        compose.onNodeWithTag("pad").performTouchInput { down(center) }
        compose.runOnIdle { assertTrue(state.shadowPointer.pressure > 0f) }
        compose.onNodeWithTag("pad").performTouchInput { cancel() }
        compose.runOnIdle { assertEquals(0f, state.shadowPointer.pressure) }
        compose.onNodeWithTag("pad").performTouchInput { down(center) }
        compose.runOnIdle {
            state.clear()
            assertEquals(0f, state.shadowPointer.pressure)
        }
        compose.onNodeWithTag("pad").performTouchInput {
            moveTo(Offset(150f, 150f))
            up()
        }
        compose.runOnIdle {
            assertEquals(0f, state.shadowPointer.pressure)
            assertTrue(state.getSignature().events.isEmpty())
            assertTrue(state.isEmpty)
        }
        compose.onNodeWithTag("pad").performTouchInput { down(center) }
        compose.runOnIdle { assertTrue(state.shadowPointer.pressure > 0f) }
        compose.onNodeWithTag("pad").performTouchInput { up() }
    }

    @Test
    fun restoringMidGesture_ignoresRemainingTouchEvents() {
        lateinit var state: SignaturePadState
        compose.setContent {
            state = rememberSignaturePadState()
            SignaturePadView(
                modifier = Modifier.size(200.dp).testTag("pad"),
                state = state,
                shadowIntensity = 0.5f,
            )
        }
        compose.onNodeWithTag("pad").performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(40f, 40f))
            up()
        }
        val saved = compose.runOnIdle { state.getSignature() }
        compose.onNodeWithTag("pad").performTouchInput {
            down(Offset(70f, 70f))
            moveTo(Offset(100f, 100f))
        }
        val expectedBitmap = compose.runOnIdle {
            state.setSignature(saved)
            state.getTransparentSignatureBitmap().copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        }
        val expectedSvg = compose.runOnIdle { state.getSignatureSvg() }
        compose.onNodeWithTag("pad").performTouchInput {
            moveTo(Offset(150f, 150f))
            up()
        }
        compose.runOnIdle {
            assertEquals(saved.events, state.getSignature().events)
            assertEquals(expectedSvg, state.getSignatureSvg())
            assertTrue(expectedBitmap.sameAs(state.getTransparentSignatureBitmap()))
            assertEquals(0f, state.shadowPointer.pressure)
        }
    }

    @Test
    fun liftingSigningFinger_doesNotTransferStrokeToRemainingFinger() {
        lateinit var state: SignaturePadState
        compose.setContent {
            state = rememberSignaturePadState()
            SignaturePadView(
                modifier = Modifier.size(200.dp).testTag("pad"),
                state = state,
                shadowIntensity = 0.5f,
            )
        }
        compose.onNodeWithTag("pad").performTouchInput {
            down(0, Offset(10f, 10f))
            down(1, Offset(100f, 100f))
            up(0)
        }
        val saved = compose.runOnIdle {
            assertEquals(0f, state.shadowPointer.pressure)
            state.getSignature()
        }
        compose.onNodeWithTag("pad").performTouchInput {
            moveTo(1, Offset(150f, 150f))
            up(1)
        }
        compose.runOnIdle { assertEquals(saved.events, state.getSignature().events) }
    }

    @Test
    fun removingCanvas_clearsPointerInHoistedState() {
        lateinit var state: SignaturePadState
        val visible = mutableStateOf(true)
        compose.setContent {
            state = rememberSignaturePadState()
            if (visible.value) {
                SignaturePadView(
                    modifier = Modifier.size(200.dp).testTag("pad"),
                    state = state,
                    shadowIntensity = 0.5f,
                )
            }
        }
        compose.onNodeWithTag("pad").performTouchInput { down(center) }
        compose.runOnIdle {
            assertTrue(state.shadowPointer.pressure > 0f)
            visible.value = false
        }
        compose.runOnIdle { assertEquals(0f, state.shadowPointer.pressure) }
    }
}
