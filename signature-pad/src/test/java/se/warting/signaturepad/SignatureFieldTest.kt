package se.warting.signaturepad

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
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
class SignatureFieldTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun placeholderTracksSignatureAndDecorationsAreNotExported() {
        lateinit var state: SignaturePadState
        compose.setContent {
            state = rememberSignaturePadState()
            SignatureField(Modifier.testTag("field"), state = state) {
                BasicText("Sign here")
            }
        }
        compose.onNodeWithText("Sign here").assertExists()
        compose.runOnIdle {
            val bitmap = state.getTransparentSignatureBitmap()
            assertTrue(bitmap.width > 1 && bitmap.height > 1)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue(pixels.all { it == android.graphics.Color.TRANSPARENT })
        }
        compose.onNodeWithTag("field").performTouchInput {
            down(Offset(20f, 20f))
            moveTo(Offset(60f, 40f))
            moveTo(Offset(90f, 60f))
            up()
        }
        compose.onNodeWithText("Sign here").assertDoesNotExist()
        compose.runOnIdle { state.clear() }
        compose.onNodeWithText("Sign here").assertExists()
    }
}
