package se.warting.signaturepad

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import se.warting.signaturecore.ExperimentalSignatureApi
import se.warting.signaturecore.Signature
import se.warting.signaturecore.utils.SignedListener
import se.warting.signatureview.views.SignaturePad

@SuppressWarnings("LongParameterList")
@Composable
fun SignaturePadView(
    modifier: Modifier = Modifier,
    penMinWidth: Dp = 3.dp,
    penMaxWidth: Dp = 7.dp,
    penColor: Color = Color.Black,
    velocityFilterWeight: Float = 0.9F,
    clearOnDoubleClick: Boolean = false,
    onReady: (svg: SignaturePadAdapter) -> Unit = {},
    onStartSigning: () -> Unit = {},
    onSigning: () -> Unit = {},
    onSigned: () -> Unit = {},
    onClear: () -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            // Creates custom view
            SignaturePad(context, null).apply {
                this.setOnSignedListener(object : SignedListener {

                    override fun onStartSigning() {
                        onStartSigning()
                    }

                    override fun onSigning() {
                        onSigning()
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
            it.setMinWidth(penMinWidth.value)
            it.setMaxWidth(penMaxWidth.value)
            it.setPenColor(penColor.toArgb())
            it.setVelocityFilterWeight(velocityFilterWeight)
            it.setClearOnDoubleClick(clearOnDoubleClick)
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
