package se.warting.signaturecore

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap

/**
 * Renders this [Signature] into a [Bitmap] of the requested size without needing
 * a `SignaturePadAdapter` or `SignaturePad` view instance.
 *
 * @param width Bitmap width in pixels.
 * @param height Bitmap height in pixels.
 * @param backgroundColor ARGB color drawn behind the signature.
 * @param penColor ARGB color of the signature stroke.
 * @param penMinWidth Minimum stroke width in pixels.
 * @param penMaxWidth Maximum stroke width in pixels.
 * @param velocityFilterWeight Smoothing weight applied to stroke velocity.
 */
@ExperimentalSignatureApi
@Suppress("LongParameterList")
fun Signature.toBitmap(
    width: Int,
    height: Int,
    @ColorInt backgroundColor: Int = Color.WHITE,
    @ColorInt penColor: Int = SignatureSDK.DEFAULT_ATTR_PEN_COLOR,
    penMinWidth: Int = SignatureSDK.DEFAULT_ATTR_PEN_MIN_WIDTH_PX,
    penMaxWidth: Int = SignatureSDK.DEFAULT_ATTR_PEN_MAX_WIDTH_PX,
    velocityFilterWeight: Float = SignatureSDK.DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT,
): Bitmap {
    val sdk = newConfiguredSdk(penMinWidth, penMaxWidth, penColor, velocityFilterWeight)
    sdk.initializeBitmap(width, height)
    sdk.restoreEvents(events)
    return sdk.getSignatureBitmap(backgroundColor = backgroundColor)
        ?: createBitmap(width, height).apply { eraseColor(backgroundColor) }
}

/**
 * Renders this [Signature] into a transparent [Bitmap] without needing a
 * `SignaturePadAdapter` or `SignaturePad` view instance.
 *
 * @param width Bitmap width in pixels.
 * @param height Bitmap height in pixels.
 * @param trimBlankSpace When true, the returned bitmap is cropped to the signature bounds.
 * @param penColor ARGB color of the signature stroke.
 * @param penMinWidth Minimum stroke width in pixels.
 * @param penMaxWidth Maximum stroke width in pixels.
 * @param velocityFilterWeight Smoothing weight applied to stroke velocity.
 */
@ExperimentalSignatureApi
@Suppress("LongParameterList")
fun Signature.toTransparentBitmap(
    width: Int,
    height: Int,
    trimBlankSpace: Boolean = false,
    @ColorInt penColor: Int = SignatureSDK.DEFAULT_ATTR_PEN_COLOR,
    penMinWidth: Int = SignatureSDK.DEFAULT_ATTR_PEN_MIN_WIDTH_PX,
    penMaxWidth: Int = SignatureSDK.DEFAULT_ATTR_PEN_MAX_WIDTH_PX,
    velocityFilterWeight: Float = SignatureSDK.DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT,
): Bitmap {
    val sdk = newConfiguredSdk(penMinWidth, penMaxWidth, penColor, velocityFilterWeight)
    sdk.initializeBitmap(width, height)
    sdk.restoreEvents(events)
    return sdk.getTransparentSignatureBitmap(trimBlankSpace = trimBlankSpace)
        ?: createBitmap(width, height)
}

/**
 * Renders this [Signature] as an SVG document without needing a
 * `SignaturePadAdapter` or `SignaturePad` view instance.
 *
 * @param width SVG canvas width in pixels.
 * @param height SVG canvas height in pixels.
 * @param penColor ARGB color of the signature stroke. When null the stroke defaults to black.
 * @param backgroundColor ARGB color filled behind the signature. When null the SVG is transparent.
 * @param penMinWidth Minimum stroke width in pixels.
 * @param penMaxWidth Maximum stroke width in pixels.
 * @param velocityFilterWeight Smoothing weight applied to stroke velocity.
 */
@ExperimentalSignatureApi
@Suppress("LongParameterList")
fun Signature.toSvg(
    width: Int,
    height: Int,
    @ColorInt penColor: Int? = null,
    @ColorInt backgroundColor: Int? = null,
    penMinWidth: Int = SignatureSDK.DEFAULT_ATTR_PEN_MIN_WIDTH_PX,
    penMaxWidth: Int = SignatureSDK.DEFAULT_ATTR_PEN_MAX_WIDTH_PX,
    velocityFilterWeight: Float = SignatureSDK.DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT,
): String {
    val sdk = newConfiguredSdk(
        penMinWidth,
        penMaxWidth,
        penColor ?: SignatureSDK.DEFAULT_ATTR_PEN_COLOR,
        velocityFilterWeight,
    )
    sdk.restoreEvents(events)
    return sdk.getSignatureSvg(width, height, penColor, backgroundColor)
}

private fun newConfiguredSdk(
    minWidth: Int,
    maxWidth: Int,
    @ColorInt penColor: Int,
    velocityFilterWeight: Float,
): SignatureSDK = SignatureSDK().also {
    it.configure(
        minWidth = minWidth,
        maxWidth = maxWidth,
        penColor = penColor,
        velocityFilterWeight = velocityFilterWeight,
    )
}
