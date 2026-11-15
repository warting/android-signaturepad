package se.warting.signaturepad.app

import android.view.MotionEvent
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import se.warting.signaturecore.Event
import se.warting.signaturecore.ExperimentalSignatureApi
import se.warting.signaturecore.Signature
import se.warting.signaturepad.SignatureField
import se.warting.signaturepad.rememberSignaturePadState

@PreviewTest
@Preview(widthDp = 360, heightDp = 200, showBackground = true)
@Composable
fun EmptySignatureField() {
    MaterialTheme {
        Surface { SignatureField(Modifier.height(200.dp)) { Text("Sign here") } }
    }
}

@OptIn(ExperimentalSignatureApi::class)
@PreviewTest
@Preview(widthDp = 360, heightDp = 200, showBackground = true)
@Composable
fun SignedSignatureField() {
    val state = rememberSignaturePadState()
    val density = LocalDensity.current.density
    remember(state, density) {
        state.setSignature(Signature(1, listOf(
            Event(0L, MotionEvent.ACTION_DOWN, 30f, 100f),
            Event(16L, MotionEvent.ACTION_MOVE, 80f, 40f),
            Event(32L, MotionEvent.ACTION_MOVE, 100f, 140f),
            Event(48L, MotionEvent.ACTION_MOVE, 160f, 70f),
            Event(64L, MotionEvent.ACTION_MOVE, 220f, 100f),
            Event(80L, MotionEvent.ACTION_UP, 280f, 50f),
        ).map { it.copy(x = it.x * density, y = it.y * density) }))
    }
    MaterialTheme {
        Surface { SignatureField(Modifier.height(200.dp), state = state) { Text("Sign here") } }
    }
}
