/*
 * MIT License
 *
 * Copyright (c) 2021. Stefan Wärting
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import se.warting.signaturecore.Event
import se.warting.signaturecore.ExperimentalSignatureApi
import se.warting.signaturecore.Signature

private const val SIGNATURE_PAD_HEIGHT = 120

@SuppressWarnings("LongMethod")
class SaveRestoreActivity : ComponentActivity() {
    @OptIn(ExperimentalSignatureApi::class)
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
            }
        }
    }

    private fun Signature.serialize(): String {
        // save version!
        return this.events.joinToString(separator = "\n") {
            ("${it.timestamp},${it.action},${it.x},${it.y}")
        }
    }

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
}
