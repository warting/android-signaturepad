package se.warting.signaturepad.app

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.warting.signaturecore.SignatureSDK
import se.warting.signaturepad.SignaturePadState
import se.warting.signaturepad.SignaturePadView
import se.warting.signaturepad.rememberSignaturePadState

private const val SIGNATURE_PAD_HEIGHT_DP = 200
private const val PEN_WIDTH_MIN_DP = 1f
private const val PEN_WIDTH_MAX_DP = 20f
private const val DEFAULT_PEN_MIN_WIDTH_DP = 3f
private const val DEFAULT_PEN_MAX_WIDTH_DP = 7f
private const val DEFAULT_SHADOW_INTENSITY = 0f
private const val SHADOW_ANGLE_MIN_DEGREES = 0f
private const val SHADOW_ANGLE_MAX_DEGREES = 360f
private val sampleShadowBlue = Color(0xFF1565C0)

@Composable
private fun ColorToggleGroup(
    @StringRes title: Int,
    options: List<Pair<Int, Color>>,
    selected: Color,
    onSelect: (Color) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(title), style = MaterialTheme.typography.bodyMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { idx, (labelRes, color) ->
                SegmentedButton(
                    selected = color == selected,
                    onClick = { onSelect(color) },
                    shape = SegmentedButtonDefaults.itemShape(idx, options.size),
                    modifier = Modifier.semantics { role = Role.RadioButton },
                ) {
                    Text(stringResource(labelRes))
                }
            }
        }
    }
}

@Composable
private fun SaveClearRow(
    onSave: () -> Unit,
    onClear: () -> Unit,
    onUndo: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onSave, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.save))
        }
        Button(onClick = onUndo, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.undo))
        }
        Button(onClick = onClear, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.clear))
        }
    }
}

private fun SignaturePadState.extractBitmaps(
    useOverride: Boolean,
    bgColor: Color,
    strokeColor: Color?
): Pair<ImageBitmap?, ImageBitmap?> {
    fun toBmp(op: SignaturePadState.() -> android.graphics.Bitmap?) =
        op()?.asImageBitmap()

    return if (useOverride) {
        toBmp { getSignatureBitmap(bgColor.toArgb(), strokeColor?.toArgb()) } to
                toBmp { getTransparentSignatureBitmap(penColor = strokeColor?.toArgb()) }
    } else {
        toBmp { getSignatureBitmap() } to
                toBmp { getTransparentSignatureBitmap() }
    }
}

