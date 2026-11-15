package se.warting.signaturepad.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
    fun rawStylusEventsPreservePressureAcrossDetach() = verifyStylusDetach(MotionEvent.TOOL_TYPE_STYLUS)

    @Test
    fun reversedStylusPreservesAxesAcrossDetach() = verifyStylusDetach(MotionEvent.TOOL_TYPE_ERASER)

    @Test
    fun normalStylusHandoffPreservesPressureAndAxes() = verifyStylusDetach(MotionEvent.TOOL_TYPE_STYLUS, detach = false)

    private fun verifyStylusDetach(tool: Int, detach: Boolean = true) {
        val state = compose.activity.inkState
        compose.runOnIdle {
            val view = checkNotNull(findInk(compose.activity.window.decorView))
            val start = SystemClock.uptimeMillis() - 100
            val properties = MotionEvent.PointerProperties().apply {
                id = 7
                toolType = tool
            }
            listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_UP).forEachIndexed { index, action ->
                val coords = MotionEvent.PointerCoords().apply {
                    x = 30f + index * 30f
                    y = 50f + index * 10f
                    pressure = 0.2f + index * 0.2f
                    size = 0.1f
                    setAxisValue(MotionEvent.AXIS_TILT, 0.4f)
                    orientation = 0.5f
                }
                val event = MotionEvent.obtain(start, start + index * 20, action, 1,
                    arrayOf(properties), arrayOf(coords), 0, 0, 1f, 1f, 0, 0,
                    InputDevice.SOURCE_STYLUS, 0)
                try { assertTrue(view.dispatchTouchEvent(event)) } finally { event.recycle() }
            }
            if (detach) (view.parent as ViewGroup).removeView(view)
        }
        compose.waitUntil(10_000) { !state.isDrawing && state.strokes.isNotEmpty() }
        compose.runOnIdle {
            val inputs = state.strokes.single().inputs
            assertEquals(InputToolType.STYLUS, inputs[0].toolType)
            assertTrue(inputs.hasPressure())
            assertEquals(0.4f, inputs[0].tiltRadians, 0.001f)
            assertEquals((0.5f + 2.5f * Math.PI.toFloat()).mod(2 * Math.PI.toFloat()),
                inputs[0].orientationRadians, 0.001f)
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

    @Test
    fun cancelledGesturePreservesRedoAndDisabledViewRejectsInput() {
        val state = compose.activity.inkState
        compose.runOnIdle { state.restore(listOf(stroke(0.9f))); state.undo() }
        compose.onNodeWithTag("ink").performTouchInput {
            down(Offset(30f, 60f))
            moveTo(Offset(100f, 100f), 32)
            cancel()
        }
        compose.runOnIdle {
            assertTrue(state.canRedo)
            val view = checkNotNull(findInk(compose.activity.window.decorView))
            view.isEnabled = false
            val now = SystemClock.uptimeMillis()
            val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 30f, 50f, 0)
            try { assertFalse(view.dispatchTouchEvent(event)) } finally { event.recycle() }
            assertFalse(state.isDrawing)
            assertTrue(state.canRedo)
        }
    }

    @Test
    fun endedStrokeSurvivesImmediateDetachBeforeHandoff() {
        val state = compose.activity.inkState
        compose.runOnIdle {
            val view = checkNotNull(findInk(compose.activity.window.decorView))
            val now = SystemClock.uptimeMillis() - 60
            listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP)
                .forEachIndexed { index, action ->
                    val event = MotionEvent.obtain(now, now + index * 20, action,
                        30f + index * 50, 50f + index * 20, 0)
                    try { view.dispatchTouchEvent(event) } finally { event.recycle() }
                }
            assertTrue(state.isDrawing)
            (view.parent as ViewGroup).removeView(view)
        }
        compose.waitUntil(10_000) { !state.isDrawing && state.strokes.size == 1 }
    }

    @Test
    fun disablingImmediatelyCancelsInputWithoutAnotherEvent() {
        val state = compose.activity.inkState
        compose.runOnIdle { state.restore(listOf(stroke(0.9f))); state.undo() }
        compose.onNodeWithTag("ink").performTouchInput { down(Offset(30f, 60f)) }
        compose.runOnIdle {
            assertTrue(state.isDrawing)
            checkNotNull(findInk(compose.activity.window.decorView)).isEnabled = false
            assertFalse(state.isDrawing)
            assertTrue(state.canRedo)
            state.toBitmap(320, 180).recycle()
        }
    }

    @Test
    fun paddingKeepsBothDrawingLayersInWrapperCoordinates() {
        val view = compose.runOnIdle { checkNotNull(findInk(compose.activity.window.decorView)) }
        compose.runOnIdle { view.setPadding(25, 40, 30, 15) }
        compose.waitForIdle()
        compose.runOnIdle {
            for (index in 0 until view.childCount) {
                val layer = view.getChildAt(index)
                assertEquals(0, layer.left)
                assertEquals(0, layer.top)
                assertEquals(view.width, layer.width)
                assertEquals(view.height, layer.height)
            }
        }
        compose.onNodeWithTag("ink").performTouchInput {
            down(Offset(10f, 20f))
            moveTo(Offset(10f, 60f), 32)
            moveTo(Offset(10f, 100f), 32)
            up()
        }
        val state = compose.activity.inkState
        compose.waitUntil(10_000) { !state.isDrawing && state.strokes.isNotEmpty() }
        compose.runOnIdle {
            assertEquals(10f, state.strokes.single().inputs[0].x, 0.01f)
            assertEquals(20f, state.strokes.single().inputs[0].y, 0.01f)
            assertFalse(view.clipToPadding)
            val rendered = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                rendered.eraseColor(Color.WHITE)
                view.draw(Canvas(rendered))
                var black = 0
                var white = 0
                for (y in 0 until 100) for (x in 0 until 25) {
                    if (rendered.getPixel(x, y) == Color.BLACK) black++
                    if (rendered.getPixel(x, y) == Color.WHITE) white++
                }
                assertTrue("Stroke inside left padding must remain visible", black > 0)
                assertTrue("Padding must not be a solid rendering layer", white > 0)
            } finally { rendered.recycle() }
        }
    }

    private fun findInk(view: View): InkSignatureView? {
        if (view is InkSignatureView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findInk(view.getChildAt(index))?.let { return it }
        }
        return null
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
