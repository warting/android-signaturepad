package se.warting.signaturepad

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    val mutableSvg = remember { mutableStateOf("") }
                    Column {

                        var signaturePadAdapter: SignaturePadAdapter? = null

                        Box(
                            modifier = Modifier
                                .height(120.dp)
                                .fillMaxWidth()
                                .border(
                                    width = 2.dp,
                                    color = Color.Red,
                                )
                        ) {

                            SignaturePadView(onReady = {
                                signaturePadAdapter = it
                            })
                        }
                        Row {
                            Button(onClick = {
                                Log.d(
                                    "MainActivity",
                                    signaturePadAdapter?.getSignatureSvg() ?: "null"
                                )
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
        SignaturePadView {
        }
    }
}
