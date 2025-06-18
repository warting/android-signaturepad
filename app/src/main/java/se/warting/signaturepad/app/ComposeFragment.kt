package se.warting.signaturepad.app

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.warting.signaturepad.SignaturePadAdapter
import se.warting.signaturepad.SignaturePadView

private const val SIGNATURE_PAD_HEIGHT = 120

@Suppress("LongMethod")
@Composable
fun ComposeSample() {

    val mutableSvg = remember { mutableStateOf("") }
    val savedBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    Column(
        modifier = Modifier
    ) {
        var signaturePadAdapter: SignaturePadAdapter? = null
        var penColor by remember { mutableStateOf(Color.Black) }
        var imageBackgroundColor by remember { mutableStateOf<Color?>(null) }
        var overrideStrokeColor by remember { mutableStateOf<Color?>(null) }

        Box(
            modifier = Modifier
                .height(SIGNATURE_PAD_HEIGHT.dp)
                .fillMaxWidth()
                .border(width = 2.dp, color = Color.Red)
        ) {
            SignaturePadView(
                onReady = {
                    signaturePadAdapter = it
                },
                penColor = penColor,

                onStartSigning = {
                    Log.d("SignedListener", "OnStartSigning")
                },
                onSigning = {
                    Log.d("SignedListener", "onSigning")
                },
                onSigned = {
                    Log.d("SignedListener", "onSigned")
                },
                onClear = {
                    Log.d(
                        "ComposeFragment", "onClear isEmpty:"
                                + signaturePadAdapter?.isEmpty
                    )
                },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            Text("Pen color:")
            Row {
                Button(onClick = {
                    penColor = Color.Red
                }) {
                    Text("Red")
                }
                Button(onClick = {
                    penColor = Color.Black
                }) {
                    Text("Black")
                }
                Button(onClick = {
                    penColor = Color.White
                }) {
                    Text("White")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Signature image background color:")
            Row {
                Button(onClick = {
                    imageBackgroundColor = Color.Red
                }) {
                    Text("Red")
                }
                Button(onClick = {
                    imageBackgroundColor = Color.Green
                }) {
                    Text("Green")
                }
                Button(onClick = {
                    imageBackgroundColor = Color.Blue
                }) {
                    Text("Blue")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Signature image pen color:")
            Row {
                Button(onClick = {
                    overrideStrokeColor = Color.Red
                }) {
                    Text("Red")
                }
                Button(onClick = {
                    overrideStrokeColor = Color.Black
                }) {
                    Text("Black")
                }
                Button(onClick = {
                    overrideStrokeColor = Color.White
                }) {
                    Text("White")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    onClick = {
                    mutableSvg.value = signaturePadAdapter?.getSignatureSvg().orEmpty()
                    val signatureBitmap = signaturePadAdapter?.getSignatureBitmap(
                        backgroundColor = imageBackgroundColor?.toArgb() ?: Color.White.toArgb(),
                        overrideStrokeColor = overrideStrokeColor?.toArgb()
                    )
                    savedBitmap.value = signatureBitmap?.asImageBitmap()
                }) {
                    Text("Save")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                    mutableSvg.value = ""
                    signaturePadAdapter?.clear()
                }) {
                    Text("Clear")
                }
            }
            savedBitmap.value?.let { img ->
                Image(
                    bitmap = img,
                    contentDescription = "Saved signature",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SIGNATURE_PAD_HEIGHT.dp)
                        .border(width = 1.dp, color = Color.Gray)
                )
            }
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(text = "SVG: " + mutableSvg.value)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        SignaturePadView()
    }
}
