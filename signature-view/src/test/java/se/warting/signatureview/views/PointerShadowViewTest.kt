package se.warting.signatureview.views

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.InputDevice
import android.view.MotionEvent
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import se.warting.signaturecore.ExperimentalSignatureApi

@OptIn(ExperimentalSignatureApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PointerShadowViewTest {
    @Test
    fun redo_restoresPixelsSvgAndEvents() {
        val pad = createPad()
        touch(pad, MotionEvent.ACTION_DOWN, 20f)
        touch(pad, MotionEvent.ACTION_MOVE, 50f)
        touch(pad, MotionEvent.ACTION_MOVE, 80f)
        touch(pad, MotionEvent.ACTION_UP, 110f)
        val bitmap = pad.getTransparentSignatureBitmap()
        val svg = pad.getSignatureSvg()
        val events = pad.getSignature().events
        pad.undo()
        assertTrue(pad.canRedo())
        assertTrue(pad.isEmpty)
        pad.redo()
        assertTrue(!pad.canRedo())
        assertEquals(events, pad.getSignature().events)
        assertEquals(svg, pad.getSignatureSvg())
        assertTrue(bitmap.sameAs(pad.getTransparentSignatureBitmap()))
    }

    @Test
    fun resize_updatesBitmapBeforeNextDrawAndAcceptsInputInNewArea() {
        val pad = createPad()
        render(pad)
        pad.layout(0, 0, 400, 200)
        // Touch and export may happen before the first draw at the new size.
        touch(pad, MotionEvent.ACTION_DOWN, 250f)
        touch(pad, MotionEvent.ACTION_MOVE, 280f)
        touch(pad, MotionEvent.ACTION_MOVE, 310f)
        touch(pad, MotionEvent.ACTION_UP, 340f)
        val bitmap = pad.getTransparentSignatureBitmap()
        val options = se.warting.signaturecore.SignatureExportOptions(paddingPixels = 4)
        val bytes = requireNotNull(pad.toPngByteArray(options))
        val png = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertEquals(bitmap.width + 8, png.width)
        assertEquals(bitmap.height + 8, png.height)
        assertTrue(bytes.contentEquals(android.util.Base64.decode(pad.toBase64(options), 0)))
        assertEquals(400, bitmap.width)
        assertEquals(200, bitmap.height)
        assertTrue(android.graphics.Color.alpha(bitmap.getPixel(280, 100)) > 0)
    }

    @Test
    fun liftingActiveFinger_endsStrokeAndKeepsShadowOffUntilNewGesture() {
        val pad = createPad()
        touch(pad, MotionEvent.ACTION_DOWN)
        multiTouch(pad, MotionEvent.ACTION_POINTER_DOWN, listOf(0, 1), actionIndex = 1)
        assertShadowVisible(pad)
        multiTouch(pad, MotionEvent.ACTION_POINTER_UP, listOf(0, 1), actionIndex = 0)
        assertShadowHidden(pad)
        val events = pad.getSignature().events
        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP), events.map { it.action })
        assertEquals(100f, events.last().x)
        multiTouch(pad, MotionEvent.ACTION_MOVE, listOf(1))
        multiTouch(pad, MotionEvent.ACTION_UP, listOf(1))
        assertEquals(events, pad.getSignature().events)
        assertShadowHidden(pad)
        touch(pad, MotionEvent.ACTION_DOWN)
        assertShadowVisible(pad)
    }

    @Test
    fun liftingOtherFinger_doesNotClearActiveShadowOrStorePointerEvents() {
        val pad = createPad()
        touch(pad, MotionEvent.ACTION_DOWN)
        multiTouch(pad, MotionEvent.ACTION_POINTER_DOWN, listOf(0, 1), actionIndex = 1)
        multiTouch(pad, MotionEvent.ACTION_POINTER_UP, listOf(0, 1), actionIndex = 1)
        assertShadowVisible(pad)
        assertEquals(listOf(MotionEvent.ACTION_DOWN), pad.getSignature().events.map { it.action })
        touch(pad, MotionEvent.ACTION_UP)
        assertShadowHidden(pad)
    }

    @Test
    fun cancel_removesShadowAndNextGestureWorks() {
        val pad = createPad()
        touch(pad, MotionEvent.ACTION_DOWN)
        assertShadowVisible(pad)
        touch(pad, MotionEvent.ACTION_CANCEL)
        assertShadowHidden(pad)
        touch(pad, MotionEvent.ACTION_DOWN)
        assertShadowVisible(pad)
        touch(pad, MotionEvent.ACTION_UP)
        assertShadowHidden(pad)
    }

    @Test
    fun clearAndDisable_removeActiveShadow() {
        val pad = createPad()
        touch(pad, MotionEvent.ACTION_DOWN)
        pad.clear()
        touch(pad, MotionEvent.ACTION_MOVE)
        touch(pad, MotionEvent.ACTION_UP)
        assertTrue(pad.getSignature().events.isEmpty())
        assertShadowHidden(pad)
        touch(pad, MotionEvent.ACTION_DOWN)
        assertShadowVisible(pad)
        pad.isEnabled = false
        assertShadowHidden(pad)
    }

    @Test
    fun restoreDuringTouch_preservesExportAndStopsRemainingInput() {
        val pad = createPad()
        touch(pad, MotionEvent.ACTION_DOWN)
        touch(pad, MotionEvent.ACTION_UP)
        val saved = pad.getSignature()
        touch(pad, MotionEvent.ACTION_DOWN)
        touch(pad, MotionEvent.ACTION_MOVE)
        pad.setSignature(saved)
        val bitmap = pad.getTransparentSignatureBitmap()
        val svg = pad.getSignatureSvg()
        assertEquals(200, bitmap.width)
        touch(pad, MotionEvent.ACTION_MOVE)
        touch(pad, MotionEvent.ACTION_UP)
        assertEquals(saved.events, pad.getSignature().events)
        assertEquals(svg, pad.getSignatureSvg())
        assertTrue(bitmap.sameAs(pad.getTransparentSignatureBitmap()))
        assertShadowHidden(pad)
    }

    private fun createPad(): SignaturePad {
        val context = RuntimeEnvironment.getApplication()
        val pad = SignaturePad(context, null)
        FrameLayout(context).addView(pad)
        pad.layout(0, 0, 200, 200)
        pad.setShadowIntensity(0.5f)
        return pad
    }

    private fun touch(pad: SignaturePad, action: Int, x: Float = 100f) {
        val event = MotionEvent.obtain(0L, 10L, action, x, 100f, 0)
        try {
            pad.onTouchEvent(event)
        } finally {
            event.recycle()
        }
    }

    private fun multiTouch(pad: SignaturePad, action: Int, ids: List<Int>, actionIndex: Int = 0) {
        val properties = ids.map { id ->
            MotionEvent.PointerProperties().apply {
                this.id = id
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }.toTypedArray()
        val coordinates = ids.map { id ->
            MotionEvent.PointerCoords().apply {
                x = 100f + id * 40f
                y = 100f
                pressure = 1f
                size = 1f
            }
        }.toTypedArray()
        val event = MotionEvent.obtain(
            0L, 20L, action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            ids.size, properties, coordinates, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
        try {
            pad.onTouchEvent(event)
        } finally {
            event.recycle()
        }
    }

    private fun render(pad: SignaturePad): ShadowCanvas {
        val canvas = ShadowCanvas()
        pad.draw(canvas)
        return canvas
    }

    private fun assertShadowVisible(pad: SignaturePad) {
        assertTrue(render(pad).shadowDraws > 0)
    }

    private fun assertShadowHidden(pad: SignaturePad) {
        assertEquals(0, render(pad).shadowDraws)
    }

    private class ShadowCanvas : Canvas() {
        var shadowDraws = 0
        override fun isHardwareAccelerated(): Boolean = true
        override fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) = Unit
        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
            if (paint.shader != null) shadowDraws++
        }
    }
}
