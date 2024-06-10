package se.warting.signaturepad

import android.graphics.Color
import android.util.Log
import android.view.MotionEvent
import se.warting.signaturecore.DrawEvent
import se.warting.signaturecore.Event
import se.warting.signatureview.utils.Bezier
import se.warting.signatureview.utils.ControlTimedPoints
import se.warting.signatureview.utils.TimedPoint
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

open class BezierThing(
    private val minWidth: Int,
    private val maxWidth: Int,
    private val velocityFilterWeight: Float
) {
    private val svgBezier: MutableList<Pair<Bezier, Float>> = mutableListOf()
    private val pointsCache: MutableList<TimedPoint?> = ArrayList()
    private var strokeWidth : Float = 0f
    private val paint : Int = Color.BLACK
    val drawThis: MutableList<DrawEvent> = mutableListOf()

    private val points = mutableListOf<TimedPoint>()

    var lastWidth = (minWidth + maxWidth) / 2f
    var mLastVelocity = 0f


    open fun current(event: Event) {
        val timestamp = event.timestamp
        val eventX = event.x
        val eventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                points.clear()
                addTimedPoint(getNewTimedPoint(eventX, eventY, timestamp))
            }

            MotionEvent.ACTION_MOVE -> {
                addTimedPoint(getNewTimedPoint(eventX, eventY, timestamp))
            }

            MotionEvent.ACTION_UP -> {
                addTimedPoint(getNewTimedPoint(eventX, eventY, timestamp))
            }

            else -> {
                throw IllegalStateException("Unknown Motion " + event.action)
            }
        }
    }

    private fun calculateStrokeWidth(velocity: Float): Float {
        return max(maxWidth / (velocity + 1), minWidth.toFloat())
    }

    @SuppressWarnings("MagicNumber")
    private fun addTimedPoint(timedPoint: TimedPoint) {
        points.add(timedPoint)
        val pointsCount = points.size
        if (pointsCount > 3) {
            var tmp: ControlTimedPoints =
                calculateCurveControlPoints(points[0], points[1], points[2], timedPoint.timestamp)
            val c2 = tmp.c2
            recyclePoint(tmp.c1)
            tmp = calculateCurveControlPoints(points[1], points[2], points[3], timedPoint.timestamp)
            val c3 = tmp.c1
            recyclePoint(tmp.c2)
            val curve = Bezier(points[1], c2, c3, points[2])
            val startPoint = curve.startPoint
            val endPoint = curve.endPoint
            var velocity = endPoint.velocityFrom(startPoint)

            velocity =
                (velocityFilterWeight * velocity + (1 - velocityFilterWeight) * mLastVelocity)

            // The new width is a function of the velocity. Higher velocities
            // correspond to thinner strokes.
            val newWidth = calculateStrokeWidth(velocity)

            // The Bezier's width starts out as last curve's final width, and
            // gradually changes to the stroke width just calculated. The new
            // width calculation is based on the velocity between the Bezier's
            // start and end mPoints.
            addBezier(curve, lastWidth, newWidth)
            mLastVelocity = velocity
            lastWidth = newWidth

            // Remove the first element from the list,
            // so that we always have no more than 4 mPoints in mPoints array.
            recyclePoint(points.removeAt(0))
            recyclePoint(c2)
            recyclePoint(c3)
        } else if (pointsCount == 1) {
            // To reduce the initial lag make it work with 3 mPoints
            // by duplicating the first point
            val firstPoint = points[0]
            points.add(getNewTimedPoint(firstPoint.x, firstPoint.y, timedPoint.timestamp))
        }
    }

    @SuppressWarnings("MagicNumber")
    private fun addBezier(curve: Bezier, startWidth: Float, endWidth: Float) {
        synchronized(drawThis) {
            Log.d("EventManager", "addBezier")
            svgBezier.add(curve to (startWidth + endWidth) / 2)

            val originalWidth = strokeWidth
            val widthDelta = endWidth - startWidth
            val drawSteps = ceil(curve.length().toDouble()).toFloat()
            var i = 0
            while (i < drawSteps) {
                // Calculate the Bezier (x, y) coordinate for this step.
                val t = i.toFloat() / drawSteps
                val tt = t * t
                val ttt = tt * t
                val u = 1 - t
                val uu = u * u
                val uuu = uu * u
                var x = uuu * curve.startPoint.x
                x += 3 * uu * t * curve.control1.x
                x += 3 * u * tt * curve.control2.x
                x += ttt * curve.endPoint.x
                var y = uuu * curve.startPoint.y
                y += 3 * uu * t * curve.control1.y
                y += 3 * u * tt * curve.control2.y
                y += ttt * curve.endPoint.y

                // Set the incremental stroke width and draw.
                strokeWidth = startWidth + ttt * widthDelta
                // mSignatureBitmapCanvas!!.drawPoint(x, y, mPaint)
                draw(
                    DrawEvent(
                        x,
                        y,
                        MotionEvent.ACTION_MOVE,
                        strokeWidth,
                        paint
                    )
                )

                i++
            }
            strokeWidth = originalWidth
            Log.d("EventManager", "onCanvasChanged")
        }
    }

    open fun draw(drawEvent: DrawEvent) {
        drawThis.add(drawEvent)
    }

    private fun calculateCurveControlPoints(
        s1: TimedPoint,
        s2: TimedPoint,
        s3: TimedPoint,
        timestamp: Long
    ): ControlTimedPoints {
        val dx1 = s1.x - s2.x
        val dy1 = s1.y - s2.y
        val dx2 = s2.x - s3.x
        val dy2 = s2.y - s3.y
        val m1X = (s1.x + s2.x) / 2.0f
        val m1Y = (s1.y + s2.y) / 2.0f
        val m2X = (s2.x + s3.x) / 2.0f
        val m2Y = (s2.y + s3.y) / 2.0f
        val l1 = sqrt((dx1 * dx1 + dy1 * dy1).toDouble()).toFloat()
        val l2 = sqrt((dx2 * dx2 + dy2 * dy2).toDouble()).toFloat()
        val dxm = m1X - m2X
        val dym = m1Y - m2Y
        var k = l2 / (l1 + l2)
        if (k.isNaN()) k = 0.0f
        val cmX = m2X + dxm * k
        val cmY = m2Y + dym * k
        val tx = s2.x - cmX
        val ty = s2.y - cmY
        return ControlTimedPoints(
            getNewTimedPoint(m1X + tx, m1Y + ty, timestamp),
            getNewTimedPoint(m2X + tx, m2Y + ty, timestamp)
        )
    }

    private fun getNewTimedPoint(x: Float, y: Float, timestamp: Long): TimedPoint {
        val cacheSize = pointsCache.size
        val timedPoint: TimedPoint? = if (cacheSize == 0) {
            // Cache is empty, create a new point
            TimedPoint()
        } else {
            // Get point from cache
            pointsCache.removeAt(cacheSize - 1)
        }
        return timedPoint!!.set(x, y, timestamp)
    }

    private fun recyclePoint(point: TimedPoint?) {
        pointsCache.add(point)
    }
}
