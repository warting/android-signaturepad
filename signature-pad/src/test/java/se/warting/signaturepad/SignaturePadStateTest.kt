package se.warting.signaturepad

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import se.warting.signaturecore.ExperimentalSignatureApi

@OptIn(ExperimentalSignatureApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignaturePadStateTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun signingUndoRestoreAndClear_updateSurroundingUiAndCallbackState() {
        lateinit var state: SignaturePadState
        compose.setContent {
            state = rememberSignaturePadState()
            Column {
                SignaturePadView(
                    Modifier.size(200.dp).testTag("pad"),
                    state = state,
                    onStartSigning = { assertFalse(state.isEmpty) },
                    onSigned = { assertTrue(state.canUndo()) },
                    onClear = { assertTrue(state.isEmpty) },
                )
                BasicText(
                    "empty=${state.isEmpty};undo=${state.canUndo()};redo=${state.canRedo()}",
                    Modifier.testTag("status")
                )
            }
        }
        assertStatus(empty = true, undo = false)
        compose.onNodeWithTag("pad").performTouchInput { down(Offset(10f, 10f)) }
        assertStatus(empty = false, undo = false)
        compose.onNodeWithTag("pad").performTouchInput {
            moveTo(Offset(60f, 60f))
            moveTo(Offset(90f, 90f))
            up()
        }
        assertStatus(empty = false, undo = true)
        val saved = compose.runOnIdle { state.getSignature() }
        compose.runOnIdle { state.undo() }
        assertStatus(empty = true, undo = false, redo = true)
        compose.runOnIdle { state.redo() }
        assertStatus(empty = false, undo = true)
        compose.runOnIdle { state.undo() }
        compose.runOnIdle { state.setSignature(saved) }
        assertStatus(empty = false, undo = true)
        compose.runOnIdle { state.clear() }
        assertStatus(empty = true, undo = false)
    }

    @Test
    fun resize_preservesInkAndAcceptsInputInNewArea() {
        lateinit var state: SignaturePadState
        val width = mutableStateOf(200.dp)
        compose.setContent {
            state = rememberSignaturePadState()
            SignaturePadView(Modifier.size(width.value, 200.dp).testTag("pad"), state = state)
        }
        compose.onNodeWithTag("pad").performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(60f, 60f))
            moveTo(Offset(90f, 90f))
            up()
        }
        val before = compose.runOnIdle { state.getTransparentSignatureBitmap() }
        compose.runOnIdle { width.value = 300.dp }
        compose.runOnIdle {
            val after = state.getTransparentSignatureBitmap()
            assertEquals(state.size.width, after.width)
            assertTrue(after.width > before.width)
            assertTrue(before.sameAs(Bitmap.createBitmap(after, 0, 0, before.width, before.height)))
        }
        val newX = before.width + 10f
        compose.onNodeWithTag("pad").performTouchInput {
            down(Offset(newX, 20f))
            moveTo(Offset(newX + 10f, 20f))
            moveTo(Offset(newX + 20f, 20f))
            up()
        }
        compose.runOnIdle {
            val bitmap = state.getTransparentSignatureBitmap()
            assertTrue(android.graphics.Color.alpha(bitmap.getPixel(newX.toInt() + 5, 20)) > 0)
        }
    }

    private fun assertStatus(empty: Boolean, undo: Boolean, redo: Boolean = false) {
        compose.onNodeWithTag("status").assertTextEquals("empty=$empty;undo=$undo;redo=$redo")
    }
}
