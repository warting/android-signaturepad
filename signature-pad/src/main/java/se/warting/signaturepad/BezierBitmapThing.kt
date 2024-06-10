package se.warting.signaturepad

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import se.warting.signaturecore.DrawEvent


class BezierBitmapThing(
    minWidth: Int,
    maxWidth: Int,
    velocityFilterWeight: Float
) : BezierThing(minWidth, maxWidth, velocityFilterWeight) {
    var bitmap: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    var canvas = Canvas(bitmap)
    val bmp = mutableStateOf(bitmap.asImageBitmap())

    override fun draw(drawEvent: DrawEvent) {
        super.draw(drawEvent)
        // draw on canvas
        val paint = Paint()
        paint.color = drawEvent.color
        paint.strokeWidth = drawEvent.strokeWidth
        canvas.drawPoint(drawEvent.x, drawEvent.y, paint)
        bmp.value = bitmap.asImageBitmap()
    }

    fun setBitmapSize(width: Int, height: Int) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
         canvas = Canvas(bitmap)
    }
}
