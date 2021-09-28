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

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import se.warting.signatureview.views.SignaturePad
import se.warting.signatureview.views.SignedListener

@SuppressWarnings("LongParameterList")
@Composable
fun SignaturePadView(
    penMinWidth: Dp = 3.dp,
    penMaxWidth: Dp = 7.dp,
    penColor: Color = Color.Black,
    velocityFilterWeight: Float = 0.9F,
    clearOnDoubleClick: Boolean = true,
    onReady: (svg: SignaturePadAdapter) -> Unit = {},
    onStartSigning: () -> Unit = {},
    onSigned: () -> Unit = {},
    onClear: () -> Unit = {},
) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize(),
        factory = { context ->
            // Creates custom view
            SignaturePad(context, null).apply {
                this.setMinWidth(penMinWidth.value)
                this.setMaxWidth(penMaxWidth.value)
                this.setPenColor(penColor.toArgb())
                this.setVelocityFilterWeight(velocityFilterWeight)
                this.setClearOnDoubleClick(clearOnDoubleClick)

                this.setOnSignedListener(object : SignedListener {
                    override fun onStartSigning() {
                        onStartSigning()
                    }

                    override fun onSigned() {
                        onSigned()
                    }

                    override fun onClear() {
                        onClear()
                    }
                })
            }
        },
        update = {
            onReady(SignaturePadAdapter(it))
        },
    )
}

class SignaturePadAdapter(private val signaturePad: SignaturePad) {

    fun clear() {
        signaturePad.clear()
    }

    val isEmpty: Boolean
        get() = signaturePad.isEmpty

    @Suppress("unused")
    fun getSignatureBitmap(): Bitmap {
        return signaturePad.getSignatureBitmap()
    }

    @Suppress("unused")
    fun getTransparentSignatureBitmap(): Bitmap {
        return signaturePad.getTransparentSignatureBitmap()
    }

    fun getSignatureSvg(): String {
        return signaturePad.getSignatureSvg()
    }
}