@Suppress("LongMethod")
@Composable
fun ComposeSample() {
    var svg by remember { mutableStateOf("") }
    var bmpPair by remember { mutableStateOf<Pair<ImageBitmap?, ImageBitmap?>>(null to null) }
    val signatureState = rememberSignaturePadState()

    data class Toggle(
        @param:StringRes val title: Int,
        val options: List<Pair<Int, Color>>,
        var state: MutableState<Color>
    )

    val penColor = remember { mutableStateOf(Color.Black) }
    val shadowColor = remember { mutableStateOf(Color.Black) }
    val imageBackgroundColor = remember { mutableStateOf(Color.White) }
    val imagePenColor = remember { mutableStateOf(Color.Black) }
    val toggles = listOf(
        Toggle(
            R.string.pen_color_label,
            listOf(
                R.string.color_red to Color.Red,
                R.string.color_black to Color.Black,
                R.string.color_white to Color.White,
            ),
            penColor
        ),
        Toggle(
            R.string.shadow_color_label,
            listOf(
                R.string.color_black to Color.Black,
                R.string.color_blue to sampleShadowBlue,
                R.string.color_red to Color.Red,
            ),
            shadowColor
        ),
        Toggle(
            R.string.image_background_color_label,
            listOf(
                R.string.color_red to Color.Red,
                R.string.color_green to Color.Green,
                R.string.color_blue to Color.Blue,
            ),
            imageBackgroundColor
        ),
        Toggle(
            R.string.image_pen_color_label,
            listOf(
                R.string.color_red to Color.Red,
                R.string.color_black to Color.Black,
                R.string.color_white to Color.White,
            ),
            imagePenColor
        )
    )

    var useOverride by remember { mutableStateOf(false) }
    var penMinWidth by remember { mutableFloatStateOf(DEFAULT_PEN_MIN_WIDTH_DP) }
    var penMaxWidth by remember { mutableFloatStateOf(DEFAULT_PEN_MAX_WIDTH_DP) }
    var shadowIntensity by remember { mutableFloatStateOf(DEFAULT_SHADOW_INTENSITY) }
    var shadowAngleDegrees by remember {
        mutableFloatStateOf(SignatureSDK.DEFAULT_ATTR_SHADOW_ANGLE_DEGREES)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            Modifier
                .height(SIGNATURE_PAD_HEIGHT_DP.dp)
                .fillMaxWidth()
                .border(2.dp, Color.Gray)
        ) {
            SignaturePadView(
                modifier = Modifier.fillMaxSize(),
                state = signatureState,
                penColor = penColor.value,
                penMinWidth = penMinWidth.dp,
                penMaxWidth = penMaxWidth.dp,
                shadowColor = shadowColor.value,
                shadowIntensity = shadowIntensity,
                shadowAngleDegrees = shadowAngleDegrees,
                onStartSigning = { Log.d("SignedListener", "onStartSigning") },
                onSigning = { Log.d("SignedListener", "onSigning") },
                onSigned = { Log.d("SignedListener", "onSigned") },
                onClear = { Log.d("ComposeSample", "isEmpty=${signatureState.isEmpty}") }
            )
        }

        Spacer(Modifier.height(8.dp))

        toggles.forEach { tol ->
            ColorToggleGroup(tol.title, tol.options, tol.state.value) { tol.state.value = it }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            stringResource(R.string.pen_min_width_format, penMinWidth),
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = penMinWidth,
            onValueChange = {
                penMinWidth = it
                if (penMaxWidth < it) penMaxWidth = it
            },
            valueRange = PEN_WIDTH_MIN_DP..PEN_WIDTH_MAX_DP,
        )
        Text(
            stringResource(R.string.pen_max_width_format, penMaxWidth),
            style = MaterialTheme.typography.bodyMedium
        )

        Slider(
            value = penMaxWidth,
            onValueChange = {
                penMaxWidth = it
                if (penMinWidth > it) penMinWidth = it
            },
            valueRange = PEN_WIDTH_MIN_DP..PEN_WIDTH_MAX_DP,
        )
        Text(
            stringResource(R.string.shadow_intensity_format, shadowIntensity),
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = shadowIntensity,
            onValueChange = { shadowIntensity = it },
            valueRange = 0f..1f,
        )
        Text(
            stringResource(R.string.shadow_angle_format, shadowAngleDegrees),
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = shadowAngleDegrees,
            onValueChange = { shadowAngleDegrees = it },
            valueRange = SHADOW_ANGLE_MIN_DEGREES..SHADOW_ANGLE_MAX_DEGREES,
        )
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.use_override_colors), Modifier.weight(1f))
            Switch(useOverride, onCheckedChange = { useOverride = it })
        }

        Spacer(Modifier.height(8.dp))

        SaveClearRow(
            onSave = {
                svg = if (useOverride) {
                    signatureState.getSignatureSvg(
                        penColor = imagePenColor.value.toArgb(),
                        backgroundColor = imageBackgroundColor.value.toArgb(),
                    )
                } else {
                    signatureState.getSignatureSvg()
                }
                bmpPair = signatureState.extractBitmaps(
                    useOverride = useOverride,
                    bgColor = imageBackgroundColor.value,
                    strokeColor = imagePenColor.value
                )
            },
            onClear = {
                svg = ""
                signatureState.clear()
            },
            onUndo = {
                signatureState.undo()
            }
        )

        bmpPair.first?.let {
            Text(stringResource(R.string.bitmap))
            Image(
                it, stringResource(R.string.signature), Modifier
                    .fillMaxWidth()
                    .height(SIGNATURE_PAD_HEIGHT_DP.dp)
                    .border(1.dp, Color.Gray)
            )
        }
        Spacer(Modifier.height(8.dp))
        bmpPair.second?.let {
            Text(stringResource(R.string.transparent_bitmap))
            Image(
                it, stringResource(R.string.transparent), Modifier
                    .fillMaxWidth()
                    .height(SIGNATURE_PAD_HEIGHT_DP.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.svg_label), style = MaterialTheme.typography.bodyMedium)
        Text(svg, style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true)
@Composable
fun ComposeSamplePreview() {
    MaterialTheme { ComposeSample() }
}
