package se.warting.signaturecore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Base64
import androidx.core.graphics.createBitmap
import java.io.ByteArrayOutputStream

/** Returns an independent bitmap, or null if no raster surface has been initialized. */
fun SignatureSDK.exportBitmap(options: SignatureExportOptions = SignatureExportOptions()): Bitmap? {
    val ink = getTransparentSignatureBitmap(options.trimBlankSpace, options.penColor) ?: return null
    return if (options.paddingPixels == 0 && options.backgroundColor == Color.TRANSPARENT) {
        ink
    } else {
        try {
            val width = ink.width.toLong() + options.paddingPixels.toLong() * 2
            val height = ink.height.toLong() + options.paddingPixels.toLong() * 2
            require(width <= Int.MAX_VALUE && height <= Int.MAX_VALUE) { "Export dimensions overflow" }
            createBitmap(width.toInt(), height.toInt()).also { bitmap ->
                Canvas(bitmap).apply {
                    drawColor(options.backgroundColor)
                    drawBitmap(ink, options.paddingPixels.toFloat(), options.paddingPixels.toFloat(), null)
                }
            }
        } finally {
            ink.recycle()
        }
    }
}

/** Encodes PNG bytes. Returns null before the raster surface is initialized. */
fun SignatureSDK.toPngByteArray(options: SignatureExportOptions = SignatureExportOptions()): ByteArray? {
    val bitmap = exportBitmap(options) ?: return null
    return try {
        ByteArrayOutputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)) { "PNG encoding failed" }
            stream.toByteArray()
        }
    } finally {
        bitmap.recycle()
    }
}

/** Encodes PNG as standard Base64, without line breaks or a data-URL prefix. */
fun SignatureSDK.toBase64(options: SignatureExportOptions = SignatureExportOptions()): String? =
    toPngByteArray(options)?.let { Base64.encodeToString(it, Base64.NO_WRAP) }

// PNG is lossless; Android ignores the quality argument for this format.
private const val PNG_QUALITY = 100
