package se.warting.signaturepad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import se.warting.signatureink.InkSignaturePad
import se.warting.signatureink.InkSignatureState

/** Optional low-latency renderer demo, separate from the existing default rendering examples. */
class InkSampleActivity : ComponentActivity() {
    private val model: InkSampleModel by viewModels()
    val inkState: InkSignatureState get() = model.state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().padding(20.dp)) {
                        Text("Jetpack Ink", style = MaterialTheme.typography.headlineMedium)
                        Text("Sign with your finger or a pressure-sensitive pen")
                        InkSignaturePad(
                            Modifier.fillMaxWidth().height(240.dp).background(Color.White).testTag("ink"),
                            state = inkState,
                        )
                        Row {
                            Button(onClick = inkState::undo, enabled = inkState.canUndo) { Text("Undo") }
                            Button(onClick = inkState::redo, enabled = inkState.canRedo) { Text("Redo") }
                            Button(onClick = inkState::clear, enabled = !inkState.isEmpty) { Text("Clear") }
                        }
                        Text(if (inkState.isDrawing) "Drawing…" else "${inkState.strokes.size} strokes")
                    }
                }
            }
        }
    }
}

class InkSampleModel : ViewModel() {
    val state = InkSignatureState()
}
