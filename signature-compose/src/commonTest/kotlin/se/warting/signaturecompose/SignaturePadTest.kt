package se.warting.signaturecompose

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SignaturePadTest {
    @Test
    fun drawUndoRedoClearAndRestore() = runComposeUiTest {
        val state = SignaturePadState()
        setContent { SignaturePad(Modifier.testTag("pad"), state) }
        onNodeWithTag("pad").performTouchInput {
            down(Offset(20f, 30f))
            moveTo(Offset(60f, 70f))
            moveTo(Offset(100f, 30f))
            moveTo(Offset(140f, 60f))
            up()
        }
        runOnIdle {
            assertFalse(state.isEmpty)
            assertTrue(state.canUndo)
            assertTrue(state.segments.isNotEmpty())
            val saved = state.events
            val svg = state.toSvg(320, 160)
            assertTrue(svg.contains("<path"))
            state.undo()
            assertTrue(state.isEmpty)
            assertTrue(state.canRedo)
            state.redo()
            assertEquals(saved, state.events)
            assertEquals(svg, state.toSvg(320, 160))
            state.clear()
            assertTrue(state.isEmpty)
            state.restore(saved)
            assertEquals(svg, state.toSvg(320, 160))
        }
    }
}
