package se.warting.signaturesample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import se.warting.signaturecompose.SignaturePad
import se.warting.signaturecompose.rememberSignaturePadState
import se.warting.signatureengine.SignatureEvent

@Composable
fun SignatureDemo() {
    MaterialTheme {
        Surface {
            val state = rememberSignaturePadState()
            var saved by remember { mutableStateOf<List<SignatureEvent>>(emptyList()) }
            var svg by remember { mutableStateOf("") }
            var padSize by remember { mutableStateOf(IntSize.Zero) }
            val density = LocalDensity.current.density
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("Signature Pad", style = MaterialTheme.typography.headlineMedium)
                Text("One signature engine · Android · iOS · Desktop · Web")
                SignaturePad(
                    Modifier.fillMaxWidth().height(220.dp).background(Color.White).testTag("signature").onSizeChanged { padSize = it },
                    state = state,
                )
                Row {
                    Button(onClick = state::undo, enabled = state.canUndo) { Text("Undo") }
                    Button(onClick = state::redo, enabled = state.canRedo) { Text("Redo") }
                    Button(onClick = state::clear, enabled = !state.isEmpty) { Text("Clear") }
                }
                Row {
                    Button(onClick = { saved = state.events }, enabled = !state.isEmpty) { Text("Save") }
                    Button(onClick = { state.restore(saved) }, enabled = saved.isNotEmpty()) { Text("Restore") }
                    Button(onClick = { svg = state.toSvg(ceil(padSize.width / density).toInt(), ceil(padSize.height / density).toInt()) }, enabled = !state.isEmpty) { Text("SVG") }
                }
                Text(if (state.isEmpty) "Draw your signature above" else "${state.events.size} input samples")
                if (svg.isNotEmpty()) Text(svg, modifier = Modifier.testTag("svg-output"))
            }
        }
    }
}
