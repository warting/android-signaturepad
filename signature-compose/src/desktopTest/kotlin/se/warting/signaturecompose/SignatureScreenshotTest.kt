package se.warting.signaturecompose

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import org.jetbrains.skia.Image
import se.warting.signatureengine.SignatureEvent
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SignatureScreenshotTest {
    @Test
    fun restoredSignatureMatchesBaseline() = runComposeUiTest {
        val state = SignaturePadState().apply {
            restore(listOf(
                SignatureEvent(0, 0, 30f, 90f),
                SignatureEvent(16, 2, 70f, 30f),
                SignatureEvent(32, 2, 100f, 130f),
                SignatureEvent(48, 2, 160f, 60f),
                SignatureEvent(64, 2, 220f, 90f),
                SignatureEvent(80, 1, 280f, 40f),
            ))
        }
        setContent {
            SignaturePad(Modifier.background(Color.White).testTag("pad"), state)
        }
        val bitmap = onNodeWithTag("pad").captureToImage()
        val output = File("build/screenshots/signature.png").apply { parentFile.mkdirs() }
        Image.makeFromBitmap(bitmap.asSkiaBitmap()).use { image ->
            image.encodeToData()!!.use { output.writeBytes(it.bytes) }
        }
        val baseline = File("src/desktopTest/reference/signature.png")
        if (System.getenv("UPDATE_SIGNATURE_SCREENSHOTS") == "true") {
            baseline.parentFile.mkdirs()
            output.copyTo(baseline, overwrite = true)
        }
        assertTrue(baseline.exists(), "Generate and review the baseline using UPDATE_SIGNATURE_SCREENSHOTS=true")
        val expected = ImageIO.read(baseline)
        val actual = ImageIO.read(output)
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)
        var different = 0
        for (y in 0 until actual.height) for (x in 0 until actual.width) {
            val reference = expected.getRGB(x, y)
            val rendered = actual.getRGB(x, y)
            if (reference != rendered) different++
            for (shift in listOf(0, 8, 16, 24)) {
                assertTrue(abs(((reference ushr shift) and 255) - ((rendered ushr shift) and 255)) <= 3,
                    "Pixel ($x, $y) differs beyond raster rounding; actual: ${output.absolutePath}")
            }
        }
        // Linux/macOS Skia differ at 306/51200 edge pixels by at most 3/255 per channel.
        assertTrue(different <= actual.width * actual.height / 100,
            "More than 1% of pixels differ; actual image: ${output.absolutePath}")
    }
}
