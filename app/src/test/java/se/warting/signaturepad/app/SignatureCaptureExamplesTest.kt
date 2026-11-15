package se.warting.signaturepad.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignatureCaptureExamplesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun dialog_supportsClearSaveAndCancel() = verifyCapture("Sign in dialog")

    @Test
    fun sheet_supportsClearSaveAndCancel() = verifyCapture("Sign in sheet")

    private fun verifyCapture(openLabel: String) {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme { SignatureCaptureExamples() } }
        compose.onNodeWithText(openLabel).performClick()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        sign()
        compose.onNodeWithText("Save").assertIsEnabled()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Save").assertIsEnabled()
        compose.onNodeWithText("Clear").performClick()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("Sign here").assertExists()
        sign()
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithTag("capture-field").assertDoesNotExist()
        compose.onNodeWithContentDescription("Saved signature").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Saved signature").assertExists()
        compose.onNodeWithText(openLabel).performClick()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        sign()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("capture-field").assertDoesNotExist()
        compose.onNodeWithContentDescription("Saved signature").assertExists()
    }

    private fun sign() {
        compose.onNodeWithTag("capture-field").performTouchInput {
            down(Offset(20f, 20f))
            moveTo(Offset(60f, 40f))
            moveTo(Offset(90f, 60f))
            up()
        }
    }
}
