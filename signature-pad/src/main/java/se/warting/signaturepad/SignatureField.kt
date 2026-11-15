package se.warting.signaturepad

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Optional signature field with a decorative guideline and caller-provided placeholder.
 * The placeholder is visible only while empty. Decorations are excluded from all exports.
 * The default height is 160 dp; set a height in [modifier] to override it. Placeholder size
 * does not determine the canvas size, so hiding it cannot crop an existing signature.
 * Use [SignaturePadView] directly for lower-level configuration and signing callbacks.
 */
@Suppress("LongParameterList")
@Composable
fun SignatureField(
    modifier: Modifier = Modifier,
    state: SignaturePadState = rememberSignaturePadState(),
    showGuideline: Boolean = true,
    guidelineColor: Color = Color.Gray,
    penColor: Color = Color.Black,
    penMinWidth: Dp = 3.dp,
    penMaxWidth: Dp = 7.dp,
    content: @Composable () -> Unit = {},
) {
    Box(modifier.fillMaxWidth().height(160.dp)) {
        SignaturePadView(
            modifier = Modifier.matchParentSize().drawBehind {
                if (showGuideline) {
                    val inset = 16.dp.toPx().coerceAtMost(size.width / 2)
                    val baseline = (size.height - 32.dp.toPx()).coerceAtLeast(0f)
                    drawLine(guidelineColor, Offset(inset, baseline), Offset(size.width - inset, baseline))
                }
            },
            state = state,
            penColor = penColor,
            penMinWidth = penMinWidth,
            penMaxWidth = penMaxWidth,
        )
        if (state.isEmpty) {
            Box(Modifier.align(Alignment.Center)) { content() }
        }
    }
}
