package se.warting.signaturepad.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import se.warting.signatureink.InkSignatureView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import se.warting.signatureink.InkSignatureState
import java.io.File

class InkSignatureTest {
    @get:Rule
    val compose = createAndroidComposeRule<InkSampleActivity>()

    @Test
    fun realInputHandsOffToHistoryAndClearCancelsAnActiveStroke() {
        val state = compose.activity.inkState
        compose.onNodeWithTag("ink").performTouchInput {
            down(Offset(30f, 60f))
            moveTo(Offset(100f, 100f), 32)
            moveTo(Offset(180f, 40f), 32)
            up()
        }
        compose.waitUntil(10_000) { !state.isDrawing && state.strokes.isNotEmpty() }
        compose.runOnIdle {
            assertEquals(1, state.strokes.size)
            state.undo()
            assertTrue(state.isEmpty)
            state.redo()
            assertEquals(1, state.strokes.size)
            state.clear()
        }
        compose.onNodeWithTag("ink").performTouchInput { down(Offset(40f, 40f)) }
        compose.runOnIdle { state.clear() }
        compose.onNodeWithTag("ink").performTouchInput {
            moveTo(Offset(150f, 80f), 32)
            up()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertFalse(state.isDrawing)
            assertTrue(state.isEmpty)
        }
    }

    @Test
    fun rawStylusEventsPreservePressure() {
        val state = compose.activity.inkState
        compose.runOnIdle {
            fun findInk(view: View): InkSignatureView? {
                if (view is InkSignatureView) return view
                if (view is ViewGroup) for (index in 0 until view.childCount) {
                    findInk(view.getChildAt(index))?.let { return it }
                }
                return null
            }
            val view = checkNotNull(findInk(compose.activity.window.decorView))
            val start = SystemClock.uptimeMillis() - 100
            val properties = MotionEvent.PointerProperties().apply {
                id = 7
                toolType = MotionEvent.TOOL_TYPE_STYLUS
            }
            listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_UP).forEachIndexed { index, action ->
                val coords = MotionEvent.PointerCoords().apply {
                    x = 30f + index * 30f
                    y = 50f + index * 10f
                    pressure = 0.2f + index * 0.2f
                    size = 0.1f
                }
                val event = MotionEvent.obtain(start, start + index * 20, action, 1,
                    arrayOf(properties), arrayOf(coords), 0, 0, 1f, 1f, 0, 0,
                    InputDevice.SOURCE_STYLUS, 0)
                try { assertTrue(view.dispatchTouchEvent(event)) } finally { event.recycle() }
            }
        }
        compose.waitUntil(10_000) { !state.isDrawing && state.strokes.isNotEmpty() }
        compose.runOnIdle {
            val inputs = state.strokes.single().inputs
            assertEquals(InputToolType.STYLUS, inputs[0].toolType)
            assertTrue(inputs.hasPressure())
            assertEquals(0.2f, inputs[0].pressure, 0.001f)
            assertEquals(0.8f, inputs[inputs.size - 1].pressure, 0.001f)
        }
    }

    @Test
    fun pressureChangesInkWidthAndExportMatchesBaseline() {
        compose.runOnIdle {
            val state = InkSignatureState()
            state.restore(listOf(stroke(0.2f)))
            val thin = state.toBitmap(320, 180)
            state.restore(listOf(stroke(0.9f)))
            val thick = state.toBitmap(320, 180)
            assertTrue("Pressure-sensitive brush must change rendered width", inkPixels(thick) > inkPixels(thin))
            thin.recycle()
            thick.recycle()
            val exported = state.toBitmap(320, 180, Color.WHITE)
            val png = state.toPngByteArray(320, 180, Color.WHITE)
            val decoded = BitmapFactory.decodeByteArray(png, 0, png.size)
            assertTrue(exported.sameAs(decoded))
            decoded.recycle()
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val output = File(instrumentation.targetContext.getExternalFilesDir("ink-screenshots"), "signature.png")
            output.writeBytes(png)
            if (InstrumentationRegistry.getArguments().getString("updateInkScreenshots") != "true") {
                val expected = instrumentation.context.assets.open("reference/ink-signature.png").use {
                    BitmapFactory.decodeStream(it)
                }
                assertTrue("Ink screenshot differs: ${output.absolutePath}", exported.sameAs(expected))
                expected.recycle()
            }
            exported.recycle()
        }
    }

    private fun stroke(pressure: Float): Stroke {
        val brush = Brush.createWithColorIntArgb(StockBrushes.pressurePen(), Color.BLACK, 18f, 0.1f)
        val inputs = MutableStrokeInputBatch()
        listOf(30f to 100f, 80f to 40f, 110f to 140f, 180f to 70f, 260f to 100f).forEachIndexed { index, point ->
            inputs.add(StrokeInput.create(point.first, point.second, index * 20L,
                toolType = InputToolType.STYLUS, pressure = pressure))
        }
        return Stroke(brush, inputs)
    }

    private fun inkPixels(bitmap: Bitmap): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { Color.alpha(it) > 0 }
    }
}
