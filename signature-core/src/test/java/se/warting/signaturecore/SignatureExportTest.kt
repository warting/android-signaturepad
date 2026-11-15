package se.warting.signaturecore

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignatureExportTest {
    @Test
    fun pngAndBase64_preserveCroppedInkWithPaddingAndBackground() {
        val sdk = SignatureSDK().apply {
            configure(5, 5, Color.BLACK)
            initializeBitmap(100, 100)
            addEvent(Event(0L, MotionEvent.ACTION_DOWN, 0f, 20f))
            addEvent(Event(16L, MotionEvent.ACTION_MOVE, 30f, 20f))
            addEvent(Event(32L, MotionEvent.ACTION_MOVE, 60f, 20f))
            addEvent(Event(48L, MotionEvent.ACTION_UP, 80f, 20f))
        }
        val original = requireNotNull(sdk.getTransparentSignatureBitmap())
        val trimmed = requireNotNull(sdk.getTransparentSignatureBitmap(true))
        val options = SignatureExportOptions(true, 8, Color.WHITE, Color.RED)
        val bytes = requireNotNull(sdk.toPngByteArray(options))
        val png = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        assertEquals(trimmed.width + 16, png.width)
        assertEquals(trimmed.height + 16, png.height)
        assertEquals(Color.WHITE, png.getPixel(0, 0))
        assertEquals(Color.RED, png.getPixel(png.width / 2, png.height / 2))
        val base64 = requireNotNull(sdk.toBase64(options))
        assertFalse(base64.contains('\n'))
        val decoded = Base64.decode(base64, Base64.DEFAULT)
        assertTrue(bytes.contentEquals(decoded))
        assertTrue(original.sameAs(sdk.getTransparentSignatureBitmap()))
    }

    @Test
    fun emptyAndUninitializedSignatures_haveDefinedExports() {
        val sdk = SignatureSDK()
        assertNull(sdk.toPngByteArray())
        assertNull(sdk.toBase64())
        sdk.initializeBitmap(20, 30)
        val empty = requireNotNull(sdk.exportBitmap(SignatureExportOptions(true, 2)))
        assertEquals(24, empty.width)
        assertEquals(34, empty.height)
        assertEquals(Color.TRANSPARENT, empty.getPixel(2, 2))
        empty.recycle()
        assertTrue(sdk.toPngByteArray()!!.isNotEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativePadding_isRejected() {
        SignatureExportOptions(paddingPixels = -1)
    }
}
