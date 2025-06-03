package se.warting.signaturepad.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.warting.signaturepad.SignaturePadAdapter
import se.warting.signaturepad.SignaturePadView

private const val SIGNATURE_PAD_HEIGHT = 120

@Suppress("LongMethod")
class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Scaffold { padding ->

                    val mutableSvg = remember { mutableStateOf("") }
                    Column(modifier = Modifier.padding(padding)) {

                        var signaturePadAdapter: SignaturePadAdapter? = null
                        var penColor by remember { mutableStateOf(Color.Black) }

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
                                        "ComposeActivity",
                                        "onClear isEmpty:" + signaturePadAdapter?.isEmpty
                                    )
                                },
                            )
                        }
                        Row {
                            Button(onClick = {
                                mutableSvg.value = signaturePadAdapter?.getSignatureSvg() ?: ""
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
                                penColor = Color.Red
                            }) {
                                Text("Red")
                            }

                            Button(onClick = {
                                penColor = Color.Black
                            }) {
                                Text("Black")
                            }
                        }

                        Text(text = "SVG: " + mutableSvg.value)
                    }
                }
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
