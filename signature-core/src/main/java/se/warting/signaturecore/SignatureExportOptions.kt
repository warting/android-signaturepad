package se.warting.signaturecore

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * Raster export options shared by Compose, Views and standalone SDK instances.
 * Cropping uses the visible ink bounds before applying the background. Empty ink
 * retains the canvas dimensions. Padding adds pixels on every side, even at a
 * canvas edge; it never clips the signature to make room for the margin.
 */
data class SignatureExportOptions(
    val trimBlankSpace: Boolean = false,
    val paddingPixels: Int = 0,
    @ColorInt val backgroundColor: Int = Color.TRANSPARENT,
    @ColorInt val penColor: Int? = null,
) {
    init {
        require(paddingPixels >= 0) { "paddingPixels must be non-negative" }
    }
}
