package se.warting.signatureink

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.brush.Brush

@Composable
fun rememberInkSignatureState(): InkSignatureState = remember { InkSignatureState() }

/** Compose integration for the optional Android low-latency Ink surface. */
@Composable
fun InkSignaturePad(
    modifier: Modifier = Modifier,
    state: InkSignatureState = rememberInkSignatureState(),
    brush: Brush? = null,
) {
    AndroidView(
        factory = { context -> InkSignatureView(context) },
        modifier = modifier,
        update = { view ->
            view.state = state
            view.brush = brush ?: view.defaultBrush
        },
    )
}
