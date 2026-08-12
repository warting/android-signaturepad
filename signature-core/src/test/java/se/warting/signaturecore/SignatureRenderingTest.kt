package se.warting.signaturecore

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalSignatureApi::class)
class SignatureRenderingTest {

    @Test
    fun toSvg_emptySignature_emitsEmptyGroup() {
        val signature = Signature(versionCode = 1, events = emptyList())

        val svg = signature.toSvg(width = 100, height = 50)

        assertTrue("Expected SVG to start with xml declaration", svg.startsWith("<?xml"))
        assertFalse("Empty signature should produce no <path>", svg.contains("<path "))
    }

    @Test
    fun toSvg_withStroke_emitsPath() {
        val signature = Signature(versionCode = 1, events = sampleStroke())

        val svg = signature.toSvg(
            width = 200,
            height = 100,
            penColor = 0xFF112233.toInt(),
        )

        assertTrue("Expected at least one <path> in SVG output", svg.contains("<path "))
        assertTrue(svg.contains("stroke=\"#112233\""))
    }

    @Test
    fun toSvg_includesBackgroundWhenSpecified() {
        val signature = Signature(versionCode = 1, events = sampleStroke())

        val svg = signature.toSvg(
            width = 50,
            height = 50,
            backgroundColor = 0xFFFFFFFF.toInt(),
        )

        assertTrue("Expected <rect> background", svg.contains("<rect"))
        assertTrue(svg.contains("fill=\"#FFFFFF\""))
    }

    @Test
    fun configuredPointerShadow_isNotSerializedIntoSvg() {
        val sdk = SignatureSDK().apply {
            configure(
                minWidth = SignatureSDK.DEFAULT_ATTR_PEN_MIN_WIDTH_PX,
                maxWidth = SignatureSDK.DEFAULT_ATTR_PEN_MAX_WIDTH_PX,
                penColor = SignatureSDK.DEFAULT_ATTR_PEN_COLOR,
                velocityFilterWeight = SignatureSDK.DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT,
            )
            configureShadow(
                shadowColor = 0x80112233.toInt(),
                shadowIntensity = 0.5f,
            )
            restoreEvents(sampleStroke())
        }

        val svg = sdk.getSignatureSvg(width = 200, height = 100)

        assertFalse("Pointer shadow should not be baked into SVG", svg.contains("signature-shadow"))
        assertFalse(svg.contains("feDropShadow"))
        assertFalse(svg.contains("filter=\"url("))
    }

    @Test
    fun toSvg_isStableForSameInput() {
        val signature = Signature(versionCode = 1, events = sampleStroke())

        val first = signature.toSvg(width = 200, height = 100)
        val second = signature.toSvg(width = 200, height = 100)

        assertEquals("Repeated rendering must produce identical SVG", first, second)
    }

    @Suppress("MagicNumber")
    private fun sampleStroke(): List<Event> = listOf(
        Event(timestamp = 0L, action = MotionEvent.ACTION_DOWN, x = 10f, y = 10f),
        Event(timestamp = 16L, action = MotionEvent.ACTION_MOVE, x = 30f, y = 40f),
        Event(timestamp = 32L, action = MotionEvent.ACTION_MOVE, x = 60f, y = 60f),
        Event(timestamp = 48L, action = MotionEvent.ACTION_MOVE, x = 90f, y = 70f),
        Event(timestamp = 64L, action = MotionEvent.ACTION_UP, x = 120f, y = 80f),
    )
}
