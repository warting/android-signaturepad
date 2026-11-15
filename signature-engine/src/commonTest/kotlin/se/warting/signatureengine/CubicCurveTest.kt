package se.warting.signatureengine

import kotlin.test.Test
import kotlin.test.assertEquals

class CubicCurveTest {
    @Test
    fun straightLineRetainsLengthAndEndpoints() {
        val curve = CubicCurve(CurvePoint(0f, 0f), CurvePoint(10f, 0f), CurvePoint(20f, 0f), CurvePoint(30f, 0f))
        assertEquals(30f, curve.length(), 0.0001f)
        assertEquals(0.0, CubicCurve.coordinate(0f, 0f, 10f, 20f, 30f))
        assertEquals(30.0, CubicCurve.coordinate(1f, 0f, 10f, 20f, 30f))
        assertEquals(15.0, CubicCurve.coordinate(0.5f, 0f, 10f, 20f, 30f))
    }
}
