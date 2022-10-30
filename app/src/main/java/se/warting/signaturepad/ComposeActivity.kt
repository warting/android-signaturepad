package se.warting.signaturepad

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


private const val SIGNATURE_PAD_HEIGHT = 120

@Suppress("LongMethod")
class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    val mutableSvg = remember { mutableStateOf("") }
                    val mutableBitmap: MutableState<Bitmap?> = remember { mutableStateOf(null) }
                    Column {

                        var signaturePadAdapter: SignaturePadAdapter? = null
                        val penColor = remember { mutableStateOf(Color.Black) }

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
                                penColor = penColor.value,
                                onSigned = {
                                    if (BuildConfig.DEBUG) {
                                        Log.d("ComposeActivity", "onSigned")
                                    }
                                },
                                onClear = {
                                    if (BuildConfig.DEBUG) {
                                        Log.d(
                                            "ComposeActivity",
                                            "onClear isEmpty:" + signaturePadAdapter?.isEmpty
                                        )
                                    }
                                },
                                onStartSigning = {
                                    if (BuildConfig.DEBUG) {
                                        Log.d("ComposeActivity", "onStartSigning")
                                    }
                                })
                        }
                        Row {
                            Button(onClick = {
                                mutableSvg.value = signaturePadAdapter?.getSignatureSvg() ?: ""

                                var rawBitmap = signaturePadAdapter?.getTransparentSignatureBitmap(
                                    trimBlankSpace = true
                                )!!

                                 rawBitmap = ConvertTransparentBackgroundToWhite(rawBitmap);

                                val outputStream = ByteArrayOutputStream()

                                rawBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

                                rawBitmap.recycle()

                                val bitmap2 = BitmapFactory.decodeStream(ByteArrayInputStream(
                                    outputStream.toByteArray()
                                )) // no triangles

                                mutableBitmap.value = bitmap2
                            }) {
                                Text("Save")
                            }

                            Button(onClick = {
                                mutableSvg.value = ""
                                signaturePadAdapter?.clear()
                            }) {
                                Text("Clear")
                            }

                            Button(onClick = {
                                penColor.value = Color.Red
                            }) {
                                Text("Red")
                            }

                            Button(onClick = {
                                penColor.value = Color.Black
                            }) {
                                Text("Black")
                            }
                        }

                        mutableBitmap.value?.let { bitmap ->
                            Column(Modifier.background(Color.Magenta)) {

                                Text(text = "Transparent bitmap: " + mutableBitmap.value)
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "some useful description",
                                )
                            }

                        }

                        Text(text = "SVG: " + mutableSvg.value)
                    }
                }
            }
        }
    }
}

private fun ConvertTransparentBackgroundToWhite(bm: Bitmap): Bitmap {
    var bm: Bitmap? = bm ?: return null
    if (bm.hasAlpha()) {
        bm = try {
            val newBitmap = Bitmap.createBitmap(bm.width, bm.height, bm.config)
            val canvas = Canvas(newBitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(bm, 0, 0, null)
            newBitmap
        } catch (exc: RuntimeException) {
            return null
        }
    }
    return bm
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        SignaturePadView()
    }
}
