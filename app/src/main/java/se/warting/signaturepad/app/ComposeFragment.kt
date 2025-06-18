package se.warting.signaturepad.app

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.warting.signaturepad.SignaturePadAdapter
import se.warting.signaturepad.SignaturePadView

private const val SIGNATURE_PAD_HEIGHT_DP = 120

@Composable
private fun ColorToggleGroup(
    title: String,
    options: List<Pair<String, Color>>,
    selected: Color,
    onSelect: (Color) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Row {
            options.forEachIndexed { idx, (label, color) ->
                val isSel = color == selected
                val shape = when (idx) {
                    0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    options.lastIndex -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    else -> RectangleShape
                }
                OutlinedButton(
                    onClick = { onSelect(color) },
                    shape = shape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSel)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        contentColor = if (isSel)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) { Text(label) }

                if (idx < options.lastIndex) {
                    Spacer(
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveClearRow(
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(modifier = Modifier.weight(1f), onClick = onSave) { Text("Save") }
        Button(modifier = Modifier.weight(1f), onClick = onClear) { Text("Clear") }
    }
}

private fun SignaturePadAdapter.extractBitmaps(
    useOverride: Boolean,
    bgColor: Color,
    strokeColor: Color?
): Pair<ImageBitmap?, ImageBitmap?> {
    fun toBmp(op: SignaturePadAdapter.() -> android.graphics.Bitmap?) =
        op()?.asImageBitmap()

    return if (useOverride) {
        toBmp { getSignatureBitmap(bgColor.toArgb(), strokeColor?.toArgb()) } to
                toBmp { getTransparentSignatureBitmap(penColor = strokeColor?.toArgb()) }
    } else {
        toBmp { getSignatureBitmap() } to
                toBmp { getTransparentSignatureBitmap() }
    }
}

@Composable
fun ComposeSample() {
    var svg by remember { mutableStateOf("") }
    var bmpPair by remember { mutableStateOf<Pair<ImageBitmap?, ImageBitmap?>>(null to null) }
    var adapter by remember { mutableStateOf<SignaturePadAdapter?>(null) }

    data class Toggle(
        val title: String,
        val options: List<Pair<String, Color>>,
        var state: MutableState<Color>
    )

    val toggles = listOf(
        Toggle(
            "Pen color:",
            listOf("Red" to Color.Red, "Black" to Color.Black, "White" to Color.White),
            remember { mutableStateOf(Color.Black) }),
        Toggle(
            "Image background color:",
            listOf("Red" to Color.Red, "Green" to Color.Green, "Blue" to Color.Blue),
            remember { mutableStateOf(Color.White) }),
        Toggle(
            "Image pen color:",
            listOf("Red" to Color.Red, "Black" to Color.Black, "White" to Color.White),
            remember { mutableStateOf(Color.Black) })
    )

    var useOverride by remember { mutableStateOf(false) }

    Column(
        Modifier
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
    ) {
        Box(
            Modifier
                .height(SIGNATURE_PAD_HEIGHT_DP.dp)
                .fillMaxWidth()
                .border(2.dp, Color.Gray)
        ) {
            SignaturePadView(
                onReady = { adapter = it },
                penColor = toggles[0].state.value,
                onStartSigning = { Log.d("SignedListener", "onStartSigning") },
                onSigning = { Log.d("SignedListener", "onSigning") },
                onSigned = { Log.d("SignedListener", "onSigned") },
                onClear = { Log.d("ComposeSample", "isEmpty=${adapter?.isEmpty}") }
            )
        }

        Spacer(Modifier.height(8.dp))

        toggles.forEach { tol ->
            ColorToggleGroup(tol.title, tol.options, tol.state.value) { tol.state.value = it }
            Spacer(Modifier.height(8.dp))
        }

        Row(
            Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Use override colors", Modifier.weight(1f))
            Switch(useOverride, onCheckedChange = { useOverride = it })
        }

        Spacer(Modifier.height(8.dp))

        SaveClearRow(
            onSave = {
                svg = adapter?.getSignatureSvg().orEmpty()
                adapter?.let {
                    bmpPair = it.extractBitmaps(
                        useOverride = useOverride,
                        bgColor = toggles[1].state.value,
                        strokeColor = toggles[2].state.value
                    )
                }
            },
            onClear = {
                svg = ""
                adapter?.clear()
            }
        )

        bmpPair.first?.let {
            Text("Bitmap")
            Image(
                it, "Signature", Modifier
                    .fillMaxWidth()
                    .height(SIGNATURE_PAD_HEIGHT_DP.dp)
                    .border(1.dp, Color.Gray)
            )
        }
        Spacer(Modifier.height(8.dp))
        bmpPair.second?.let {
            Text("Transparent Bitmap")
            Image(
                it, "Transparent", Modifier
                    .fillMaxWidth()
                    .height(SIGNATURE_PAD_HEIGHT_DP.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text("SVG:", style = MaterialTheme.typography.bodyMedium)
        Text(svg, style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true)
@Composable
fun ComposeSamplePreview() {
    MaterialTheme { ComposeSample() }
}
