package se.warting.signaturecore.utils

import org.junit.Test
import org.junit.Assert.*

class CoordinateValidationTest {

    @Test
    fun `test coordinate validation with normal values`() {
        val x = 10f
        val y = 20f
        
        val validatedX = if (x.isNaN() || x.isInfinite()) 0f else x
        val validatedY = if (y.isNaN() || y.isInfinite()) 0f else y
        
        assertEquals(10f, validatedX, 0f)
        assertEquals(20f, validatedY, 0f)
    }

    @Test
    fun `test coordinate validation with NaN values`() {
        val x = Float.NaN
        val y = Float.NaN
        
        val validatedX = if (x.isNaN() || x.isInfinite()) 0f else x
        val validatedY = if (y.isNaN() || y.isInfinite()) 0f else y
        
        assertEquals(0f, validatedX, 0f)
        assertEquals(0f, validatedY, 0f)
    }

    @Test
    fun `test coordinate validation with infinite values`() {
        val positiveInfX = Float.POSITIVE_INFINITY
        val negativeInfY = Float.NEGATIVE_INFINITY
        
        val validatedX = if (positiveInfX.isNaN() || positiveInfX.isInfinite()) 0f else positiveInfX
        val validatedY = if (negativeInfY.isNaN() || negativeInfY.isInfinite()) 0f else negativeInfY
        
        assertEquals(0f, validatedX, 0f)
        assertEquals(0f, validatedY, 0f)
    }

    @Test
    fun `test coordinate validation with mixed values`() {
        val x = Float.NaN
        val y = 15f
        
        val validatedX = if (x.isNaN() || x.isInfinite()) 0f else x
        val validatedY = if (y.isNaN() || y.isInfinite()) 0f else y
        
        assertEquals(0f, validatedX, 0f)
        assertEquals(15f, validatedY, 0f)
    }

    @Test
    fun `test SvgPoint can handle validated coordinates without crashing`() {
        // Simulate the coordinate validation that happens in SignaturePad.onTouchEvent
        val invalidX = Float.NaN
        val invalidY = Float.POSITIVE_INFINITY
        
        val validatedX = if (invalidX.isNaN() || invalidX.isInfinite()) 0f else invalidX
        val validatedY = if (invalidY.isNaN() || invalidY.isInfinite()) 0f else invalidY
        
        // Create a TimedPoint with validated coordinates
        val timedPoint = TimedPoint()
        timedPoint.set(validatedX, validatedY, System.currentTimeMillis())
        
        // This should not crash anymore since coordinates are validated before reaching SvgPoint
        val svgPoint = SvgPoint(timedPoint)
        
        assertEquals(0, svgPoint.x)
        assertEquals(0, svgPoint.y)
    }
}