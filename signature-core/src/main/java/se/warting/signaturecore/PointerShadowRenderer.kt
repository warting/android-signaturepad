package se.warting.signaturecore

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// Keep RuntimeShader types isolated from devices running Android 12L and earlier.
internal fun createPointerShadowRenderer(): PointerShadowRenderer =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Api33PointerShadowRenderer()
    } else {
        NoOpPointerShadowRenderer
    }

internal interface PointerShadowRenderer {
    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        pointerX: Float,
        pointerY: Float,
        pressure: Float,
        @ColorInt shadowColor: Int,
        shadowIntensity: Float,
        shadowAngleDegrees: Float,
    )
}

private object NoOpPointerShadowRenderer : PointerShadowRenderer {
    override fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        pointerX: Float,
        pointerY: Float,
        pressure: Float,
        @ColorInt shadowColor: Int,
        shadowIntensity: Float,
        shadowAngleDegrees: Float,
    ) = Unit
}

private data class ShadowFloat3(
    val x: Float,
    val y: Float,
    val z: Float,
) {
    fun magnitude(): Float = sqrt(x * x + y * y + z * z)

    operator fun minus(value: ShadowFloat3) = ShadowFloat3(
        x = x - value.x,
        y = y - value.y,
        z = z - value.z,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class Api33PointerShadowRenderer : PointerShadowRenderer {
    private var shader: RuntimeShader? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        pointerX: Float,
        pointerY: Float,
        pressure: Float,
        @ColorInt shadowColor: Int,
        shadowIntensity: Float,
        shadowAngleDegrees: Float,
    ) {
        val hasValidPosition = pointerX.isFinite() && pointerY.isFinite()
        if (!canvas.isHardwareAccelerated || !hasValidPosition ||
            shouldSkipDraw(width, height, pressure, shadowIntensity)
        ) {
            return
        }

        val runtimeShader = shader ?: RuntimeShader(CAPSULE_SOFT_SHADOW_SHADER).also {
            shader = it
        }
        val normalizedPressure = pressure.coerceIn(0f, 1f)
        val shadowAlpha = (Color.alpha(shadowColor) / MAX_COLOR_COMPONENT) *
            shadowIntensity.coerceIn(0f, 1f) *
            sqrt(normalizedPressure)
        if (shadowAlpha <= 0f) return

        val maxDimension = 1.0f / max(width, height).toFloat()
        val fingerPosition = ShadowFloat3(
            x = (2.0f * pointerX - width) * maxDimension,
            y = (2.0f * pointerY - height) * maxDimension,
            z = FINGER_Z,
        )

        configureShader(
            shader = runtimeShader,
            fingerPosition = fingerPosition,
            shadowColor = Color.argb(
                (shadowAlpha * MAX_COLOR_COMPONENT).roundToInt().coerceIn(0, MAX_COLOR_COMPONENT_INT),
                Color.red(shadowColor),
                Color.green(shadowColor),
                Color.blue(shadowColor),
            ),
            shadowAngleDegrees = shadowAngleDegrees,
        )
        runtimeShader.setFloatUniform("size", width.toFloat(), height.toFloat())

        paint.shader = runtimeShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
    }

    private fun shouldSkipDraw(
        width: Int,
        height: Int,
        pressure: Float,
        shadowIntensity: Float,
    ): Boolean = when {
        width <= 0 || height <= 0 -> true
        !pressure.isFinite() || !shadowIntensity.isFinite() -> true
        pressure <= 0f || shadowIntensity <= 0f -> true
        else -> false
    }

    private fun configureShader(
        shader: RuntimeShader,
        fingerPosition: ShadowFloat3,
        @ColorInt shadowColor: Int,
        shadowAngleDegrees: Float,
    ) {
        val fingerDirection = rotateDefaultVector(FINGER_DIRECTION, shadowAngleDegrees)
        val lightOffset = rotateDefaultVector(LIGHT_POSITION - fingerPosition, shadowAngleDegrees)
        val lightPosition = ShadowFloat3(
            x = fingerPosition.x + lightOffset.x,
            y = fingerPosition.y + lightOffset.y,
            z = fingerPosition.z + lightOffset.z,
        )
        val fingerDirectionMagnitude = 1.0f / fingerDirection.magnitude()
        val lightDirection = lightPosition - fingerPosition
        val lightDirectionMagnitude = 1.0f / lightDirection.magnitude()

        shader.setColorUniform("shadowColor", shadowColor)
        shader.setFloatUniform(
            "fingerPosition",
            fingerPosition.x,
            fingerPosition.y,
            fingerPosition.z,
        )
        shader.setFloatUniform(
            "fingerDirection",
            fingerDirection.x * fingerDirectionMagnitude,
            fingerDirection.y * fingerDirectionMagnitude,
            fingerDirection.z * fingerDirectionMagnitude,
        )
        shader.setFloatUniform("fingerLength", FINGER_LENGTH)
        shader.setFloatUniform("fingerSquareRadius", FINGER_RADIUS * FINGER_RADIUS)
        shader.setFloatUniform(
            "lightConeDirection",
            lightDirection.x * lightDirectionMagnitude,
            lightDirection.y * lightDirectionMagnitude,
            lightDirection.z * lightDirectionMagnitude,
        )

        val coneAngle = radians(LIGHT_ANGLE) / 2f
        shader.setFloatUniform("lightConeAngle", cos(coneAngle), coneAngle)
        shader.setFloatUniform(
            "fadeDistance",
            lightPosition.x,
            lightPosition.y,
            lightPosition.z,
            1.0f / (FADE_DISTANCE * FADE_DISTANCE),
        )
    }

    private fun rotateDefaultVector(vector: ShadowFloat3, angleDegrees: Float): ShadowFloat3 {
        val delta = radians(angleDegrees - SignatureSDK.DEFAULT_ATTR_SHADOW_ANGLE_DEGREES)
        val cosDelta = cos(delta)
        val sinDelta = sin(delta)
        return ShadowFloat3(
            x = vector.x * cosDelta - vector.y * sinDelta,
            y = vector.x * sinDelta + vector.y * cosDelta,
            z = vector.z,
        )
    }

    private fun radians(degrees: Float): Float = degrees * (FLOAT_PI / HALF_CIRCLE_DEGREES)

    private companion object {
        private const val MAX_COLOR_COMPONENT = 255f
        private const val MAX_COLOR_COMPONENT_INT = 255
        private const val HALF_CIRCLE_DEGREES = 180f
        private const val FLOAT_PI = 3.1415927f
        private const val FINGER_Z = -0.1f
        private const val FINGER_LENGTH = 0.9f
        private const val FINGER_RADIUS = 0.06f
        private const val LIGHT_ANGLE = 55.0f
        private const val FADE_DISTANCE = 3.0f

        private val FINGER_DIRECTION = ShadowFloat3(0.18f, 0.22f, -0.12f)
        private val LIGHT_POSITION = ShadowFloat3(0.0f, -1.0f, -1.3f)
    }
}

// Adapted from Romain Guy's shadow-pointer sample:
// https://github.com/romainguy/shadow-pointer (Apache-2.0).
private const val CAPSULE_SOFT_SHADOW_SHADER = """
layout(color) uniform half4 shadowColor;
uniform vec3 fingerPosition;
uniform float fingerSquareRadius;
uniform vec3 fingerDirection;
uniform float fingerLength;
uniform vec3 lightConeDirection;
uniform vec2 lightConeAngle;
uniform vec2 size;
uniform vec4 fadeDistance;

const float PI = 3.1415927;

float sq(float x) {
    return x * x;
}

float acosFast(float x) {
    float y = abs(x);
    float p = -0.1565827 * y + 1.570796;
    p *= sqrt(1.0 - y);
    return x >= 0.0 ? p : PI - p;
}

float acosFastPositive(float x) {
    float p = -0.1565827 * x + 1.570796;
    return p * sqrt(1.0 - x);
}

float sphericalCapsIntersection(float cosCap1, float cosCap2, float cap2, float cosDistance) {
    float r1 = acosFastPositive(cosCap1);
    float r2 = cap2;
    float d  = acosFast(cosDistance);

    if (min(r1, r2) <= max(r1, r2) - d) {
        return 1.0 - max(cosCap1, cosCap2);
    } else if (r1 + r2 <= d) {
        return 0.0;
    }

    float delta = abs(r1 - r2);
    float x = 1.0 - saturate((d - delta) / max(r1 + r2 - delta, 0.0001));
    float area = sq(x) * (-2.0 * x + 3.0);
    return area * (1.0 - max(cosCap1, cosCap2));
}

float directionalOcclusionSphere(
    in vec3 pos,
    in vec4 sphere,
    in vec3 coneDirection,
    in vec2 coneAngle
) {
    vec3 occluder = sphere.xyz - pos;
    float occluderLength2 = dot(occluder, occluder);
    vec3 occluderDir = occluder * inversesqrt(occluderLength2);

    float cosPhi = dot(occluderDir, coneDirection);
    float cosTheta = sqrt(occluderLength2 / (sphere.w + occluderLength2));

    float occlusion =
        sphericalCapsIntersection(cosTheta, coneAngle.x, coneAngle.y, cosPhi) / (1.0 - coneAngle.x);
    return occlusion;
}

float directionalOcclusionCapsule(
    in vec3 pos,
    in vec3 capsuleA,
    in vec3 capsuleB,
    in float capsuleRadius,
    in vec3 coneDirection,
    in vec2 coneAngle
) {
    vec3 Ld = capsuleB - capsuleA;
    vec3 L0 = capsuleA - pos;
    float a = dot(coneDirection, Ld);
    float t = saturate(dot(L0, a * coneDirection - Ld) / (dot(Ld, Ld) - a * a));
    vec3 posToRay = capsuleA + t * Ld;

    return directionalOcclusionSphere(pos, vec4(posToRay, capsuleRadius), coneDirection, coneAngle);
}

half4 main(float2 fragCoord) {
    vec3 position = vec3((2.0 * fragCoord - size) / vec2(max(size.x, size.y)), 0.0);
    vec3 fingerEnd = fingerPosition + fingerDirection * fingerLength;
    float occlusion = directionalOcclusionCapsule(
        position,
        fingerPosition,
        fingerEnd,
        fingerSquareRadius,
        lightConeDirection,
        lightConeAngle
    );

    vec3 posToLight = fadeDistance.xyz - position;
    float distanceSquare = dot(posToLight, posToLight);
    float factor = distanceSquare * fadeDistance.w;
    float smoothFactor = max(1.0 - factor * factor, 0.0);
    float attenuation = (smoothFactor * smoothFactor) / max(distanceSquare, 1e-4);
    attenuation *= occlusion;
    // AGSL output must use premultiplied alpha, including colored shadows.
    half alpha = saturate(shadowColor.a * attenuation);
    return half4(shadowColor.rgb * alpha, alpha);
}
"""
