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
    Column {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (label, color) ->
                val isSelected = color == selected
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    options.lastIndex -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    else -> RectangleShape
                }
                OutlinedButton(
                    onClick = { onSelect(color) },
                    shape = shape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label)
                }
                if (index < options.lastIndex) {
                    Spacer(
                        modifier = Modifier
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
fun ComposeSample() {
    var svg by remember { mutableStateOf("") }
    var savedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var adapter by remember { mutableStateOf<SignaturePadAdapter?>(null) }

    var penColor by remember { mutableStateOf(Color.Black) }
    var bgColor by remember { mutableStateOf(Color.White) }
    var strokeOverride by remember { mutableStateOf<Color?>(null) }

    // Toggle to choose between default bitmap vs. override-color bitmap
    var useOverride by remember { mutableStateOf(false) }

    Column(
        Modifier
            .padding(16.dp)
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
                penColor = penColor,
                onStartSigning = { Log.d("SignedListener", "onStartSigning") },
                onSigning = { Log.d("SignedListener", "onSigning") },
                onSigned = { Log.d("SignedListener", "onSigned") },
                onClear = { Log.d("ComposeSample", "isEmpty=${adapter?.isEmpty}") }
            )
        }

        Spacer(Modifier.height(12.dp))

        ColorToggleGroup(
            title = "Pen color:",
            options = listOf("Red" to Color.Red, "Black" to Color.Black, "White" to Color.White),
            selected = penColor,
            onSelect = { penColor = it }
        )

        Spacer(Modifier.height(8.dp))

        ColorToggleGroup(
            title = "Image background color:",
            options = listOf("Red" to Color.Red, "Green" to Color.Green, "Blue" to Color.Blue),
            selected = bgColor,
            onSelect = { bgColor = it }
        )

        Spacer(Modifier.height(8.dp))

        ColorToggleGroup(
            title = "Image pen color:",
            options = listOf("Red" to Color.Red, "Black" to Color.Black, "White" to Color.White),
            selected = strokeOverride ?: Color.Black,
            onSelect = { strokeOverride = it }
        )

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Use override colors", Modifier.weight(1f))
            Switch(
                checked = useOverride,
                onCheckedChange = { useOverride = it }
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    svg = adapter?.getSignatureSvg().orEmpty()

                    savedBitmap = if (useOverride) {
                        adapter
                            ?.getSignatureBitmap(
                                backgroundColor = bgColor.toArgb(),
                                overrideStrokeColor = strokeOverride?.toArgb()
                            )
                            ?.asImageBitmap()
                    } else {
                        adapter
                            ?.getSignatureBitmap()
                            ?.asImageBitmap()
                    }
                }
            ) {
                Text("Save")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    svg = ""
                    adapter?.clear()
                }
            ) {
                Text("Clear")
            }
        }

        savedBitmap?.let { img ->
            Spacer(Modifier.height(12.dp))
            Image(
                bitmap = img,
                contentDescription = "Saved signature",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SIGNATURE_PAD_HEIGHT_DP.dp)
                    .border(1.dp, Color.Gray)
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("SVG:", style = MaterialTheme.typography.bodyMedium)
        Text(svg, style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true)
@Composable
fun ComposeSamplePreview() {
    MaterialTheme {
        ComposeSample()
    }
}
