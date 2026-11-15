package se.warting.signaturepad.app

import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.util.Base64
import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import se.warting.signaturecore.Event
import se.warting.signaturecore.SignatureExportOptions
import se.warting.signaturecore.SignatureSDK
import se.warting.signaturecore.toBase64

@PreviewTest
@Preview(widthDp = 360, heightDp = 240, showBackground = true)
@Composable
fun FullCanvasPng() = ExportPreview("Full canvas PNG", SignatureExportOptions(backgroundColor = AndroidColor.WHITE))

@PreviewTest
@Preview(widthDp = 360, heightDp = 240, showBackground = true)
@Composable
fun CroppedPngWithMargin() = ExportPreview("Cropped PNG + 12 px margin",
    SignatureExportOptions(trimBlankSpace = true, paddingPixels = 12, backgroundColor = AndroidColor.WHITE))

@PreviewTest
@Preview(widthDp = 360, heightDp = 240, showBackground = true)
@Composable
fun ColoredPngWithMargin() = ExportPreview("Cropped PNG + blue ink",
    SignatureExportOptions(trimBlankSpace = true, paddingPixels = 12,
        backgroundColor = AndroidColor.rgb(235, 245, 255), penColor = AndroidColor.rgb(20, 65, 170)))

@Composable
private fun ExportPreview(label: String, options: SignatureExportOptions) {
    val bitmap = remember(options) {
        val sdk = SignatureSDK().apply {
            configure(minWidth = 3, maxWidth = 7, penColor = AndroidColor.BLACK, velocityFilterWeight = 0.9f)
            restoreSignature(listOf(
                Event(0, MotionEvent.ACTION_DOWN, 30f, 90f),
                Event(16, MotionEvent.ACTION_MOVE, 70f, 30f),
                Event(32, MotionEvent.ACTION_MOVE, 100f, 130f),
                Event(48, MotionEvent.ACTION_MOVE, 160f, 60f),
                Event(64, MotionEvent.ACTION_MOVE, 220f, 90f),
                Event(80, MotionEvent.ACTION_UP, 280f, 40f),
            ), 320, 180)
        }
        // Exercise PNG encoding and Base64 transport, then display the decoded export.
        val bytes = Base64.decode(checkNotNull(sdk.toBase64(options)), Base64.NO_WRAP)
        checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
    }
    DisposableEffect(bitmap) { onDispose { bitmap.recycle() } }
    MaterialTheme {
        Surface(color = Color(0xffeeeeee)) {
            Column(Modifier.padding(12.dp)) {
                Text(label)
                Text("${bitmap.width} × ${bitmap.height} px")
                Image(bitmap.asImageBitmap(), contentDescription = label,
                    modifier = Modifier.size(bitmap.width.dp, bitmap.height.dp))
            }
        }
    }
}
