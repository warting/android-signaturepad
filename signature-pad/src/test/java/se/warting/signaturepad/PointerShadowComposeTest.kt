package se.warting.signaturepad

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PointerShadowComposeTest {
    @get:Rule
    val compose = createComposeRule()

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
        compose.onNodeWithTag("pad").performTouchInput { up() }
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
