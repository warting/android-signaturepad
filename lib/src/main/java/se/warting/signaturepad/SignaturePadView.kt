package se.warting.signaturepad

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.gcacace.signaturepad.views.SignaturePad

@Composable
fun SignaturePadView(
    penMinWidth: Dp = 3.dp,
    penMaxWidth: Dp = 7.dp,
    penColor: Color = Color.Black,
    velocityFilterWeight: Float = 0.9F,
    onReady: (svg: SignaturePadAdapter) -> Unit,
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

                this.setOnSignedListener(object : SignaturePad.OnSignedListener {
                    override fun onStartSigning() {
                        Log.d("SignaturePadView", "onStartSigning")
                    }

                    override fun onSigned() {
                        // onSigned(signatureSvg)
                        Log.d("SignaturePadView", "onSigned")
                    }

                    override fun onClear() {
                        Log.d("SignaturePadView", "onClear")
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

    fun getSignatureBitmap(): Bitmap {
        return signaturePad.signatureBitmap!!
    }

    fun getTransparentSignatureBitmap(): Bitmap {
        return signaturePad.transparentSignatureBitmap!!
    }

    fun getSignatureSvg(): String {
        return signaturePad.signatureSvg
    }
}
