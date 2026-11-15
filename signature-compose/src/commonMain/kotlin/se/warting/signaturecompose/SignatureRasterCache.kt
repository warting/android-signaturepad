package se.warting.signaturecompose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import se.warting.signatureengine.SignatureDocument
import se.warting.signatureengine.forEachDab

/** Each canvas retains its raster; appending input only rasterizes newly produced segments. */
internal class SignatureRasterCache {
    private var bitmap: ImageBitmap? = null
    private var canvas: Canvas? = null
    private var generation = -1
    private var density = 0f
    private var color = Color.Unspecified
    private var segmentCursor = 0
    internal var rasterizedSegments = 0
        private set

    fun render(document: SignatureDocument, generation: Int, width: Int, height: Int,
        density: Float, color: Color): ImageBitmap? {
        if (width <= 0 || height <= 0) return null
        if (bitmap?.width != width || bitmap?.height != height || this.generation != generation ||
            this.density != density || this.color != color || segmentCursor > document.segmentCount) {
            bitmap = ImageBitmap(width, height)
            canvas = Canvas(checkNotNull(bitmap))
            this.generation = generation
            this.density = density
            this.color = color
            segmentCursor = 0
        }
        val paint = Paint().apply { this.color = color; isAntiAlias = true }
        document.segmentsFrom(segmentCursor).forEach { segment ->
            segment.forEachDab { x, y, strokeWidth ->
                canvas!!.drawCircle(Offset(x * density, y * density), strokeWidth * density / 2, paint)
            }
            rasterizedSegments++
            segmentCursor++
        }
        return bitmap
    }
}
