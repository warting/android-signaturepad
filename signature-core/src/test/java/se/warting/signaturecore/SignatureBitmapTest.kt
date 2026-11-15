package se.warting.signaturecore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import se.warting.signaturecore.utils.SignedListener

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignatureBitmapTest {

    @Test
    fun redoMatchesLegacyRestoreUsingCurrentGlobalPenSettings() {
        val sdk = signature(horizontal = true, penWidth = 5)
        val events = sdk.getEvents()
        sdk.undo()
        sdk.configure(12, 12, Color.RED, 0.9f)
        sdk.redo()
        val legacyRestored = SignatureSDK().apply {
            configure(12, 12, Color.RED, 0.9f)
            initializeBitmap(50, 50)
            restoreEvents(events)
        }
        val actual = requireNotNull(sdk.getTransparentSignatureBitmap())
        val expected = requireNotNull(legacyRestored.getTransparentSignatureBitmap())
        try { assertTrue(expected.sameAs(actual)) } finally {
            expected.recycle()
            actual.recycle()
        }
        assertEquals(events, sdk.getEvents())
    }

    @Test
    fun resizeDuringStroke_preservesInputAndRenderingWithoutExtraCallbacks() {
        val sdk = SignatureSDK().apply {
            configure(5, 5, Color.BLACK, 0.9f)
            initializeBitmap(50, 50)
        }
        var callbacks = 0
        sdk.setOnSignedListener(object : SignedListener {
            override fun onStartSigning() { callbacks++ }
            override fun onSigning() { callbacks++ }
            override fun onSigned() { callbacks++ }
            override fun onClear() { callbacks++ }
        })
        sdk.input.start(7L, 10f, 20f, 0.5f, 0L)
        sdk.input.move(7L, 30f, 20f, 0.5f, 16L)
        val events = sdk.getEvents()
        val svg = sdk.getSignatureSvg(100, 80)
        sdk.initializeBitmap(100, 80)
        sdk.initializeBitmap(100, 80)
        sdk.initializeBitmap(0, 0)
        assertEquals(2, callbacks)
        assertEquals(events, sdk.getEvents())
        assertEquals(svg, sdk.getSignatureSvg(100, 80))
        assertTrue(sdk.input.isActive(7L))
        assertEquals(0.5f, sdk.input.pressure)
        sdk.input.move(7L, 60f, 20f, 0.5f, 32L)
        sdk.input.move(7L, 80f, 20f, 0.5f, 48L)
        sdk.input.end(7L, 90f, 20f, 64L)
        val expected = SignatureSDK().apply {
            configure(5, 5, Color.BLACK, 0.9f)
            initializeBitmap(100, 80)
            restoreEvents(sdk.getEvents())
        }
        assertEquals(5, callbacks)
        assertTrue(expected.getTransparentSignatureBitmap()!!.sameAs(sdk.getTransparentSignatureBitmap()))
        assertEquals(expected.getSignatureSvg(100, 80), sdk.getSignatureSvg(100, 80))
    }

    @Test
    fun resizeAfterPenChange_copiesInkWithoutRecoloringOrScaling() {
        val sdk = signature(horizontal = true, penWidth = 5)
        val before = requireNotNull(sdk.getTransparentSignatureBitmap())
        val events = sdk.getEvents()
        val svg = sdk.getSignatureSvg(100, 80)
        sdk.configure(20, 20, Color.RED)
        sdk.initializeBitmap(100, 80)
        val enlarged = requireNotNull(sdk.getTransparentSignatureBitmap())
        assertEquals(100, enlarged.width)
        assertEquals(80, enlarged.height)
        assertTrue(before.sameAs(Bitmap.createBitmap(enlarged, 0, 0, 50, 50)))
        sdk.initializeBitmap(25, 30)
        val reduced = requireNotNull(sdk.getTransparentSignatureBitmap())
        assertTrue(Bitmap.createBitmap(before, 0, 0, 25, 30).sameAs(reduced))
        assertEquals(events, sdk.getEvents())
        assertEquals(svg, sdk.getSignatureSvg(100, 80))
        assertTrue(sdk.canUndo())
        assertFalse(sdk.isEmpty)
    }

    @Test
    fun restoreSignature_beforeAndAfterMeasurement_hasIdenticalPixelsAndSingleCallbackSequence() {
        val source = signature(horizontal = true, penWidth = 5)
        val events = source.getEvents()
        for (measured in listOf(false, true)) {
            val restored = SignatureSDK().apply { configure(5, 5, Color.BLACK) }
            val callbacks = mutableListOf<String>()
            restored.setOnSignedListener(object : SignedListener {
                override fun onStartSigning() { callbacks.add("down") }
                override fun onSigning() { callbacks.add("move") }
                override fun onSigned() { callbacks.add("up") }
                override fun onClear() { callbacks.add("clear") }
            })
            restored.input.start(1L, 1f, 1f, 1f, 0L)
            callbacks.clear()
            restored.restoreSignature(events, if (measured) 50 else 0, if (measured) 50 else 0)
            assertFalse(restored.input.isActive(1L))
            restored.initializeBitmap(50, 50)
            restored.initializeBitmap(50, 50)
            assertEquals(listOf("clear", "down", "move", "move", "up"), callbacks)
            assertEquals(events, restored.getEvents())
            assertTrue(source.getTransparentSignatureBitmap()!!.sameAs(restored.getTransparentSignatureBitmap()))
            assertEquals(source.getSignatureSvg(50, 50), restored.getSignatureSvg(50, 50))
        }
    }

    @Test
    fun clear_afterResize_recreatesBitmapAtCurrentSizeAndReplaysNewStroke() {
        val sdk = signature(horizontal = true, penWidth = 5)

        sdk.clear()
        assertFalse(sdk.hasBitmap())
        // A View may receive input before its next draw initializes the new bitmap.
        sdk.addEvent(Event(0L, MotionEvent.ACTION_DOWN, 60.5f, 60.5f))
        sdk.addEvent(Event(16L, MotionEvent.ACTION_MOVE, 70.5f, 60.5f))
        sdk.addEvent(Event(32L, MotionEvent.ACTION_MOVE, 80.5f, 60.5f))
        sdk.addEvent(Event(48L, MotionEvent.ACTION_UP, 90.5f, 60.5f))
        sdk.initializeBitmap(100, 80)

        val exported = requireNotNull(sdk.getTransparentSignatureBitmap())
        assertEquals(100, exported.width)
        assertEquals(80, exported.height)
        assertTrue(Color.alpha(exported.getPixel(65, 60)) > 0)
        assertEquals(Color.TRANSPARENT, exported.getPixel(15, 20))
    }

    @Test
    fun initializeBitmap_replaysEventsRestoredBeforeMeasurement() {
        val source = signature(horizontal = true, penWidth = 5)
        val restored = SignatureSDK().apply {
            configure(minWidth = 5, maxWidth = 5, penColor = Color.BLACK)
            restoreEvents(source.getEvents())
        }
        var callbacks = 0
        restored.setOnSignedListener(object : SignedListener {
            override fun onStartSigning() { callbacks++ }
            override fun onSigning() { callbacks++ }
            override fun onSigned() { callbacks++ }
            override fun onClear() { callbacks++ }
        })

        restored.initializeBitmap(50, 50)

        val expected = requireNotNull(source.getTransparentSignatureBitmap())
        val actual = requireNotNull(restored.getTransparentSignatureBitmap())
        assertTrue(expected.sameAs(actual))
        assertEquals(source.getSignatureSvg(50, 50), restored.getSignatureSvg(50, 50))
        assertEquals(0, callbacks)
    }

    @Test
    fun draw_afterClearAndNewStroke_doesNotReplayOrCrash() {
        val sdk = signature(horizontal = true, penWidth = 5)
        val events = sdk.getEvents()
        sdk.clear()
        sdk.initializeBitmap(50, 50)
        events.forEach(sdk::addEvent)
        val expectedSvg = sdk.getSignatureSvg(50, 50)

        sdk.drawSignature(Canvas(Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)))

        assertEquals(expectedSvg, sdk.getSignatureSvg(50, 50))
    }

    @Test
    fun trim_includesEveryVisiblePixel() {
        val sdk = signature(horizontal = true, penWidth = 5)
        val full = requireNotNull(sdk.getTransparentSignatureBitmap())
        val trimmed = requireNotNull(sdk.getTransparentSignatureBitmap(trimBlankSpace = true))
        val pixels = buildList {
            for (y in 0 until full.height) {
                for (x in 0 until full.width) {
                    if (Color.alpha(full.getPixel(x, y)) > 0) add(x to y)
                }
            }
        }
        assertTrue(pixels.isNotEmpty())
        val left = pixels.minOf { it.first }
        val top = pixels.minOf { it.second }
        assertEquals(pixels.maxOf { it.first } - left + 1, trimmed.width)
        assertEquals(pixels.maxOf { it.second } - top + 1, trimmed.height)
        for ((x, y) in pixels) {
            assertEquals(full.getPixel(x, y), trimmed.getPixel(x - left, y - top))
        }
    }

    @Test
    fun trim_onePixelHighStroke_doesNotCrash() {
        val sdk = signature(horizontal = true, penWidth = 1)

        val trimmed = requireNotNull(sdk.getTransparentSignatureBitmap(trimBlankSpace = true))

        assertEquals(1, trimmed.height)
        assertTrue(trimmed.width > 1)
    }

    @Test
    fun trim_onePixelWideStroke_doesNotCrash() {
        val sdk = signature(horizontal = false, penWidth = 1)

        val trimmed = requireNotNull(sdk.getTransparentSignatureBitmap(trimBlankSpace = true))

        assertEquals(1, trimmed.width)
        assertTrue(trimmed.height > 1)
    }

    @Test
    fun trim_emptyBitmap_returnsIndependentCopy() {
        val sdk = SignatureSDK().apply { initializeBitmap(50, 50) }
        val first = requireNotNull(sdk.getTransparentSignatureBitmap(trimBlankSpace = true))
        val second = requireNotNull(sdk.getTransparentSignatureBitmap(trimBlankSpace = true))

        assertEquals(50, first.width)
        assertEquals(50, first.height)
        assertNotSame(first, second)
        first.recycle()
        assertTrue(sdk.getTransparentSignatureBitmap() != null)
    }

    @Test
    fun trim_fullCanvas_returnsIndependentCopy() {
        val sdk = signature(horizontal = true, penWidth = 100)
        val exported = requireNotNull(sdk.getTransparentSignatureBitmap(trimBlankSpace = true))
        assertEquals(50, exported.width)
        assertEquals(50, exported.height)

        exported.recycle()

        assertTrue(sdk.getTransparentSignatureBitmap() != null)
    }

    private fun signature(horizontal: Boolean, penWidth: Int) = SignatureSDK().apply {
        configure(minWidth = penWidth, maxWidth = penWidth, penColor = Color.BLACK)
        initializeBitmap(50, 50)
        val coordinates = listOf(10.5f, 20.5f, 30.5f, 40.5f)
        coordinates.forEachIndexed { index, coordinate ->
            val action = when (index) {
                0 -> MotionEvent.ACTION_DOWN
                coordinates.lastIndex -> MotionEvent.ACTION_UP
                else -> MotionEvent.ACTION_MOVE
            }
            addEvent(
                Event(
                    index * 16L,
                    action,
                    if (horizontal) coordinate else 20.5f,
                    if (horizontal) 20.5f else coordinate,
                ),
            )
        }
    }
}
