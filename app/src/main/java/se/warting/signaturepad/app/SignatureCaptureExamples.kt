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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import se.warting.signaturecore.ExperimentalSignatureApi
import se.warting.signaturecore.Signature
import se.warting.signaturecore.toBitmap
import kotlin.math.roundToInt
import se.warting.signaturepad.SignatureField
import se.warting.signaturepad.SignaturePadState
import se.warting.signaturepad.rememberSignaturePadState

/** Each capture session owns its state; cancel leaves the saved preview untouched. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSignatureApi::class)
@Composable
fun SignatureCaptureExamples() {
    var dialog by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    var saved by rememberSaveable { mutableStateOf<Signature?>(null) }
    var savedWidth by rememberSaveable { mutableStateOf(1) }
    var savedHeight by rememberSaveable { mutableStateOf(1) }
    var savedDensity by rememberSaveable { mutableStateOf(1f) }
    val density = LocalDensity.current.density
    val preview = remember(saved, savedWidth, savedHeight, savedDensity) {
        saved?.toBitmap(
            savedWidth, savedHeight,
            penMinWidth = (3 * savedDensity).roundToInt(),
            penMaxWidth = (7 * savedDensity).roundToInt(),
        )?.asImageBitmap()
    }
    fun save(state: SignaturePadState) {
        val bitmap = state.getSignatureBitmap()
        savedWidth = bitmap.width
        savedHeight = bitmap.height
        bitmap.recycle()
        savedDensity = density
        saved = state.getSignature()
    }
    Column {
        Row {
            Button(onClick = { dialog = true }) { Text("Sign in dialog") }
            Button(onClick = { sheet = true }) { Text("Sign in sheet") }
        }
        preview?.let { Image(it, "Saved signature", Modifier.fillMaxWidth().height(80.dp)) }
    }
    if (dialog) {
        val state = rememberSignaturePadState()
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("Your signature") },
            text = { CaptureField(state) },
            confirmButton = {
                TextButton(enabled = !state.isEmpty, onClick = {
                    save(state)
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
                        save(state)
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
        Surface(color = Color.White, contentColor = Color.Black) {
            SignatureField(modifier = Modifier.testTag("capture-field"), state = state) {
                Text("Sign here")
            }
        }
        TextButton(onClick = { state.clear() }, enabled = !state.isEmpty) { Text("Clear") }
    }
}
