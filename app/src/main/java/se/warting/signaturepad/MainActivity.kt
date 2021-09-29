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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    val context = LocalContext.current

                    Column {
                        Button(onClick = {
                            val intent = Intent(context, ComposeActivity::class.java)
                            startActivity(intent)
                        }) {
                            Text("Compose")
                        }

                        Button(onClick = {
                            val intent = Intent(context, ViewActivity::class.java)
                            startActivity(intent)
                        }) {
                            Text("View")
                        }
                        Button(onClick = {
                            val intent = Intent(context, ViewActivity::class.java)
                            startActivity(intent)
                        }) {
                            Text("databind")
                        }
                        Button(onClick = {
                            val intent = Intent(context, SaveRestoreActivity::class.java)
                            startActivity(intent)
                        }) {
                            Text("SaveRestoreActivity")
                        }
                    }
                }
            }
        }
    }
}
