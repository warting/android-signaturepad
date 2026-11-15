package se.warting.signaturepad.app

import android.view.MotionEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
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
import se.warting.signaturepad.SignaturePadView
import se.warting.signaturepad.rememberSignaturePadState

@PreviewTest
@Preview(widthDp = 360, heightDp = 220, showBackground = true)
@Composable
fun AfterUndo() = HistoryPreview(redo = false)

@PreviewTest
@Preview(widthDp = 360, heightDp = 220, showBackground = true)
@Composable
fun AfterRedo() = HistoryPreview(redo = true)

@OptIn(ExperimentalSignatureApi::class)
@Composable
private fun HistoryPreview(redo: Boolean) {
    val state = rememberSignaturePadState()
    val density = LocalDensity.current.density
    remember(state, density, redo) {
        val events = listOf(
            Event(0, MotionEvent.ACTION_DOWN, 30f, 90f),
            Event(16, MotionEvent.ACTION_MOVE, 70f, 30f),
            Event(32, MotionEvent.ACTION_MOVE, 100f, 130f),
            Event(48, MotionEvent.ACTION_MOVE, 160f, 60f),
            Event(64, MotionEvent.ACTION_UP, 220f, 90f),
            Event(100, MotionEvent.ACTION_DOWN, 60f, 145f),
            Event(116, MotionEvent.ACTION_MOVE, 160f, 140f),
            Event(132, MotionEvent.ACTION_MOVE, 250f, 125f),
            Event(148, MotionEvent.ACTION_UP, 300f, 110f),
        ).map { it.copy(x = it.x * density, y = it.y * density) }
        state.setSignature(Signature(1, events))
        state.undo()
        if (redo) state.redo()
    }
    MaterialTheme {
        Surface {
            Column {
                SignaturePadView(Modifier.fillMaxWidth().height(160.dp), state = state)
                Text(if (redo) "Redo: underline restored" else "Undo: underline removed")
                Text("Can redo: ${state.canRedo()}")
            }
        }
    }
}
