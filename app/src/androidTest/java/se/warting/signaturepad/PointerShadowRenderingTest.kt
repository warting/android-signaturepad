package se.warting.signaturepad

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import se.warting.signaturepad.app.MainActivity
import se.warting.signatureview.views.SignaturePad

/** Exercises actual GPU rendering; JVM tests separately cover older API levels and exports. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class PointerShadowRenderingTest {
    @Test
    fun coloredShadow_isVisibleOnGpuAndDisappearsOnCancelAndClear() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var pad: SignaturePad
            scenario.onActivity { activity ->
                pad = SignaturePad(activity, null).apply {
                    setBackgroundColor(Color.WHITE)
                    setShadowColor(Color.BLUE)
                }
                val root = FrameLayout(activity)
                root.addView(pad, FrameLayout.LayoutParams(400, 400, Gravity.CENTER))
                activity.setContentView(root)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertTrue(pad.isHardwareAccelerated)
                touch(pad, MotionEvent.ACTION_DOWN)
            }
            // Activity/window entrance animations can still be running after main-thread idle.
            // Wait for the known white surface before establishing the pixel baseline.
            var withoutShadow = capture(pad)
            val deadline = SystemClock.uptimeMillis() + 5_000
            while (withoutShadow.getPixel(0, 0) != Color.WHITE && SystemClock.uptimeMillis() < deadline) {
                withoutShadow = capture(pad)
            }
            assertTrue("Test surface should finish entering", withoutShadow.getPixel(0, 0) == Color.WHITE)
            scenario.onActivity { pad.setShadowIntensity(0.5f) }
            val shadow = capture(pad)
            assertFalse("GPU shadow should change visible pixels", withoutShadow.sameAs(shadow))
            assertTrue("Shadow should contain blue pixels", hasBluePixels(shadow))
            scenario.onActivity { touch(pad, MotionEvent.ACTION_CANCEL) }
            val cancelled = capture(pad)
            assertFalse("Cancel should remove the blue overlay", hasBluePixels(cancelled))
            assertTrue("Cancel should preserve the signature", withoutShadow.sameAs(cancelled))
            scenario.onActivity {
                touch(pad, MotionEvent.ACTION_DOWN)
                pad.clear()
            }
            val cleared = capture(pad)
            scenario.onActivity { pad.setShadowIntensity(0f) }
            assertTrue("Clear should remove the overlay", cleared.sameAs(capture(pad)))
        }
    }

    private fun touch(pad: SignaturePad, action: Int) {
        val time = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(time, time, action, 200f, 200f, 0)
        try {
            pad.onTouchEvent(event)
        } finally {
            event.recycle()
        }
    }

    private fun capture(pad: SignaturePad): Bitmap {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // Let the GPU submit the frame before taking a screenshot of the actual display.
        SystemClock.sleep(250)
        val location = IntArray(2)
        instrumentation.runOnMainSync { pad.getLocationOnScreen(location) }
        val screen = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        return Bitmap.createBitmap(screen, location[0], location[1], pad.width, pad.height)
    }

    private fun hasBluePixels(bitmap: Bitmap): Boolean {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.any { Color.blue(it) > Color.red(it) + 10 }
    }
}
