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
import se.warting.signaturecore.ExperimentalSignatureApi
import se.warting.signaturecore.Signature
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

    @ExperimentalSignatureApi
    fun getSignature(): Signature {
        return signaturePad.getSignature()
    }

    @ExperimentalSignatureApi
    fun setSignature(signature: Signature) {
        return signaturePad.setSignature(signature)
    }
}
