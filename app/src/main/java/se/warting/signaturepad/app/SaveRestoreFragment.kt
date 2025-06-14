package se.warting.signaturepad.app

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import se.warting.signaturecore.Event
import se.warting.signaturecore.ExperimentalSignatureApi
import se.warting.signaturecore.Signature
import se.warting.signaturepad.SignaturePadAdapter
import se.warting.signaturepad.SignaturePadView

private const val SIGNATURE_PAD_HEIGHT = 120

/**
 * Sample demonstrates saving and restoring signature data
 */
@Suppress("LongMethod")
@Composable
fun SaveRestoreSample() {
    Column {
        val mutableSvg = remember { mutableStateOf("") }

        var signaturePadAdapter: SignaturePadAdapter? = null

        Box(
            modifier = Modifier
                .height(SIGNATURE_PAD_HEIGHT.dp)
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = Color.Red,
                )
        ) {

            SignaturePadView(
                onReady = {
                    signaturePadAdapter = it
                },
                onStartSigning = {
                    if (BuildConfig.DEBUG) {
                        Log.d("SaveRestoreFragment", "onStartSigning")
                    }
                },
                onSigning = {
                    if (BuildConfig.DEBUG) {
                        Log.d("SaveRestoreFragment", "onStartSigning")
                    }
                },
                onSigned = {
                    if (BuildConfig.DEBUG) {
                        Log.d("SaveRestoreFragment", "onSigned")
                    }
                },
                onClear = {
                    if (BuildConfig.DEBUG) {
                        Log.d(
                            "SaveRestoreFragment",
                            "onClear isEmpty:" + signaturePadAdapter?.isEmpty
                        )
                    }
                },
            )
        }
        Row {
            Button(onClick = {
                mutableSvg.value =
                    signaturePadAdapter?.getSignature()?.serialize() ?: ""
                signaturePadAdapter?.clear()
            }) {
                Text("Save")
            }

            Button(onClick = {
                signaturePadAdapter?.setSignature(mutableSvg.value.deserialize())
                mutableSvg.value = ""
            }) {
                Text("Restore")
            }
        }

        Text(text = "Signature data: " + mutableSvg.value)
    }
}

@ExperimentalSignatureApi
private fun Signature.serialize(): String {
    // save version!
    return this.events.joinToString(separator = "\n") {
        ("${it.timestamp},${it.action},${it.x},${it.y}")
    }
}

@ExperimentalSignatureApi
@SuppressWarnings("MagicNumber")
private fun String.deserialize(): Signature {
    // restore version!
    val events = this.split("\n").map {
        val parts = it.split(",")
        Event(parts[0].toLong(), parts[1].toInt(), parts[2].toFloat(), parts[3].toFloat())
    }
    return Signature(
        versionCode = 0,
        events = events
    )
}
