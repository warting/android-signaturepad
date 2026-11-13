package se.warting.signaturecore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PointerShadowTest {
    @Test
    fun shadow_isOptInAndDrawsOnlyWhilePointerIsActive() {
        val sdk = SignatureSDK()
        assertTrue(render(sdk).isBlank())
        sdk.configureShadow(shadowIntensity = 0.5f)
        assertFalse(render(sdk).isBlank())
        assertTrue(render(sdk, pressure = 0f).isBlank())
    }

    @Test
    fun drawingShadow_doesNotChangeAnyExport() {
        val sdk = SignatureSDK().apply {
            configure(minWidth = 3, maxWidth = 3, penColor = Color.BLACK)
            initializeBitmap(200, 200)
            addEvent(Event(0L, MotionEvent.ACTION_DOWN, 10f, 10f))
            addEvent(Event(16L, MotionEvent.ACTION_MOVE, 50f, 50f))
            addEvent(Event(32L, MotionEvent.ACTION_UP, 100f, 80f))
        }
        val svg = sdk.getSignatureSvg(200, 200)
        val events = sdk.getEvents()
        val transparent = requireNotNull(sdk.getTransparentSignatureBitmap())
        val opaque = requireNotNull(sdk.getSignatureBitmap())
        val trimmed = requireNotNull(sdk.getTransparentSignatureBitmap(true))
        sdk.configureShadow(shadowColor = Color.RED, shadowIntensity = 0.8f)
        sdk.configureShadowAngle(120f)
        assertFalse(render(sdk).isBlank())
        assertEquals(svg, sdk.getSignatureSvg(200, 200))
        assertEquals(events, sdk.getEvents())
        assertTrue(transparent.sameAs(sdk.getTransparentSignatureBitmap()))
        assertTrue(opaque.sameAs(sdk.getSignatureBitmap()))
        assertTrue(trimmed.sameAs(sdk.getTransparentSignatureBitmap(true)))
    }

    @Test
    fun nonFiniteInputs_doNotCrashOrDrawInvalidShadow() {
        val sdk = SignatureSDK()
        sdk.configureShadow(shadowIntensity = Float.NaN)
        assertTrue(render(sdk).isBlank())
        sdk.configureShadow(shadowIntensity = 1f)
        sdk.configureShadowAngle(Float.POSITIVE_INFINITY)
        assertTrue(render(sdk, pressure = Float.NaN).isBlank())
        assertTrue(render(sdk, x = Float.NaN).isBlank())
        assertFalse(render(sdk).isBlank())
    }

    @Test
    @Config(sdk = [23, 32])
    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    fun olderAndroid_shadowIsANoOp() {
        val sdk = SignatureSDK().apply { configureShadow(shadowIntensity = 1f) }
        assertTrue(render(sdk).isBlank())
    }

    @Test
    fun softwareCanvas_isANoOpEvenWhenEnabled() {
        val sdk = SignatureSDK().apply { configureShadow(shadowIntensity = 1f) }
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val blank = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        sdk.drawPointerShadow(Canvas(bitmap), 200, 200, 100f, 100f, 1f)
        assertTrue(bitmap.sameAs(blank))
    }

    private fun render(sdk: SignatureSDK, pressure: Float = 1f, x: Float = 100f): ShadowCanvas {
        val canvas = ShadowCanvas()
        sdk.drawPointerShadow(canvas, 200, 200, x, 100f, pressure)
        return canvas
    }

    private class ShadowCanvas : Canvas() {
        private var shadowDraws = 0
        override fun isHardwareAccelerated(): Boolean = true
        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
            if (paint.shader != null) shadowDraws++
        }
        fun isBlank(): Boolean = shadowDraws == 0
    }
}
