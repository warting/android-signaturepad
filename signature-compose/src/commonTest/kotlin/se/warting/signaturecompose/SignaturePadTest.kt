package se.warting.signaturecompose

import androidx.compose.foundation.background
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import se.warting.signatureengine.SignatureEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
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
    @Test
    fun svgOnlyObserversRecomposeWhenEventsChange() = runComposeUiTest {
        val state = SignaturePadState()
        var observed = ""
        setContent {
            val svg = state.toSvg(320, 160)
            SideEffect { observed = svg }
        }
        runOnIdle { state.restore(fixture()) }
        runOnIdle { assertTrue(observed.contains("<path")) }
        runOnIdle { state.clear() }
        runOnIdle { assertFalse(observed.contains("<path")) }
    }

    @Test
    fun rasterCacheAppendsOnceAndRebuildsAfterHistoryChanges() = runComposeUiTest {
        runOnIdle {
            val state = SignaturePadState()
            state.restore(fixture())
            val cache = SignatureRasterCache()
            fun render() = checkNotNull(cache.render(state.document, state.rasterGeneration, 320, 160, 1f, Color.Black))
            render()
            val initial = cache.rasterizedSegments
            assertEquals(state.document.segmentCount, initial)
            repeat(5) { render() }
            assertEquals(initial, cache.rasterizedSegments)
            state.document.input.start(1, 100f, 20f, 1f, 100)
            state.document.input.move(1, 140f, 50f, 1f, 120)
            state.document.input.end(1, 180f, 20f, 140)
            render()
            assertEquals(state.document.segmentCount, cache.rasterizedSegments)
            state.clear()
            val pixels = render().toPixelMap()
            assertTrue((0 until pixels.height).all { y ->
                (0 until pixels.width).all { x -> pixels[x, y].alpha == 0f }
            })
            state.restore(fixture())
            val restored = render().toPixelMap()
            assertTrue((0 until restored.height).any { y ->
                (0 until restored.width).any { x -> restored[x, y].alpha > 0f }
            })
        }
    }

    @Test
    fun cachedInkIsVisibleAfterAppendUndoAndClear() = runComposeUiTest {
        val state = SignaturePadState()
        setContent { SignaturePad(Modifier.background(Color.White).testTag("pad"), state) }
        fun inkPixels(): Int {
            val pixels = onNodeWithTag("pad").captureToImage().toPixelMap()
            var count = 0
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                if (pixels[x, y] != Color.White) count++
            }
            return count
        }
        runOnIdle { state.restore(fixture()) }
        val original = inkPixels()
        assertTrue(original > 0)
        runOnIdle {
            state.document.input.start(8, 180f, 120f, 1f, 100)
            state.document.input.move(8, 230f, 140f, 1f, 120)
            state.document.input.end(8, 280f, 120f, 140)
        }
        assertTrue(inkPixels() > original)
        runOnIdle { state.undo() }
        assertEquals(original, inkPixels())
        runOnIdle { state.clear() }
        assertEquals(0, inkPixels())
    }

    private fun fixture() = listOf(
        SignatureEvent(0, 0, 20f, 30f), SignatureEvent(16, 2, 60f, 70f),
        SignatureEvent(32, 2, 100f, 30f), SignatureEvent(48, 1, 140f, 60f),
    )
}
