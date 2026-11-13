package se.warting.signatureview.views

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PointerShadowViewTest {
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
        assertShadowHidden(pad)
        touch(pad, MotionEvent.ACTION_DOWN)
        assertShadowVisible(pad)
        pad.isEnabled = false
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

    private fun touch(pad: SignaturePad, action: Int) {
        val event = MotionEvent.obtain(0L, 10L, action, 100f, 100f, 0)
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
