package se.warting.signaturepad.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import se.warting.signaturepad.SignatureField
import se.warting.signaturepad.SignaturePadState
import se.warting.signaturepad.rememberSignaturePadState

/** Each capture session owns its state; cancel leaves the saved preview untouched. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureCaptureExamples() {
    var dialog by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<ImageBitmap?>(null) }
    Column {
        Row {
            Button(onClick = { dialog = true }) { Text("Sign in dialog") }
            Button(onClick = { sheet = true }) { Text("Sign in sheet") }
        }
        saved?.let { Image(it, "Saved signature", Modifier.fillMaxWidth().height(80.dp)) }
    }
    if (dialog) {
        val state = rememberSignaturePadState()
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("Your signature") },
            text = { CaptureField(state) },
            confirmButton = {
                TextButton(enabled = !state.isEmpty, onClick = {
                    saved = state.getSignatureBitmap().asImageBitmap()
                    dialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { dialog = false }) { Text("Cancel") } },
        )
    }
    if (sheet) {
        val state = rememberSignaturePadState()
        ModalBottomSheet(onDismissRequest = { sheet = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("Your signature")
                CaptureField(state)
                Row {
                    TextButton(onClick = { sheet = false }) { Text("Cancel") }
                    Button(enabled = !state.isEmpty, onClick = {
                        saved = state.getSignatureBitmap().asImageBitmap()
                        sheet = false
                    }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun CaptureField(state: SignaturePadState) {
    Column {
        SignatureField(modifier = Modifier.testTag("capture-field"), state = state) {
            Text("Sign here")
        }
        TextButton(onClick = { state.clear() }, enabled = !state.isEmpty) { Text("Clear") }
    }
}
