package se.warting.signaturepad
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import se.warting.signaturecore.DrawEvent

/**
 * Draws the given [SignatureLine]s to a Canvas and returns the resulting [ImageBitmap].
 * @param width The width of the resulting [ImageBitmap].
 * @param height The height of the resulting [ImageBitmap].
 * @param signatureColor The color of the signature lines.
 * @param signatureSize The thickness of the signature lines. Note that
 * the thickness is multiplied by 3 to make the signature lines more visible.
 * @param signatureSignatureLines The list of signature lines to draw.
 */
internal fun toImageBitmap(
    width: Int,
    height: Int,
    signatureColor: Color,
    signatureSize: Dp,
    signatureSignatureLines: List<DrawEvent>,
): ImageBitmap {
    val imgBitmap = ImageBitmap(width, height)

    Canvas(imgBitmap).apply {
        CanvasDrawScope().draw(
            density = Density(1f, 1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = this,
            size = Size(width.toFloat(), height.toFloat()),
        ) {
            signatureSignatureLines.forEach {
                drawPoints(
                    points = listOf(Offset(it.x, it.y)),
                    pointMode = PointMode.Points,
                    color = signatureColor,
                    strokeWidth = it.strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    return imgBitmap
}
